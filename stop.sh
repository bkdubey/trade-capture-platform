#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
if [[ "${1:-}" == "--native" ]]; then
  exec "$ROOT_DIR/scripts/stop-native.sh"
elif [[ $# -gt 0 ]]; then
  echo "Usage: ./stop.sh [--native]" >&2
  exit 2
fi
cd "$ROOT_DIR"
docker compose --profile tools down
