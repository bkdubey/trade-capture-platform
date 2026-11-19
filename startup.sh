#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"
case "${1:-}" in
  --native)
    exec "$ROOT_DIR/scripts/start-native.sh" all
    ;;
  --infra-only)
    "$ROOT_DIR/scripts/check-dependencies.sh"
    docker compose up --build -d --wait postgres kafka
    echo "PostgreSQL and Kafka are ready. See local-memory/manual-test.md for native app startup."
    exit 0
    ;;
  ""|--docker)
    ;;
  --help|-h)
    echo "Usage: ./startup.sh [--docker|--native|--infra-only]"
    exit 0
    ;;
  *)
    echo "Unknown option: $1" >&2
    echo "Usage: ./startup.sh [--docker|--native|--infra-only]" >&2
    exit 2
    ;;
esac
"$ROOT_DIR/scripts/check-dependencies.sh"
docker compose up --build -d --wait
docker compose --profile tools run --build --rm seed-demo
UI_PORT="$(docker compose port ui 80 | sed -E 's/.*:([0-9]+)$/\1/')"
API_PORT="$(docker compose port api 8080 | sed -E 's/.*:([0-9]+)$/\1/')"
printf '\nTrade capture is ready:\n'
printf '  Dashboard: http://localhost:%s\n' "$UI_PORT"
printf '  API:       http://localhost:%s/api/v1/trades\n' "$API_PORT"
printf '  Login:     disabled for local demo\n'
printf '  Metrics:   http://localhost:%s/actuator/prometheus\n' "$API_PORT"
