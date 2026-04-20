#!/usr/bin/env python3
"""
Chrono Queue — Load Test Report Processor
==========================================
Two sub-commands:

  collect-prometheus
      Query a running Prometheus instance for JVM / service metrics that
      were recorded during a specific k6 test window and save them as JSON.

      python3 load-tests/parse-results.py collect-prometheus \\
          --scenario  smoke \\
          --start     1714000000 \\
          --end       1714003600 \\
          --prom-url  http://localhost:9090 \\
          --out       load-tests/results/report-xxx/smoke-jvm.json

  report
      Read the JSON files produced by k6 handleSummary and
      collect-prometheus, then write a comprehensive Markdown report to
      stdout.

      python3 load-tests/parse-results.py report \\
          --report-dir load-tests/results/report-xxx \\
          --scenarios  "smoke throughput retry-storm soak"
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

# ── Prometheus helpers ─────────────────────────────────────────────────────────


def _prom_get(url: str):
    try:
        with urllib.request.urlopen(url, timeout=10) as r:
            return json.loads(r.read())
    except (urllib.error.URLError, OSError) as exc:
        return {"status": "error", "error": str(exc)}


def prom_range(prom_url: str, query: str, start: int, end: int, step: int = 15):
    params = urllib.parse.urlencode(
        {"query": query, "start": start, "end": end, "step": step}
    )
    return _prom_get(f"{prom_url}/api/v1/query_range?{params}")


def extract_by_label(result: dict, label_key: str = "service") -> dict:
    """Return {label_value: {avg, min, max, samples}} from a range query result."""
    if result.get("status") != "success":
        return {}
    out = {}
    for series in result["data"]["result"]:
        label_val = series.get("metric", {}).get(label_key, "unknown")
        vals = []
        for _, raw in series["values"]:
            try:
                f = float(raw)
                if f == f:  # skip NaN
                    vals.append(f)
            except ValueError:
                pass
        if vals:
            out[label_val] = {
                "avg": round(sum(vals) / len(vals), 6),
                "min": round(min(vals), 6),
                "max": round(max(vals), 6),
                "samples": len(vals),
            }
    return out


def collect_prometheus(
    scenario: str,
    start_ts: int,
    end_ts: int,
    prom_url: str,
    out_file: str,
):
    """Query Prometheus for the given time window and write JSON to out_file."""
    out = {
        "scenario": scenario,
        "window": {"start": start_ts, "end": end_ts},
        "prom_url": prom_url,
        "collected_at": datetime.now(timezone.utc).isoformat(),
        "jvm": {},
        "kafka": {},
    }

    # ── JVM / process metrics (keyed by Prometheus `service` label) ────────────
    jvm_queries = {
        "heap_used_bytes":
            'sum by (service) (jvm_memory_used_bytes{area="heap"})',
        "heap_committed_bytes":
            'sum by (service) (jvm_memory_committed_bytes{area="heap"})',
        "heap_max_bytes":
            'sum by (service) (jvm_memory_max_bytes{area="heap"})',
        "nonheap_used_bytes":
            'sum by (service) (jvm_memory_used_bytes{area="nonheap"})',
        "gc_pause_max_seconds":
            "max by (service) (jvm_gc_pause_seconds_max)",
        "gc_pause_rate_per_sec":
            "sum by (service) (rate(jvm_gc_pause_seconds_sum[1m]))",
        "gc_collections_rate":
            "sum by (service) (rate(jvm_gc_pause_seconds_count[1m]))",
        "threads_live":
            "max by (service) (jvm_threads_live_threads)",
        "threads_peak":
            "max by (service) (jvm_threads_peak_threads)",
        "threads_daemon":
            "max by (service) (jvm_threads_daemon_threads)",
        "cpu_usage":
            "max by (service) (process_cpu_usage)",
        "open_file_descriptors":
            "max by (service) (process_open_fds)",
        # Micrometer http.server.requests histogram (Spring Boot actuator)
        "http_server_p95_seconds": (
            "histogram_quantile(0.95, sum by (le, service) "
            "(rate(http_server_requests_seconds_bucket[1m])))"
        ),
        "http_server_p99_seconds": (
            "histogram_quantile(0.99, sum by (le, service) "
            "(rate(http_server_requests_seconds_bucket[1m])))"
        ),
        "http_server_req_rate": (
            "sum by (service) (rate(http_server_requests_seconds_count[1m]))"
        ),
        "http_server_5xx_rate": (
            'sum by (service) '
            '(rate(http_server_requests_seconds_count{status=~"5.."}[1m]))'
        ),
        "http_server_4xx_rate": (
            'sum by (service) '
            '(rate(http_server_requests_seconds_count{status=~"4.."}[1m]))'
        ),
    }

    for key, query in jvm_queries.items():
        result = prom_range(prom_url, query, start_ts, end_ts)
        out["jvm"][key] = extract_by_label(result)

    # ── Kafka consumer lag ─────────────────────────────────────────────────────
    # Spring Boot Micrometer Kafka metrics use the `service` static label added
    # by Prometheus (from prometheus.yml). We query both the running lag and the
    # per-consumer max-lag gauge to surface peak backlog during the window.
    def _collect_kafka_lag(query: str) -> dict:
        result = prom_range(prom_url, query, start_ts, end_ts)
        out_lag = {}
        if result.get("status") == "success":
            for series in result["data"]["result"]:
                lm = series.get("metric", {})
                key = f"{lm.get('service', '?')}/{lm.get('topic', '?')}"
                vals = []
                for _, raw in series["values"]:
                    try:
                        f = float(raw)
                        if f == f:  # skip NaN
                            vals.append(f)
                    except ValueError:
                        pass
                if vals:
                    out_lag[key] = {
                        "avg": round(sum(vals) / len(vals), 2),
                        "max": round(max(vals), 2),
                    }
        return out_lag

    # Average + peak of the instantaneous per-partition lag (sum across partitions)
    lag_current = _collect_kafka_lag(
        "sum by (service, topic) (kafka_consumer_fetch_manager_records_lag)"
    )
    # Max of the consumer's own worst-case lag gauge (records-lag-max)
    lag_peak = _collect_kafka_lag(
        "max by (service, topic) (kafka_consumer_fetch_manager_records_lag_max)"
    )

    # Merge into a single dict keyed by "service/topic"
    all_keys = set(lag_current) | set(lag_peak)
    for key in all_keys:
        cur = lag_current.get(key, {})
        pk = lag_peak.get(key, {})
        out["kafka"][key] = {
            "avg_lag": cur.get("avg", 0.0),
            "max_lag": cur.get("max", 0.0),
            "peak_lag_max": pk.get("max", 0.0),
        }

    Path(out_file).parent.mkdir(parents=True, exist_ok=True)
    with open(out_file, "w") as f:
        json.dump(out, f, indent=2)
    print(f"Saved Prometheus metrics → {out_file}", file=sys.stderr)


# ── Formatting helpers ─────────────────────────────────────────────────────────

def _mb(b) -> str:
    if b is None:
        return "N/A"
    return f"{b / 1_048_576:.1f}"


def _pct(v) -> str:
    if v is None:
        return "N/A"
    return f"{v * 100:.2f}%"


def _ms(v) -> str:
    if v is None:
        return "N/A"
    return f"{v:.2f}"


def _f1(v) -> str:
    if v is None:
        return "N/A"
    return f"{v:.1f}"


def _f2(v) -> str:
    if v is None:
        return "N/A"
    return f"{v:.2f}"


def _icon(ok) -> str:
    if ok is True:
        return "✅ PASS"
    if ok is False:
        return "❌ FAIL"
    return "⚠️ N/A"


def _load(path: str):
    """Load JSON from path; return None on error."""
    try:
        with open(path) as f:
            return json.load(f)
    except (FileNotFoundError, json.JSONDecodeError):
        return None


def _trend_row(label, t):
    if not t:
        return f"| {label} | N/A | N/A | N/A | N/A | N/A | N/A | N/A |"
    return (
        f"| {label} "
        f"| {_ms(t.get('avg_ms'))} "
        f"| {_ms(t.get('min_ms'))} "
        f"| {_ms(t.get('med_ms'))} "
        f"| {_ms(t.get('p90_ms'))} "
        f"| {_ms(t.get('p95_ms'))} "
        f"| {_ms(t.get('p99_ms'))} "
        f"| {_ms(t.get('max_ms'))} |"
    )


# ── Report generation ──────────────────────────────────────────────────────────

def generate_report(report_dir, scenario_names):
    summaries = {}
    jvm_data = {}

    for name in scenario_names:
        s = _load(os.path.join(report_dir, f"{name}-summary.json"))
        if s:
            summaries[name] = s
        j = _load(os.path.join(report_dir, f"{name}-jvm.json"))
        if j:
            jvm_data[name] = j

    now = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    rd_name = os.path.basename(report_dir)

    L = []
    A = L.append  # shorthand

    # ── Header ────────────────────────────────────────────────────────────────
    A("# Chrono Queue — Load Test Report")
    A("")
    A(f"**Generated:** {now}  ")
    A(f"**Report ID:** `{rd_name}`  ")
    A(f"**Scenarios:** {', '.join(scenario_names)}  ")
    A("")

    # ── Executive Summary ──────────────────────────────────────────────────────
    A("---")
    A("")
    A("## Executive Summary")
    A("")
    A("| Scenario | Total Reqs | Req/s | Avg (ms) | p90 (ms) | p95 (ms) | p99 (ms) | Fail Rate | VUs Peak | Thresholds |")
    A("|----------|-----------|-------|----------|----------|----------|----------|-----------|---------|------------|")

    for name in scenario_names:
        s = summaries.get(name)
        if not s:
            A(f"| {name} | — | — | — | — | — | — | — | — | ⚠️ no data |")
            continue
        http = s.get("http", {}) or {}
        dur = http.get("req_duration", {}) or {}
        reqs = http.get("reqs", {}) or {}
        fail = http.get("req_failed", {}) or {}
        vus = s.get("vus", {}) or {}
        th_ok = s.get("thresholds_passed")

        total = reqs.get("count", "N/A")
        rate = _f1(reqs.get("rate"))
        avg_ms = _f1(dur.get("avg_ms"))
        p90_ms = _f1(dur.get("p90_ms"))
        p95_ms = _f1(dur.get("p95_ms"))
        p99_ms = _f1(dur.get("p99_ms"))
        fail_rt = _pct(fail.get("rate"))
        peak_vus = vus.get("max", "N/A")

        A(f"| {name} | {total} | {rate} | {avg_ms} | {p90_ms} | {p95_ms} | {p99_ms} | {fail_rt} | {peak_vus} | {_icon(th_ok)} |")

    A("")

    # ── Per-scenario detail ────────────────────────────────────────────────────
    for name in scenario_names:
        s = summaries.get(name)
        j = jvm_data.get(name)

        A("---")
        A("")
        A(f"## Scenario: {name.replace('-', ' ').title()}")
        A("")

        if not s:
            A("> ⚠️ No k6 summary data found for this scenario.")
            A("")
            continue

        A(f"**Run at:** {s.get('timestamp', 'unknown')}  ")
        A(f"**Thresholds:** {_icon(s.get('thresholds_passed'))}  ")
        A("")

        http = s.get("http", {}) or {}
        dur = http.get("req_duration",        {}) or {}
        wait = http.get("req_waiting",          {}) or {}
        send = http.get("req_sending",          {}) or {}
        recv = http.get("req_receiving",        {}) or {}
        conn = http.get("req_connecting",       {}) or {}
        reqs = http.get("reqs",                 {}) or {}
        fail = http.get("req_failed",           {}) or {}
        vus = s.get("vus",  {}) or {}
        chk = s.get("checks", {}) or {}

        # ── HTTP latency breakdown ────────────────────────────────────────────
        A("### HTTP Latency (ms)")
        A("")
        A("| Metric | avg | min | med | p90 | p95 | p99 | max |")
        A("|--------|-----|-----|-----|-----|-----|-----|-----|")
        A(_trend_row("Request Duration",  dur))
        A(_trend_row("TTFB / Waiting",    wait))
        A(_trend_row("Sending",           send))
        A(_trend_row("Receiving",         recv))
        if any(conn.get(k) for k in ("avg_ms", "p95_ms", "max_ms")):
            A(_trend_row("Connecting (TCP)", conn))
        A("")

        # ── Request counters ──────────────────────────────────────────────────
        A("### Request Statistics")
        A("")
        A("| Metric | Value |")
        A("|--------|-------|")
        A(f"| Total requests | {reqs.get('count', 'N/A')} |")
        A(f"| Request rate | {_f2(reqs.get('rate'))} req/s |")
        failed_cnt = fail.get("failed_count", "N/A")
        A(f"| Failed requests | {failed_cnt} ({_pct(fail.get('rate'))}) |")
        if chk:
            total_chk = (chk.get("passes", 0) or 0) + \
                (chk.get("fails", 0) or 0)
            A(f"| Checks passed | {chk.get('passes', 0)}/{total_chk} ({_pct(chk.get('rate'))}) |")
        A(f"| Peak VUs | {vus.get('max', 'N/A')} |")
        data_rx = s.get("data_received_bytes")
        data_tx = s.get("data_sent_bytes")
        if data_rx is not None:
            A(f"| Data received | {_mb(data_rx)} MB |")
        if data_tx is not None:
            A(f"| Data sent | {_mb(data_tx)} MB |")
        A("")

        # ── Thresholds ────────────────────────────────────────────────────────
        thresholds = s.get("thresholds", {})
        if thresholds:
            A("### Threshold Results")
            A("")
            A("| Threshold | Result |")
            A("|-----------|--------|")
            for th_name, th_ok in sorted(thresholds.items()):
                A(f"| `{th_name}` | {_icon(th_ok)} |")
            A("")

        # ── JVM / Prometheus metrics ──────────────────────────────────────────
        if j:
            jvm = j.get("jvm", {})
            kafka_lag = j.get("kafka", {})

            all_svcs = set()
            for metric_data in jvm.values():
                if isinstance(metric_data, dict):
                    all_svcs.update(metric_data.keys())
            services = sorted(all_svcs)

            if services:
                A("### JVM Metrics (from Prometheus)")
                A("")

                # Memory
                A("#### Heap Memory")
                A("")
                A("| Service | Avg Used (MB) | Max Used (MB) | Max Committed (MB) | Max Heap (MB) | Non-Heap Used Avg (MB) |")
                A("|---------|---------------|---------------|--------------------|---------------|------------------------|")
                for svc in services:
                    hu = jvm.get("heap_used_bytes",      {}).get(svc, {})
                    hc = jvm.get("heap_committed_bytes", {}).get(svc, {})
                    hm = jvm.get("heap_max_bytes",       {}).get(svc, {})
                    nh = jvm.get("nonheap_used_bytes",   {}).get(svc, {})
                    A(
                        f"| {svc} "
                        f"| {_mb(hu.get('avg'))} "
                        f"| {_mb(hu.get('max'))} "
                        f"| {_mb(hc.get('max'))} "
                        f"| {_mb(hm.get('max'))} "
                        f"| {_mb(nh.get('avg'))} |"
                    )
                A("")

                # CPU & Threads
                A("#### CPU & Threads")
                A("")
                A("| Service | Avg CPU | Max CPU | Avg Live Threads | Peak Threads | Daemon Threads |")
                A("|---------|---------|---------|-----------------|--------------|----------------|")
                for svc in services:
                    cpu = jvm.get("cpu_usage",       {}).get(svc, {})
                    thr = jvm.get("threads_live",    {}).get(svc, {})
                    pthr = jvm.get("threads_peak",    {}).get(svc, {})
                    dthr = jvm.get("threads_daemon",  {}).get(svc, {})
                    avg_thr = f"{thr.get('avg', 0):.0f}" if isinstance(
                        thr.get("avg"), (int, float)) else "N/A"
                    max_thr = f"{pthr.get('max', 0):.0f}" if isinstance(
                        pthr.get("max"), (int, float)) else "N/A"
                    dae_thr = f"{dthr.get('avg', 0):.0f}" if isinstance(
                        dthr.get("avg"), (int, float)) else "N/A"
                    A(
                        f"| {svc} "
                        f"| {_pct(cpu.get('avg'))} "
                        f"| {_pct(cpu.get('max'))} "
                        f"| {avg_thr} "
                        f"| {max_thr} "
                        f"| {dae_thr} |"
                    )
                A("")

                # GC
                A("#### Garbage Collection")
                A("")
                A("| Service | GC Pause Max (ms) | Avg GC Pause Rate (ms/s) | Avg GC Collections/s |")
                A("|---------|-------------------|--------------------------|----------------------|")
                for svc in services:
                    gc_max = jvm.get("gc_pause_max_seconds",   {}).get(svc, {})
                    gc_rate = jvm.get(
                        "gc_pause_rate_per_sec",  {}).get(svc, {})
                    gc_cnt = jvm.get("gc_collections_rate",    {}).get(svc, {})
                    max_ms_val = gc_max.get("max")
                    rate_val = gc_rate.get("avg")
                    cnt_val = gc_cnt.get("avg")
                    A(
                        f"| {svc} "
                        f"| {f'{max_ms_val * 1000:.1f}' if max_ms_val is not None else 'N/A'} "
                        f"| {f'{rate_val * 1000:.2f}' if rate_val is not None else 'N/A'} "
                        f"| {f'{cnt_val:.3f}' if cnt_val is not None else 'N/A'} |"
                    )
                A("")

                # Server-side HTTP (Micrometer)
                http_p95 = jvm.get("http_server_p95_seconds", {})
                http_p99 = jvm.get("http_server_p99_seconds", {})
                http_rr = jvm.get("http_server_req_rate",    {})
                http_5xx = jvm.get("http_server_5xx_rate",    {})
                http_4xx = jvm.get("http_server_4xx_rate",    {})
                if any([http_p95, http_p99, http_rr]):
                    A("#### Server-Side HTTP (Micrometer / Spring Boot)")
                    A("")
                    A("| Service | p95 (ms) | p99 (ms) | Avg Req/s | Avg 5xx/s | Avg 4xx/s |")
                    A("|---------|----------|----------|-----------|-----------|-----------|")
                    for svc in services:
                        p95v = http_p95.get(svc, {}).get("avg")
                        p99v = http_p99.get(svc, {}).get("avg")
                        rrv = http_rr.get(svc,  {}).get("avg")
                        e5v = http_5xx.get(svc, {}).get("avg")
                        e4v = http_4xx.get(svc, {}).get("avg")
                        A(
                            f"| {svc} "
                            f"| {f'{p95v * 1000:.1f}' if p95v is not None else 'N/A'} "
                            f"| {f'{p99v * 1000:.1f}' if p99v is not None else 'N/A'} "
                            f"| {f'{rrv:.2f}' if rrv is not None else 'N/A'} "
                            f"| {f'{e5v:.4f}' if e5v is not None else 'N/A'} "
                            f"| {f'{e4v:.4f}' if e4v is not None else 'N/A'} |"
                        )
                    A("")

            # Kafka lag
            if kafka_lag:
                A("#### Kafka Consumer Lag")
                A("")
                all_zero = all(
                    lv.get("avg_lag", 0) == 0 and lv.get(
                        "peak_lag_max", 0) == 0
                    for lv in kafka_lag.values()
                )
                A("| Consumer Group / Topic | Avg Lag | Max Lag | Peak Max Lag | Status |")
                A("|------------------------|---------|---------|--------------|--------|")
                for gt, lv in sorted(kafka_lag.items()):
                    avg_v = lv.get("avg_lag",     0.0)
                    max_v = lv.get("max_lag",     0.0)
                    peak_v = lv.get("peak_lag_max", 0.0)
                    if peak_v > 100 or max_v > 100:
                        status = "❌ High lag"
                    elif peak_v > 10 or max_v > 10:
                        status = "⚠️ Elevated"
                    else:
                        status = "✅ Healthy"
                    A(f"| `{gt}` | {avg_v} | {max_v} | {peak_v} | {status} |")
                if all_zero:
                    A("")
                    A(
                        "> ✅ All consumer lag values are 0 — consumers are processing "
                        "messages as fast as they arrive. This is expected during "
                        "light-to-moderate load scenarios."
                    )
                A("")
        else:
            A(
                "> ℹ️ No Prometheus JVM data for this scenario. "
                "Run with Prometheus scraping Spring Boot `/actuator/prometheus` "
                "for heap, GC, CPU and thread metrics."
            )
            A("")

    # ── Insights ─────────────────────────────────────────────────────────────
    A("---")
    A("")
    A("## Insights & Recommendations")
    A("")

    insights = []

    for name in scenario_names:
        s = summaries.get(name)
        if not s:
            continue
        http = s.get("http", {}) or {}
        dur = http.get("req_duration", {}) or {}
        fail = http.get("req_failed",   {}) or {}
        thresholds = s.get("thresholds", {}) or {}

        fail_rate = fail.get("rate") or 0.0
        p95_val = dur.get("p95_ms") or 0.0
        p99_val = dur.get("p99_ms") or 0.0
        th_ok = s.get("thresholds_passed")

        if fail_rate > 0.01:
            insights.append(
                f"- **[{name}]** Error rate **{_pct(fail_rate)}** exceeds 1% — "
                f"review DLQ consumer logs (`docker logs dlq`) and retry-dispatcher."
            )
        if p95_val > 1000:
            insights.append(
                f"- **[{name}]** p95 latency **{p95_val:.0f} ms** > 1 s — "
                f"consider increasing Kafka `max.poll.records`, worker thread pool, "
                f"or Redis connection pool size."
            )
        if p99_val > 2000:
            insights.append(
                f"- **[{name}]** p99 latency **{p99_val:.0f} ms** > 2 s — "
                f"tail latency spike; inspect GC pauses (GC pressure section above) "
                f"and Kafka consumer lag."
            )
        if th_ok is False:
            failed_ths = [k for k, v in thresholds.items() if not v]
            insights.append(
                f"- **[{name}]** Failed thresholds: "
                + ", ".join(f"`{t}`" for t in failed_ths)
                + "."
            )

        # JVM-based heap pressure warning
        jv = jvm_data.get(name, {}).get("jvm", {})
        for svc, hu in jv.get("heap_used_bytes", {}).items():
            hm_entry = jv.get("heap_max_bytes", {}).get(svc, {})
            if hu.get("max") and hm_entry.get("max"):
                ratio = hu["max"] / hm_entry["max"]
                if ratio > 0.85:
                    insights.append(
                        f"- **[{name}] {svc}** heap reached "
                        f"**{ratio * 100:.0f}%** of max "
                        f"({_mb(hu['max'])} / {_mb(hm_entry['max'])} MB) — "
                        f"consider raising `-Xmx` or tuning GC."
                    )

    if insights:
        L.extend(insights)
    else:
        A("- All tested scenarios completed within defined thresholds. ✅")
        A(
            "- Consider re-running the **soak** scenario for ≥ 60 minutes to "
            "detect slow memory leaks."
        )
        A(
            "- Run **retry-storm** with `RATE=200` to probe the retry/DLQ path "
            "under heavier pressure."
        )
        A(
            "- Check **Grafana → Chrono Kafka & MongoDB** for consumer-lag trends "
            "during the load window."
        )

    A("")

    # ── Reference tables ──────────────────────────────────────────────────────
    A("---")
    A("")
    A("## Grafana Dashboards")
    A("")
    A("Open **http://localhost:3000** (default credentials: `admin` / `admin`) and")
    A("navigate to these auto-provisioned dashboards for live or historical data:")
    A("")
    A("| Dashboard | What to look for |")
    A("|-----------|-----------------|")
    A("| **Chrono API Endpoints** | HTTP throughput, status codes, request rate per endpoint |")
    A("| **Chrono Service Latency** | p50 / p95 / p99 latency per Spring Boot service |")
    A("| **Chrono Kafka & MongoDB** | Consumer lag (per topic), produce/consume rate, MongoDB ops |")
    A("| **Chrono Error Monitoring** | 4xx/5xx error rates, DLQ event count, retry exhaustion |")
    A("| **Chrono Logs Overview** | Raw structured logs via Loki — filter by service or level |")
    A("")

    # ── Re-run commands ───────────────────────────────────────────────────────
    A("---")
    A("")
    A("## Re-run Commands")
    A("")
    A("```bash")
    A("# Full report — runs all 4 scenarios then compiles this document")
    A("make load-test-report")
    A("")
    A("# Quick report — smoke + throughput only (skips 30-min soak)")
    A("make load-test-report-quick")
    A("")
    A("# Individual scenarios")
    A("make load-test-smoke")
    A("make load-test-throughput")
    A("make load-test-retry-storm")
    A("make load-test-soak")
    A("")
    A("# Custom parameters")
    A("RATE=200 DURATION=10m make load-test-retry-storm")
    A("VUS=10 DURATION=2m  make load-test-smoke")
    A("API_BASE_URL=http://my-host:8081 make load-test-throughput")
    A("")
    A("# Skip Prometheus queries (no stack running)")
    A("SKIP_PROMETHEUS=1 ./load-tests/generate-report.sh smoke")
    A("")
    A("# Re-generate report from an existing results directory without re-running tests")
    A("python3 load-tests/parse-results.py report \\")
    A("    --report-dir load-tests/results/report-<TS> \\")
    A("    --scenarios 'smoke throughput retry-storm soak'")
    A("")
    A("# Collect Prometheus data for a manual time window (e.g. last 15 minutes)")
    A("python3 load-tests/parse-results.py collect-prometheus \\")
    A("    --scenario manual \\")
    A("    --start $(date -d '15 minutes ago' +%s) \\")
    A("    --end   $(date +%s) \\")
    A("    --prom-url http://localhost:9090 \\")
    A("    --out /tmp/manual-jvm.json")
    A("```")
    A("")
    A("---")
    A("")
    A("_Report generated by `load-tests/generate-report.sh` · Chrono Queue_")

    return "\n".join(L)


# ── CLI ────────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Chrono Queue — load-test report tools",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    sub = parser.add_subparsers(dest="cmd", required=True)

    # collect-prometheus sub-command
    cp = sub.add_parser(
        "collect-prometheus",
        help="Query Prometheus and save JVM metrics as JSON",
    )
    cp.add_argument("--scenario",  required=True,
                    help="Scenario name (e.g. smoke)")
    cp.add_argument("--start",     required=True,
                    type=int, help="Unix epoch start")
    cp.add_argument("--end",       required=True,
                    type=int, help="Unix epoch end")
    cp.add_argument("--prom-url",  default="http://localhost:9090")
    cp.add_argument("--out",       required=True, help="Output JSON file path")

    # report sub-command
    rp = sub.add_parser(
        "report",
        help="Generate Markdown report from collected JSON files",
    )
    rp.add_argument("--report-dir", required=True,
                    help="Directory with summary JSON files")
    rp.add_argument(
        "--scenarios",
        default="smoke throughput retry-storm soak",
        help="Space-separated list of scenario names",
    )

    args = parser.parse_args()

    if args.cmd == "collect-prometheus":
        collect_prometheus(
            scenario=args.scenario,
            start_ts=args.start,
            end_ts=args.end,
            prom_url=args.prom_url,
            out_file=args.out,
        )
    elif args.cmd == "report":
        scenario_list = args.scenarios.split()
        print(generate_report(args.report_dir, scenario_list))


if __name__ == "__main__":
    main()
