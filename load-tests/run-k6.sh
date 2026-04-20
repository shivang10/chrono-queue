#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Usage: $0 <scenario-file> [k6 args...]"
  exit 1
fi

SCENARIO_FILE="$1"
shift || true

if command -v k6 >/dev/null 2>&1; then
  exec k6 run "$SCENARIO_FILE" "$@"
fi

exec docker run --rm --network host \
  --user "$(id -u):$(id -g)" \
  -v "$(pwd):/workspace" -w /workspace \
  grafana/k6:latest \
  run "$SCENARIO_FILE" "$@"
