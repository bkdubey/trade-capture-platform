# Trade Capture Platform — Architecture

## Goals and boundaries

The local implementation captures end-of-day CSV snapshots and intraday Kafka events, applies the same business validation to both, persists valid **and invalid** trades, and serves filtered, historical reports to an Angular dashboard. The design keeps business policy independent of transport, persistence, and presentation so those adapters can be replaced or scaled separately.

## Architecture

```text
                         ┌──────────────────────────┐
CSV snapshot ── upload ─▶│ File-ingestion adapter   │
                         └────────────┬─────────────┘
                                      │ TradeRecord
Kafka producer ─────────▶ Kafka listener adapter
                                      │
                              ┌───────▼────────┐
                              │ Capture use case│
                              │ + validation    │
                              └───────┬────────┘
                                      │ Trade
                              ┌───────▼────────┐       ┌─────────────────────┐
                              │ Repository port │──────▶│ PostgreSQL / Flyway │
                              └───────▲────────┘       └─────────────────────┘
                                      │
Angular UI ── HTTP / Basic ─▶ REST reporting adapter
         └── Nginx /api proxy ────────┘

Prometheus ── authenticated actuator metrics
```

### Clean-architecture layers

* **Domain** (`domain`): `Trade`, source/status types, and the deterministic `TradeValidationPolicy`. It has no database, web, or messaging dependencies.
* **Application** (`application`): capture and reporting use cases, the trade repository port, its query, and transport-neutral records. Capture groups writes in bounded 1,000-row batches and uses an injected UTC clock.
* **Inbound adapters** (`adapter.inbound`): REST controllers parse and validate uploads and expose paginated reports; a Kafka listener maps intraday events to the same capture use case.
* **Outbound adapter** (`adapter.outbound.persistence`): a PostgreSQL JDBC adapter implements the repository port with prepared parameters and JDBC batch writes. Flyway owns schema evolution.
* **Configuration** (`config`): composition root, Kafka topics/retries/DLT, UTC clock, local-demo access policy, and CORS.

The repository interface is the application’s persistence boundary. Kafka and CSV are independent ingress adapters, and both share the same validator and persistence path. Reporting returns trade time, source, status, and validation reason so invalid records can be investigated rather than discarded.

## Data and validation

`trades` stores a generated UUID, the six feed fields, source (`FILE` / `KAFKA`), validation status/reason, and ingestion time. `trade_timestamp` is the event time used for historical range filters; `received_at` is the processing time. PostgreSQL check constraints enforce source/status values and status/reason consistency. Indexes support latest-first browsing and time/source/status filters.

The policy flags `quantity = 0` and `tradePrice <= 0`; negative quantity is accepted as a sell/short position. All records that pass structural parsing are persisted, including business-invalid records. CSV headers and timestamp offsets are strict. Malformed files are rejected with a row number rather than partially imported.

Kafka consumption uses a consumer group, three local topic partitions, three retries, and a dead-letter topic (`trades.intraday.DLT`). Processing is at-least-once. A unique source/booking-system/trade-ID/event-time index and `ON CONFLICT DO NOTHING` make file replays and Kafka redelivery safe. Kafka messages are acknowledged after the synchronous database write succeeds.

## API contract

Authentication is disabled in this local demo; API requests do not require credentials.

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/ingestion/files` | Multipart `file` CSV upload |
| `GET` | `/api/v1/trades` | Filter and page trades |
| `GET` | `/api/v1/reports/summary` | Counts by status and source |
| `GET` | `/actuator/health` | Liveness/readiness and dependency health |
| `GET` | `/actuator/prometheus` | Prometheus metrics |

Trade filters: `status=VALID|INVALID`, `source=FILE|KAFKA`, search `search` (up to 128 characters across booking system, trade ID, instrument, and validation reason), ISO-8601 offset `from`/`to`, zero-based `page`, `size` (1–500; default 50), and an allow-listed sort field/direction. Time filters use trade/event time and are inclusive. Summary filters use the same time range.

## Local topology and operations

Docker Compose runs PostgreSQL 17, single-node Kafka 4 KRaft, the Java 21 Spring Boot API, Angular 20 built as static assets behind Nginx, and an optional demo seeder. Service ports bind to loopback only. Persistent named volumes hold database and Kafka data. The local broker has one replica and is **not** highly available.

`./startup.sh` checks Docker/Compose, starts the services, waits for health, and loads the sample CSV and Kafka events once. `./startup-api.sh` starts the database, broker, and API; `./startup-ui.sh` starts the dashboard and its dependencies and loads sample data. `./stop.sh` stops containers but preserves the named data volumes. `docker compose down -v` deliberately destroys local database and Kafka data.

## Security

**Authentication is intentionally disabled for the local demo.** All API endpoints, including reports, ingestion, and Prometheus metrics, are public. Compose binds service ports to loopback; do not expose this stack to a shared or untrusted network. Restore authentication and authorization before any non-local deployment. CORS is restricted to the local UI origin; actuator exposure is allow-listed; upload size is capped; request values use prepared SQL parameters; JSON rejects unknown fields; the API does not expose exception messages by default; Nginx adds baseline content/frame/referrer headers.

Dependency checks on 2026-11-18 found no known production UI dependency vulnerabilities (`npm audit --omit=dev`) and no known Python dependency vulnerabilities (`pip-audit`). Full npm audit reports an unfixed high-severity `picomatch` advisory in the Angular build/CLI development dependency tree; it is not included in the deployed Nginx image. Keep the toolchain updated and rerun audits when an upstream fix is published. A Java OWASP Dependency-Check scan could not complete without an available NVD vulnerability database.

## Scaling and evolution

This is a runnable single-node development deployment, not a billion-row production system. The boundaries allow a staged path to distributed operation:

1. Deploy stateless API replicas behind a load balancer; externalize credentials/secrets and configure readiness, graceful shutdown, and bounded connection pools.
2. Scale Kafka partitions and consumer instances together; size partitions by key/order requirements, monitor lag and DLT volume, and add replay/runbook controls.
3. Partition PostgreSQL by event date, use time-bounded retention/archive policies, tune indexes and batch sizes, and scale storage/replicas. At larger volumes, evaluate a partitioned/analytical reporting store or CQRS read model rather than unbounded OLTP aggregation.
4. Add schema compatibility/versioning for Kafka events and evolve API contracts independently.
5. Add ingestion job/batch identifiers, operational reconciliation, audit controls, and authorization roles appropriate to a regulated front office.

The present report performs indexed, paginated PostgreSQL queries and an aggregate summary. It intentionally does not claim billion-record throughput: that requires measured workload targets, production-grade HA, partition/retention design, and performance testing.
