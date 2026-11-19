#!/usr/bin/env bash
set -euo pipefail

mode="${1:-docker}"
if [[ "$mode" == "--native-api" || "$mode" == "--native-ui" || "$mode" == "--native" ]]; then
  require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
      echo "$2" >&2
      exit 1
    fi
  }

  require_command nc "Install netcat (nc) to check native database and broker ports."
  require_command curl "Install curl to check local service health."

  if [[ "$mode" == "--native-api" || "$mode" == "--native" ]]; then
    require_command java "Install Java 21 or newer to run the API without containers."
    require_command mvn "Install Maven 3.9 or newer to build the API without containers."
    java_major="$(java -version 2>&1 | sed -nE 's/.*version "([0-9]+).*/\1/p' | head -n 1)"
    if [[ -z "$java_major" || "$java_major" -lt 21 ]]; then
      echo "Java 21 or newer is required (detected: ${java_major:-unknown})." >&2
      exit 1
    fi
    maven_version="$(mvn -version 2>&1 | sed -nE '1s/.* ([0-9]+\.[0-9]+(\.[0-9]+)?).*/\1/p')"
    if [[ -z "$maven_version" ]]; then
      echo "Could not determine Maven version." >&2
      exit 1
    fi
    maven_major="${maven_version%%.*}"
    maven_minor="${maven_version#*.}"
    maven_minor="${maven_minor%%.*}"
    if [[ "$maven_major" -lt 3 || ( "$maven_major" -eq 3 && "$maven_minor" -lt 9 ) ]]; then
      echo "Maven 3.9 or newer is required (detected: $maven_version)." >&2
      exit 1
    fi
  fi

  if [[ "$mode" == "--native-ui" || "$mode" == "--native" ]]; then
    require_command node "Install Node.js 20.19+ or 22.12+ to run Angular without containers."
    require_command npm "Install npm with Node.js to run Angular without containers."
    node_version="$(node -p 'process.versions.node')"
    node_major="${node_version%%.*}"
    node_minor="${node_version#*.}"
    node_minor="${node_minor%%.*}"
    if ! { [[ "$node_major" -eq 20 && "$node_minor" -ge 19 ]] \
      || [[ "$node_major" -eq 22 && "$node_minor" -ge 12 ]] \
      || [[ "$node_major" -eq 24 ]]; }; then
      echo "Angular requires Node.js 20.19+, 22.12+, or 24.x (detected: $node_version)." >&2
      exit 1
    fi
  fi
  exit 0
fi
if [[ "$mode" != docker ]]; then
  echo "Usage: $0 [--native-api|--native-ui|--native]" >&2
  exit 2
fi

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
