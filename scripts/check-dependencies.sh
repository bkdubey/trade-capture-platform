#!/usr/bin/env bash
set -euo pipefail

if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required. Install Docker Desktop and retry." >&2
  exit 1
fi
if ! docker compose version >/dev/null 2>&1; then
  echo "Docker Compose v2 is required. Update Docker Desktop and retry." >&2
  exit 1
fi
if ! docker info >/dev/null 2>&1; then
  if [[ "$(uname -s)" == "Darwin" ]] && [[ -d /Applications/Docker.app ]]; then
    echo "Starting Docker Desktop..."
    open -a Docker
    for attempt in $(seq 1 60); do
      if docker info >/dev/null 2>&1; then break; fi
      sleep 2
    done
  fi
fi
if ! docker info >/dev/null 2>&1; then
  echo "Docker daemon is unavailable. Start Docker Desktop and retry." >&2
  exit 1
fi
