#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
run_dir="$ROOT_DIR/local-memory/run"

for service in ui api; do
  pid_file="$run_dir/$service.pid"
  [[ -f "$pid_file" ]] || continue
  pid="$(cat "$pid_file")"
  if [[ "$pid" =~ ^[0-9]+$ ]] && kill -0 "$pid" 2>/dev/null; then
    command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
    case "$command_line" in
      *"$ROOT_DIR"*)
        kill "$pid"
        echo "Stopped native $service (PID $pid)."
        ;;
      *)
        echo "Leaving unrelated process $pid untouched; removing stale $service PID file."
        ;;
    esac
  else
    echo "Removing stale $service PID file."
  fi
  rm -f "$pid_file"
done
