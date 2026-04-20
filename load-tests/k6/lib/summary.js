/**
 * k6 handleSummary helper — produces a structured JSON snapshot of every
 * metric k6 collected during the run.
 *
 * Usage inside a scenario file:
 *
 *   import { buildJsonSummary } from '../lib/summary.js';
 *
 *   export function handleSummary(data) {
 *     return {
 *       'load-tests/results/smoke-summary.json':
 *         JSON.stringify(buildJsonSummary(data, 'smoke'), null, 2),
 *     };
 *   }
 *
 * The path is relative to k6's working directory (repo root when invoked via
 * run-k6.sh / make, or /workspace when running inside Docker).
 */

function round2(v) {
    return v != null ? Math.round(v * 100) / 100 : null;
}

function round4(v) {
    return v != null ? Math.round(v * 10000) / 10000 : null;
}

/**
 * Extract a Trend metric's aggregated values (all in milliseconds as k6 stores
 * http durations in ms).
 */
function trend(metric) {
    if (!metric) return null;
    const v = metric.values;
    return {
        avg_ms: round2(v.avg),
        min_ms: round2(v.min),
        med_ms: round2(v.med),
        p90_ms: round2(v['p(90)']),
        p95_ms: round2(v['p(95)']),
        p99_ms: round2(v['p(99)']),
        max_ms: round2(v.max),
    };
}

/** Extract a Counter metric (count + per-second rate). */
function counter(metric) {
    if (!metric) return null;
    return {
        count: metric.values.count,
        rate: round4(metric.values.rate),
    };
}

/**
 * Build a self-contained JSON summary object from the k6 handleSummary `data`
 * argument.
 *
 * @param {object} data     - Raw k6 summary data object
 * @param {string} scenario - Human-readable scenario name (e.g. 'smoke')
 */
export function buildJsonSummary(data, scenario) {
    const m = data.metrics;

    // Collect threshold pass/fail results.
    // Modern k6 (grafana/k6:latest) stores per-expression threshold results
    // inside each metric object (data.metrics[name].thresholds), not in the
    // legacy top-level data.thresholds field which is now always empty.
    const thresholds = {};
    for (const [metricName, metric] of Object.entries(m || {})) {
        const mThresholds = metric.thresholds;
        if (!mThresholds || typeof mThresholds !== 'object') continue;
        for (const [expr, result] of Object.entries(mThresholds)) {
            // Key format:  "metric_name[condition]"  e.g. "http_req_duration[p(95)<500]"
            const ok = (result !== null && typeof result === 'object')
                ? result.ok
                : !!result;
            thresholds[`${metricName}[${expr}]`] = ok;
        }
    }

    // http_req_failed is a Rate metric: .values.rate = fraction failed,
    // .values.passes = count of actually-failed requests (truthy events),
    // .values.fails  = count of succeeded requests (falsy events).
    const failedMetric = m.http_req_failed;

    return {
        scenario,
        timestamp: new Date().toISOString(),

        // true  → all thresholds passed
        // false → at least one threshold failed
        // null  → no thresholds defined
        thresholds_passed:
            Object.keys(thresholds).length
                ? Object.values(thresholds).every(Boolean)
                : null,
        thresholds,

        http: {
            req_duration: trend(m.http_req_duration),
            req_waiting: trend(m.http_req_waiting),
            req_connecting: trend(m.http_req_connecting),
            req_tls_handshaking: trend(m.http_req_tls_handshaking),
            req_sending: trend(m.http_req_sending),
            req_receiving: trend(m.http_req_receiving),
            reqs: counter(m.http_reqs),
            req_failed: failedMetric
                ? {
                    rate: round4(failedMetric.values.rate),
                    failed_count: failedMetric.values.passes,
                    total_count:
                        (failedMetric.values.passes || 0) +
                        (failedMetric.values.fails || 0),
                }
                : null,
        },

        vus: m.vus
            ? { min: m.vus.values.min, max: m.vus.values.max }
            : null,

        checks: m.checks
            ? {
                passes: m.checks.values.passes,
                fails: m.checks.values.fails,
                rate: round4(m.checks.values.rate),
            }
            : null,

        iterations: counter(m.iterations),
        iteration_duration: trend(m.iteration_duration),

        // Raw byte counts for the whole run
        data_received_bytes: m.data_received
            ? m.data_received.values.count
            : null,
        data_sent_bytes: m.data_sent
            ? m.data_sent.values.count
            : null,
    };
}
