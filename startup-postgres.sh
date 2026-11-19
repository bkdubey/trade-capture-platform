#!/usr/bin/env bash
set -euo pipefail
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$ROOT_DIR/scripts/load-env.sh"
"$ROOT_DIR/scripts/check-dependencies.sh"
cd "$ROOT_DIR"
docker compose up --build -d --wait postgres
printf 'PostgreSQL is ready at localhost:%s (database: %s, user: %s).\n' \
  "${POSTGRES_PORT:-5432}" "${POSTGRES_DB:-trades}" "${POSTGRES_USER:-trades}"
