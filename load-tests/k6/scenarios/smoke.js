import { sleep } from 'k6';
import { submitJob, submitInvalidJob, getDlqJobs } from '../lib/http.js';
import { buildJsonSummary } from '../lib/summary.js';

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: Number(__ENV.VUS || 5),
      duration: __ENV.DURATION || '1m',
    },
    // Sends intentionally malformed requests at a low rate so the
    // "4xx / 5xx Error Rate Over Time" Grafana panel has real data.
    bad_requests: {
      executor: 'constant-arrival-rate',
      rate: 1,
      timeUnit: '10s',
      duration: __ENV.DURATION || '1m',
      preAllocatedVUs: 1,
      maxVUs: 2,
      exec: 'sendInvalidJob',
    },
    dlq_polling: {
      executor: 'constant-arrival-rate',
      rate: 1,
      timeUnit: '5s',
      duration: __ENV.DURATION || '1m',
      preAllocatedVUs: 1,
      maxVUs: 2,
      exec: 'pollDlqApi',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<500'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  submitJob();
  sleep(Math.random());
}

export function sendInvalidJob() {
  submitInvalidJob();
}

export function pollDlqApi() {
  getDlqJobs();
}

export function handleSummary(data) {
  return {
    'load-tests/results/smoke-summary.json':
      JSON.stringify(buildJsonSummary(data, 'smoke'), null, 2),
  };
}
