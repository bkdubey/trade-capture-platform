#!/usr/bin/env python3
"""Generate synthetic trades and submit them as Kafka events and/or a CSV file."""

import argparse
import csv
import json
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CSV_HEADER = [
    "BookingSystem",
    "TradeID",
    "InstrumentID",
    "Quantity",
    "TradePrice",
    "TradeTimestamp",
]
INSTRUMENTS = ("IBM", "AAPL", "MSFT", "NVDA", "AMZN", "ORCL", "TSLA")


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--count", type=int, default=20, help="number of events to generate (1-100000)")
    parser.add_argument(
        "--mode",
        choices=("kafka", "csv", "both"),
        default="kafka",
        help="publish to Kafka, upload one generated CSV, or do both",
    )
    parser.add_argument("--bootstrap-servers", default="localhost:29092")
    parser.add_argument("--api-url", default="http://localhost:8080")
    parser.add_argument("--output", type=Path, default=ROOT / "test-data/generated_trades.csv")
    parser.add_argument("--booking-system", default="SYNTHETIC")
    parser.add_argument("--first-trade-id", type=int, default=None)
    return parser


def generate_trade(index: int, first_trade_id: int, booking_system: str) -> dict[str, object]:
    quantity = 0 if index % 20 == 0 else (-(index + 1) if index % 7 == 0 else index + 1)
    price = 0.0 if index % 20 == 1 else round(75.25 + (index % 500) * 1.37, 4)
    timestamp = datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z")
    return {
        "bookingSystem": booking_system,
        "tradeId": first_trade_id + index,
        "instrumentId": INSTRUMENTS[index % len(INSTRUMENTS)],
        "quantity": quantity,
        "tradePrice": price,
        "tradeTimestamp": timestamp,
    }


def write_csv(trades: list[dict[str, object]], output: Path) -> Path:
    output = output.expanduser()
    if not output.is_absolute():
        output = ROOT / output
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open("w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)
        writer.writerow(CSV_HEADER)
        for trade in trades:
            writer.writerow(
                [
                    trade["bookingSystem"],
                    trade["tradeId"],
                    trade["instrumentId"],
                    trade["quantity"],
                    trade["tradePrice"],
                    trade["tradeTimestamp"],
                ]
            )
    return output


def upload_csv(api_url: str, csv_path: Path) -> int:
    boundary = "tradeflow-synthetic-boundary"
    content = csv_path.read_bytes()
    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{csv_path.name}"\r\n'
        "Content-Type: text/csv\r\n\r\n"
    ).encode() + content + f"\r\n--{boundary}--\r\n".encode()
    request = urllib.request.Request(
        f"{api_url.rstrip('/')}/api/v1/ingestion/files",
        data=body,
        method="POST",
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        result = json.loads(response.read())
    return int(result["accepted"])


def publish_kafka(
    bootstrap_servers: str, trades: list[dict[str, object]]
) -> int:
    try:
        from kafka import KafkaProducer
        from kafka.errors import KafkaError
    except ImportError as error:
        raise RuntimeError(
            "kafka-python is missing. Install it with "
            "`python3 -m pip install -r scripts/requirements.txt`."
        ) from error

    producer = KafkaProducer(
        bootstrap_servers=[server.strip() for server in bootstrap_servers.split(",")],
        key_serializer=lambda key: str(key).encode("utf-8"),
        value_serializer=lambda value: json.dumps(value, separators=(",", ":")).encode("utf-8"),
        acks="all",
        retries=5,
        max_in_flight_requests_per_connection=5,
    )
    try:
        futures = [
            producer.send("trades.intraday", key=trade["tradeId"], value=trade)
            for trade in trades
        ]
        producer.flush(timeout=60)
        for future in futures:
            future.get(timeout=60)
    except KafkaError as error:
        raise RuntimeError(f"Kafka publish failed: {error}") from error
    finally:
        producer.close(timeout=15)
    return len(trades)


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    if not 1 <= args.count <= 100_000:
        parser.error("--count must be between 1 and 100000")
    if args.first_trade_id is not None and args.first_trade_id < 1:
        parser.error("--first-trade-id must be positive")

    first_trade_id = args.first_trade_id or int(time.time() * 1_000)
    trades = [
        generate_trade(index, first_trade_id, args.booking_system)
        for index in range(args.count)
    ]

    try:
        if args.mode in ("csv", "both"):
            csv_path = write_csv(trades, args.output)
            accepted = upload_csv(args.api_url, csv_path)
            print(f"CSV uploaded: {accepted} records accepted ({csv_path})")
        if args.mode in ("kafka", "both"):
            published = publish_kafka(args.bootstrap_servers, trades)
            print(f"Kafka acknowledged: {published} events on trades.intraday")
    except (OSError, RuntimeError, TimeoutError, ValueError, urllib.error.URLError) as error:
        print(f"Synthetic trade push failed: {error}", file=sys.stderr)
        return 1
    print(f"Generated trade IDs: {first_trade_id}–{first_trade_id + args.count - 1}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
