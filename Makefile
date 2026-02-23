.PHONY: load-test-smoke load-test-throughput load-test-retry-storm load-test-soak

load-test-smoke:
	./load-tests/run-k6.sh load-tests/k6/scenarios/smoke.js

load-test-throughput:
	./load-tests/run-k6.sh load-tests/k6/scenarios/throughput.js

load-test-retry-storm:
	./load-tests/run-k6.sh load-tests/k6/scenarios/retry-storm.js

load-test-soak:
	./load-tests/run-k6.sh load-tests/k6/scenarios/soak.js
