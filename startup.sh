#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
"$ROOT_DIR/scripts/check-dependencies.sh"
cd "$ROOT_DIR"
docker compose up --build -d --wait
docker compose --profile tools run --build --rm seed-demo
UI_PORT="$(docker compose port ui 80 | sed -E 's/.*:([0-9]+)$/\1/')"
API_PORT="$(docker compose port api 8080 | sed -E 's/.*:([0-9]+)$/\1/')"
printf '\nTrade capture is ready:\n'
printf '  Dashboard: http://localhost:%s\n' "$UI_PORT"
printf '  API:       http://localhost:%s/api/v1/trades\n' "$API_PORT"
printf '  Login:     disabled for local demo\n'
printf '  Metrics:   http://localhost:%s/actuator/prometheus\n' "$API_PORT"
