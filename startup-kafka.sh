#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$ROOT_DIR/scripts/load-env.sh"
"$ROOT_DIR/scripts/check-dependencies.sh"
cd "$ROOT_DIR"
docker compose up --build -d --wait kafka
printf 'Kafka is ready for host producers at localhost:%s (topic: trades.intraday).\n' \
  "${KAFKA_EXTERNAL_PORT:-29092}"
