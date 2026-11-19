#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
source "$ROOT_DIR/scripts/load-env.sh"

usage() {
  echo "Usage: $0 api|ui|all" >&2
  exit 2
}

[[ $# -eq 1 ]] || usage
mode="$1"
case "$mode" in
  api) "$ROOT_DIR/scripts/check-dependencies.sh" --native-api ;;
  ui) "$ROOT_DIR/scripts/check-dependencies.sh" --native-ui ;;
  all) "$ROOT_DIR/scripts/check-dependencies.sh" --native ;;
  *) usage ;;
esac

check_port() {
  local host="$1" port="$2" label="$3"
  if ! nc -z -w 2 "$host" "$port" >/dev/null 2>&1; then
    echo "$label is not reachable at $host:$port. Start the native service or correct .env." >&2
    return 1
  fi
}

check_native_dependencies() {
  local kafka_host kafka_port first_broker
  check_port localhost "$POSTGRES_PORT" "PostgreSQL"
  first_broker="${KAFKA_BOOTSTRAP_SERVERS%%,*}"
  kafka_host="${first_broker%:*}"
  kafka_port="${first_broker##*:}"
  check_port "$kafka_host" "$kafka_port" "Kafka"
}

build_api() {
  cd "$ROOT_DIR"
  mvn -B -ntp -pl backend -am package -DskipTests
}

ensure_ui_dependencies() {
  if [[ ! -x "$ROOT_DIR/frontend/node_modules/.bin/ng" ]]; then
    echo "Installing frontend dependencies with npm ci..."
    npm --prefix "$ROOT_DIR/frontend" ci
  fi
}

run_api() {
  exec java -jar "$ROOT_DIR/backend/target/trade-capture-api-1.0.0-SNAPSHOT.jar"
}

run_ui() {
  exec "$ROOT_DIR/frontend/node_modules/.bin/ng" serve \
    --host 127.0.0.1 --port "$UI_PORT" --proxy-config "$ROOT_DIR/frontend/proxy.conf.json"
}

if [[ "$mode" == api ]]; then
  check_native_dependencies
  build_api
  run_api
elif [[ "$mode" == ui ]]; then
  if ! curl --silent --fail "http://localhost:${API_PORT}/actuator/health" >/dev/null; then
    echo "The API is not healthy at http://localhost:${API_PORT}. Start it first with ./startup-api.sh --native." >&2
    exit 1
  fi
  ensure_ui_dependencies
  run_ui
else
  check_native_dependencies
  build_api
  ensure_ui_dependencies

  run_dir="$ROOT_DIR/local-memory/run"
  log_dir="$ROOT_DIR/local-memory/logs"
  mkdir -p "$run_dir" "$log_dir"
  api_pid_file="$run_dir/api.pid"
  ui_pid_file="$run_dir/ui.pid"
  for pid_file in "$api_pid_file" "$ui_pid_file"; do
    if [[ -f "$pid_file" ]]; then
      recorded_pid="$(cat "$pid_file")"
      if [[ "$recorded_pid" =~ ^[0-9]+$ ]] && kill -0 "$recorded_pid" 2>/dev/null; then
        recorded_command="$(ps -p "$recorded_pid" -o command= 2>/dev/null || true)"
        if [[ "$recorded_command" == *"$ROOT_DIR"* ]]; then
          echo "Native services already appear to be running (PID file: $pid_file). Use ./status.sh." >&2
          exit 1
        fi
      fi
    fi
    rm -f "$pid_file"
  done

  java -jar "$ROOT_DIR/backend/target/trade-capture-api-1.0.0-SNAPSHOT.jar" \
    >"$log_dir/api.log" 2>&1 &
  api_pid=$!
  echo "$api_pid" > "$api_pid_file"
  ui_pid=''

  stop_children() {
    for pid in "$ui_pid" "$api_pid"; do
      if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
        kill "$pid"
      fi
    done
    if [[ -n "$ui_pid" ]]; then
      wait "$ui_pid" 2>/dev/null || true
    fi
    wait "$api_pid" 2>/dev/null || true
    rm -f "$api_pid_file" "$ui_pid_file"
  }
  handle_signal() {
    stop_children
    exit 130
  }
  trap stop_children EXIT
  trap handle_signal INT TERM

  api_ready=false
  for attempt in $(seq 1 90); do
    if curl --silent --fail "http://localhost:${API_PORT}/actuator/health" >/dev/null; then
      api_ready=true
      break
    fi
    if ! kill -0 "$api_pid" 2>/dev/null; then
      echo "Native API exited during startup. See $log_dir/api.log." >&2
      tail -n 40 "$log_dir/api.log" >&2
      exit 1
    fi
    sleep 1
  done
  if [[ "$api_ready" != true ]]; then
    echo "Native API did not become healthy. See $log_dir/api.log." >&2
    exit 1
  fi

  "$ROOT_DIR/frontend/node_modules/.bin/ng" serve \
    --host 127.0.0.1 --port "$UI_PORT" --proxy-config "$ROOT_DIR/frontend/proxy.conf.json" \
    >"$log_dir/ui.log" 2>&1 &
  ui_pid=$!
  echo "$ui_pid" > "$ui_pid_file"

  ui_ready=false
  for attempt in $(seq 1 90); do
    if curl --silent --fail "http://localhost:${UI_PORT}" >/dev/null; then
      ui_ready=true
      break
    fi
    if ! kill -0 "$ui_pid" 2>/dev/null; then
      echo "Native UI exited during startup. See $log_dir/ui.log." >&2
      tail -n 40 "$log_dir/ui.log" >&2
      exit 1
    fi
    sleep 1
  done
  if [[ "$ui_ready" != true ]]; then
    echo "Native UI did not become ready. See $log_dir/ui.log." >&2
    exit 1
  fi

  printf 'Native services are ready. Stop with Ctrl+C or ./stop.sh --native.\n'
  printf 'Dashboard: http://localhost:%s\nAPI:       http://localhost:%s\nLogs:      %s\n' \
    "$UI_PORT" "$API_PORT" "$log_dir"
  while kill -0 "$api_pid" 2>/dev/null && kill -0 "$ui_pid" 2>/dev/null; do
    sleep 2
  done
  echo "A native service exited. Check logs in $log_dir." >&2
  exit 1
fi
