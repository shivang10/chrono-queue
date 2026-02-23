import { submitJob } from '../lib/http.js';

export const options = {
  scenarios: {
    soak: {
      executor: 'constant-arrival-rate',
      rate: Number(__ENV.RATE || 20),
      timeUnit: '1s',
      duration: __ENV.DURATION || '30m',
      preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 50),
      maxVUs: Number(__ENV.MAX_VUS || 300),
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<700'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

export default function () {
  submitJob();
}
