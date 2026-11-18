#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
"$ROOT_DIR/scripts/check-dependencies.sh"
cd "$ROOT_DIR"
docker compose --profile tools up --build -d --wait ui
docker compose --profile tools run --build --rm seed-demo
UI_PORT="$(docker compose port ui 80 | sed -E 's/.*:([0-9]+)$/\1/')"
printf 'Dashboard: http://localhost:%s\n' "$UI_PORT"
