import { submitJob, submitInvalidJob, getDlqJobs } from '../lib/http.js';
import { buildJsonSummary } from '../lib/summary.js';

export const options = {
  scenarios: {
    throughput: {
      executor: 'ramping-arrival-rate',
      startRate: Number(__ENV.START_RATE || 20),
      timeUnit: '1s',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 100),
      maxVUs: Number(__ENV.MAX_VUS || 500),
      stages: [
        { target: Number(__ENV.STAGE1_TARGET || 100), duration: __ENV.STAGE1_DURATION || '2m' },
        // { target: Number(__ENV.STAGE2_TARGET || 100), duration: __ENV.STAGE2_DURATION || '3m' },
        // { target: Number(__ENV.STAGE3_TARGET || 150), duration: __ENV.STAGE3_DURATION || '3m' },
        { target: 0, duration: __ENV.STAGE4_DURATION || '1m' },
      ],
    },
    // Sends intentionally malformed requests at a low rate so the
    // "4xx / 5xx Error Rate Over Time" Grafana panel has real data.
    bad_requests: {
      executor: 'constant-arrival-rate',
      rate: 2,
      timeUnit: '1s',
      duration: __ENV.DURATION || '9m',
      preAllocatedVUs: 2,
      maxVUs: 5,
      exec: 'sendInvalidJob',
    },
    dlq_polling: {
      executor: 'constant-arrival-rate',
      rate: 1,
      timeUnit: '5s',
      duration: __ENV.DURATION || '9m',
      preAllocatedVUs: 1,
      maxVUs: 2,
      exec: 'pollDlqApi',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.02'],
    http_req_duration: ['p(95)<800', 'p(99)<1500'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  submitJob();
}

export function sendInvalidJob() {
  submitInvalidJob();
}

export function pollDlqApi() {
  getDlqJobs();
}

export function handleSummary(data) {
  return {
    'load-tests/results/throughput-summary.json':
      JSON.stringify(buildJsonSummary(data, 'throughput'), null, 2),
  };
}
