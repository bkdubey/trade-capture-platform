# Trade Capture Platform

Local-first capital-markets trade capture, validation, and reporting with a cleanly layered Java/Spring Boot API, PostgreSQL, Kafka, Python sample-feed loader, and Angular dashboard.

## Prerequisites

* macOS with Docker Desktop and Docker Compose v2 running (the startup scripts try to launch Docker Desktop if needed).
* Internet access on first run to build the Maven, Angular, PostgreSQL, Kafka, and Python images.
* Ports `4200`, `8080`, `5432`, and `29092` available.

The backend is a Maven module targeting Java 21 and builds inside Docker. Node/npm and a local PostgreSQL/Kafka installation are not required. Java 21+ and Maven 3.9+ are needed only to build/run the API directly outside Docker.

## Start the complete application

```bash
cp .env.example .env       # recommended; replace both password values
./startup.sh
```

The first build may take several minutes. Startup waits for database, broker, API, and UI health, then loads `sample-data/eod_positions.csv` through the CSV API and `sample-data/intraday_trades.jsonl` into Kafka. The demo loader skips fixture data already present in the database, so it will not duplicate its sample records on later starts.

Open **http://localhost:4200**; login is disabled for this local demo. The dashboard refreshes every 10 seconds and supports all time, today, this week, last 7 days, this month, last month, this year, last year, and custom historical time ranges; status/source filters; searchable, sortable server-side paging; light/dark themes; CSV report export; valid/invalid diagnostics; and manual CSV upload.

The fixtures contain eight EOD file records and five intraday Kafka messages, with both valid and intentionally invalid examples (zero quantity, zero/negative price). For another upload, use a CSV with the exact header:

```csv
BookingSystem,TradeID,InstrumentID,Quantity,TradePrice,TradeTimestamp
MUREX,300001,IBM,25,184.2500,2026-11-18T10:00:00+05:30
```

Every timestamp must be ISO-8601 with an explicit UTC offset. A business-invalid row is retained and reported; malformed CSV is rejected. Negative quantity is valid; zero quantity or a price less than/equal to zero is invalid.

## Service scripts

* `./startup.sh` — start all services and load sample data.
* `./startup-api.sh` — start PostgreSQL, Kafka, and the API only.
* `./startup-ui.sh` — start the API and UI stack and load sample data.
* `./stop.sh` — stop containers; named database/Kafka volumes are preserved.

To completely reset local data, run `docker compose down -v` (this permanently removes both named volumes). Container logs: `docker compose logs -f api kafka postgres ui`.

## API quick reference

API: `http://localhost:8080`; authentication is disabled for this local-only demo.

```bash
curl 'http://localhost:8080/api/v1/reports/summary'
curl \
  'http://localhost:8080/api/v1/trades?status=INVALID&from=2026-11-18T00:00:00Z&page=0&size=50'
curl -F file=@sample-data/eod_positions.csv \
  http://localhost:8080/api/v1/ingestion/files
```

The trade query accepts optional `status=VALID|INVALID`, `source=FILE|KAFKA`, ISO offset `from`/`to`, zero-based `page`, and `size` (1–500). Summary counts are total, valid, invalid, file, and Kafka. Health is `GET /actuator/health`; Prometheus metrics are at `GET /actuator/prometheus`. The dead-letter topic is `trades.intraday.DLT`.

To publish another Kafka event from a host with Python installed:

```bash
python3 -m pip install -r scripts/requirements.txt
python3 - <<'PY'
from kafka import KafkaProducer
import json

producer = KafkaProducer(
    bootstrap_servers="localhost:29092",
    value_serializer=lambda value: json.dumps(value).encode("utf-8"),
)
producer.send("trades.intraday", {
    "bookingSystem": "MUREX", "tradeId": 300002, "instrumentId": "AAPL",
    "quantity": 10, "tradePrice": 220.25,
    "tradeTimestamp": "2026-11-18T10:05:00+05:30",
}).get(timeout=15)
producer.flush()
producer.close()
PY
```

## Build, tests, and coverage

```bash
mvn -B -ntp clean verify
```

This runs backend unit tests and creates `backend/target/site/jacoco/index.html`. Maven Verify enforces at least 80% line coverage (excluding Spring composition/configuration boilerplate and the application launcher). The domain validation, capture batching, empty input, and query-boundary behaviors have unit tests.

Login is intentionally disabled for the localhost-only demo, and all API endpoints—including metrics—are accessible without authentication. Compose publishes service ports only on loopback; do not expose this setup to any shared or untrusted network. Authentication must be restored before deployment outside local development.

The local dependency checks found no known vulnerabilities in shipped UI dependencies (`npm audit --omit=dev`) or the Python client (`pip-audit`). A high-severity advisory with no published fix remains in the Angular CLI/build development dependency chain only; it is excluded from the runtime UI image. The Java OWASP dependency scan was blocked because its NVD database was unavailable. See [architecture.md](architecture.md) for the security controls and audit note.

For a direct local API process, start PostgreSQL and Kafka first, then set `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, and `KAFKA_BOOTSTRAP_SERVERS` and run `mvn -pl backend spring-boot:run`.

## Security and deployment note

Login is intentionally disabled for the localhost-only demo, and all API endpoints—including metrics—are accessible without authentication. Compose publishes service ports only on loopback; do not expose this setup to any shared or untrusted network. Authentication must be restored before deployment outside local development. `.env` is ignored by Git. See [architecture.md](architecture.md) for boundaries, data flow, security controls, failure handling, and the distributed-scaling path.
