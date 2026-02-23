import { submitJob } from '../lib/http.js';

export const options = {
  scenarios: {
    retry_storm: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 120),
      timeUnit: '1s',
      duration: __ENV.DURATION || '5m',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 200),
      maxVUs: Number(__ENV.MAX_VUS || 600),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.03'],
    http_req_duration: ['p(95)<1000'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  submitJob();
}
