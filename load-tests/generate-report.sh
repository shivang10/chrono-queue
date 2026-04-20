#!/usr/bin/env bash
# ==============================================================================
#  Chrono Queue — Load Test Report Generator
#
#  Runs one or more k6 load-test scenarios, records each test's time window,
#  queries Prometheus for JVM / service metrics during that window, and then
#  compiles everything into a single Markdown report.
#
#  Usage:
#    ./load-tests/generate-report.sh [scenario]
#
#    scenario  One of: smoke, throughput, retry-storm, soak, all (default: all)
#              Use SCENARIOS env var to pass a space-separated list.
#
#  Environment variables:
#    SCENARIOS        Space-separated list to run (default: all four)
#    PROMETHEUS_URL   Prometheus base URL (default: http://localhost:9090)
#    API_BASE_URL     Passed through to k6 (default: http://localhost:8081)
#    SKIP_PROMETHEUS  Set to "1" to skip Prometheus queries
#
#  Examples:
#    make load-test-report
#    SCENARIOS="smoke throughput" make load-test-report
#    SKIP_PROMETHEUS=1 ./load-tests/generate-report.sh smoke
# ==============================================================================
set -euo pipefail

# ── Resolve directories ────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

RESULTS_DIR="load-tests/results"
PROM_URL="${PROMETHEUS_URL:-http://localhost:9090}"
SKIP_PROM="${SKIP_PROMETHEUS:-0}"

# Timestamp for this report
TS=$(date +%Y%m%d_%H%M%S)
REPORT_DIR="$RESULTS_DIR/report-$TS"
# Create both dirs as the current (host) user so Docker-mounted k6 can write here
mkdir -p "$RESULTS_DIR" "$REPORT_DIR"

# Which scenarios to run
if [[ -n "${1:-}" && "$1" != "all" ]]; then
  SCENARIOS="$1"
else
  SCENARIOS="${SCENARIOS:-smoke throughput retry-storm soak}"
fi

# ── Helpers ────────────────────────────────────────────────────────────────────
log()  { echo "[$(date +%H:%M:%S)] $*" >&2; }
pass() { echo "[$(date +%H:%M:%S)] ✓ $*" >&2; }
warn() { echo "[$(date +%H:%M:%S)] ⚠ $*" >&2; }
fail() { echo "[$(date +%H:%M:%S)] ✗ $*" >&2; }

hr() { echo "────────────────────────────────────────────────────────" >&2; }

# ── Run one scenario ───────────────────────────────────────────────────────────
run_scenario() {
  local name="$1"
  local js_file="load-tests/k6/scenarios/${name}.js"

  if [[ ! -f "$js_file" ]]; then
    warn "Scenario file not found: $js_file — skipping."
    return 0
  fi

  hr
  log "▶  Starting scenario: $name"
  log "   Script : $js_file"

  local start_ts
  start_ts=$(date -u +%s)

  # k6 exits non-zero when thresholds fail; we still want to capture the
  # summary JSON and continue with remaining scenarios.
  if ./load-tests/run-k6.sh "$js_file" \
      2>&1 | tee "$REPORT_DIR/${name}.log"; then
    pass "Scenario $name completed — all thresholds met."
  else
    warn "Scenario $name exited non-zero (thresholds may have failed); continuing."
  fi

  local end_ts
  end_ts=$(date -u +%s)
  echo "$start_ts $end_ts" > "$REPORT_DIR/${name}-window.txt"

  # The handleSummary() in the scenario writes the JSON summary relative to
  # the repo root.  Copy it into the report directory for archival.
  local summary_src="$RESULTS_DIR/${name}-summary.json"
  if [[ -f "$summary_src" ]]; then
    cp "$summary_src" "$REPORT_DIR/${name}-summary.json"
    pass "Summary JSON → $REPORT_DIR/${name}-summary.json"
  else
    warn "No summary JSON found at $summary_src — k6 may have failed early."
  fi

  log "   Window : $(date -d "@$start_ts" '+%H:%M:%S') → $(date -d "@$end_ts" '+%H:%M:%S')  ($(( end_ts - start_ts ))s)"
}

# ── Query Prometheus for one scenario window ───────────────────────────────────
query_prometheus() {
  local name="$1"
  local start_ts="$2"
  local end_ts="$3"
  local out_file="$REPORT_DIR/${name}-jvm.json"

  log "Querying Prometheus for $name (window $start_ts → $end_ts)…"

  if python3 load-tests/parse-results.py collect-prometheus \
      --scenario  "$name"      \
      --start     "$start_ts"  \
      --end       "$end_ts"    \
      --prom-url  "$PROM_URL"  \
      --out       "$out_file"; then
    pass "Prometheus data → $out_file"
  else
    warn "Prometheus query failed for $name — JVM section will be empty."
  fi
}

# ── Main ───────────────────────────────────────────────────────────────────────
hr
log "Chrono Queue Load Test Report — $(date '+%Y-%m-%d %H:%M:%S')"
log "Scenarios   : $SCENARIOS"
log "Prometheus  : $PROM_URL"
log "Report dir  : $REPORT_DIR"
hr

# 1. Run scenarios sequentially
for scenario in $SCENARIOS; do
  run_scenario "$scenario"
done

# 2. Collect Prometheus JVM metrics for each scenario's time window
if [[ "$SKIP_PROM" != "1" ]]; then
  if ! command -v python3 >/dev/null 2>&1; then
    warn "python3 not found — skipping Prometheus metric collection."
  else
    for scenario in $SCENARIOS; do
      window_file="$REPORT_DIR/${scenario}-window.txt"
      if [[ -f "$window_file" ]]; then
        read -r s e < "$window_file"
        # Add a small buffer: Prometheus may not have scraped the very last 15 s
        query_prometheus "$scenario" "$s" "$(( e + 15 ))" || true
      fi
    done
  fi
else
  warn "SKIP_PROMETHEUS=1 — skipping Prometheus queries."
fi

# 3. Generate the combined Markdown report
hr
log "Generating Markdown report…"

if ! command -v python3 >/dev/null 2>&1; then
  warn "python3 not found — cannot generate combined report."
  warn "Raw JSON summaries are in $REPORT_DIR/"
  exit 0
fi

REPORT_FILE="$REPORT_DIR/report.md"
python3 load-tests/parse-results.py report \
    --report-dir "$REPORT_DIR"             \
    --scenarios  "$SCENARIOS"              \
    > "$REPORT_FILE"

hr
pass "Report written → $REPORT_FILE"
echo ""
echo "════════════════════════════════════════════════════════"
echo "  CHRONO QUEUE LOAD TEST REPORT"
echo "  $REPORT_FILE"
echo "════════════════════════════════════════════════════════"
echo ""
cat "$REPORT_FILE"
