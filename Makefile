.PHONY: load-test-smoke load-test-throughput load-test-retry-storm load-test-soak \
        load-test-report load-test-report-quick

# ── Individual scenarios ───────────────────────────────────────────────────────
load-test-smoke:
	./load-tests/run-k6.sh load-tests/k6/scenarios/smoke.js

load-test-throughput:
	./load-tests/run-k6.sh load-tests/k6/scenarios/throughput.js

load-test-retry-storm:
	./load-tests/run-k6.sh load-tests/k6/scenarios/retry-storm.js

load-test-soak:
	./load-tests/run-k6.sh load-tests/k6/scenarios/soak.js

# ── Full automated report (all 4 scenarios + Prometheus JVM data + Markdown) ──
# Env overrides:  SCENARIOS="smoke throughput"  PROMETHEUS_URL=http://...
load-test-report:
	@bash load-tests/generate-report.sh

# ── Quick report — smoke + throughput only (skips the 30-min soak run) ─────────
load-test-report-quick:
	@SCENARIOS="smoke throughput" bash load-tests/generate-report.sh
