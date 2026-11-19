#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [[ "${1:-}" == "--native" ]]; then
  exec "$ROOT_DIR/scripts/start-native.sh" api
elif [[ $# -gt 0 ]]; then
  echo "Usage: ./startup-api.sh [--native]" >&2
  exit 2
fi
"$ROOT_DIR/scripts/check-dependencies.sh"
cd "$ROOT_DIR"
docker compose up --build -d --wait postgres kafka api
API_PORT="$(docker compose port api 8080 | sed -E 's/.*:([0-9]+)$/\1/')"
printf 'Trade API: http://localhost:%s/api/v1/trades\n' "$API_PORT"
printf 'Health:    http://localhost:%s/actuator/health\n' "$API_PORT"
