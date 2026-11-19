#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "$ROOT_DIR/scripts/load-env.sh"
cd "$ROOT_DIR"

show_http() {
  local name="$1" url="$2"
  if curl --silent --fail --max-time 2 "$url" >/dev/null; then
    printf '%-14s healthy (%s)\n' "$name" "$url"
  else
    printf '%-14s not responding (%s)\n' "$name" "$url"
  fi
}

echo "Trade capture service status"
if command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
  echo
  echo "Docker Compose containers:"
  docker compose -f "$ROOT_DIR/docker-compose.yml" ps --format '  {{.Name}}: {{.State}}' 2>/dev/null || true
fi

echo
show_http "API" "http://localhost:${API_PORT}/actuator/health"
show_http "Dashboard" "http://localhost:${UI_PORT}"

if nc -z -w 2 localhost "$POSTGRES_PORT" >/dev/null 2>&1; then
  printf '%-14s reachable (localhost:%s, database=%s, user=%s)\n' \
    "PostgreSQL" "$POSTGRES_PORT" "$POSTGRES_DB" "$POSTGRES_USER"
else
  printf '%-14s not reachable (localhost:%s)\n' "PostgreSQL" "$POSTGRES_PORT"
fi

first_broker="${KAFKA_BOOTSTRAP_SERVERS%%,*}"
kafka_host="${first_broker%:*}"
kafka_port="${first_broker##*:}"
if docker compose ps --status running kafka 2>/dev/null | grep -q kafka; then
  kafka_host=localhost
  kafka_port="$KAFKA_EXTERNAL_PORT"
fi
if nc -z -w 2 "$kafka_host" "$kafka_port" >/dev/null 2>&1; then
  printf '%-14s reachable (%s:%s)\n' "Kafka" "$kafka_host" "$kafka_port"
else
  printf '%-14s not reachable (%s:%s)\n' "Kafka" "$kafka_host" "$kafka_port"
fi

run_dir="$ROOT_DIR/local-memory/run"
for service in api ui; do
  pid_file="$run_dir/$service.pid"
  if [[ -f "$pid_file" ]]; then
    pid="$(cat "$pid_file")"
    if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
      command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
      if [[ "$command_line" == *"$ROOT_DIR"* ]]; then
        printf 'Native %-8s process running (PID %s)\n' "$service" "$pid"
      else
        printf 'Native %-8s stale PID file\n' "$service"
      fi
    else
      printf 'Native %-8s stale PID file\n' "$service"
    fi
  fi
done
