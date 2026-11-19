#!/usr/bin/env python3
"""Load the local sample CSV and publish sample intraday events to Kafka."""

import json
import os
import time
import urllib.error
import urllib.request
from pathlib import Path

from kafka import KafkaProducer

API_BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080").rstrip("/")
KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:29092")
ROOT = Path(__file__).resolve().parent.parent


def api_request(path: str, method: str = "GET", data: bytes | None = None,
                content_type: str = "application/json") -> bytes:
    request = urllib.request.Request(
        f"{API_BASE_URL}{path}",
        data=data,
        method=method,
        headers={"Content-Type": content_type},
    )
    with urllib.request.urlopen(request, timeout=15) as response:
        return response.read()


def upload_snapshot() -> int:
    boundary = "trade-capture-local-boundary"
    csv_bytes = (ROOT / "test-data/eod_positions.csv").read_bytes()
    body = (
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="file"; filename="eod_positions.csv"\r\n'
        "Content-Type: text/csv\r\n\r\n"
    ).encode() + csv_bytes + f"\r\n--{boundary}--\r\n".encode()
    result = json.loads(api_request(
        "/api/v1/ingestion/files", "POST", body, f"multipart/form-data; boundary={boundary}"))
    return int(result["accepted"])


def publish_intraday() -> int:
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS.split(","),
        key_serializer=lambda key: key.encode("utf-8"),
        value_serializer=lambda value: json.dumps(value, separators=(",", ":")).encode("utf-8"),
        acks="all",
        retries=5,
        max_in_flight_requests_per_connection=5,
    )
    count = 0
    try:
        with (ROOT / "test-data/intraday_trades.jsonl").open(encoding="utf-8") as events:
            for line in events:
                event = json.loads(line)
                producer.send("trades.intraday", key=str(event["tradeId"]), value=event).get(timeout=15)
                count += 1
        producer.flush(timeout=15)
    finally:
        producer.close(timeout=15)
    return count


def main() -> None:
    summary = json.loads(api_request("/api/v1/reports/summary"))
    expected_file = sum(
        1 for _ in (ROOT / "test-data/eod_positions.csv").open(encoding="utf-8")) - 1
    expected_kafka = sum(
        1 for _ in (ROOT / "test-data/intraday_trades.jsonl").open(encoding="utf-8"))
    if summary["file"] < expected_file:
        upload_snapshot()
    if summary["kafka"] < expected_kafka:
        publish_intraday()
    if summary["file"] >= expected_file and summary["kafka"] >= expected_kafka:
        print(f"Demo data is already loaded: {summary['total']} trades.")
        return

    deadline = time.monotonic() + 45
    while time.monotonic() < deadline:
        summary = json.loads(api_request("/api/v1/reports/summary"))
        if summary["file"] >= expected_file and summary["kafka"] >= expected_kafka:
            print(
                f"Loaded EOD/Kafka sample data ({summary['file']} file and {summary['kafka']} Kafka records) "
                f"({summary['valid']} valid, {summary['invalid']} invalid)."
            )
            return
        time.sleep(1)
    raise TimeoutError("Kafka records were published but did not appear in reports within 45 seconds")


if __name__ == "__main__":
    main()
