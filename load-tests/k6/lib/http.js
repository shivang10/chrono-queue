import http from 'k6/http';
import { check } from 'k6';
import { API_BASE_URL, DLQ_BASE_URL, DEFAULT_HEADERS } from './config.js';
import { buildJobRequest } from './payloads.js';

export function submitJob() {
  const body = JSON.stringify(buildJobRequest());
  const response = http.post(`${API_BASE_URL}/api/job/`, body, {
    headers: DEFAULT_HEADERS,
    tags: { endpoint: 'create_job' },
  });

  check(response, {
    'status is 202': (r) => r.status === 202,
  });

  return response;
}

// Invalid payloads that intentionally exercise 4xx paths so the
// "4xx / 5xx Error Rate Over Time" Grafana panel has data to display.
const INVALID_BODIES = [
  // Missing jobType — fails @NotNull bean validation → 400
  JSON.stringify({ payload: { recipient: 'test@example.com' } }),
  // Unknown enum value — Jackson cannot deserialize JobType → 400
  JSON.stringify({ jobType: 'UNKNOWN_TYPE', payload: {} }),
  // Valid jobType but completely missing payload — fails @NotNull → 400
  JSON.stringify({ jobType: 'EMAIL' }),
];

export function submitInvalidJob() {
  const body = INVALID_BODIES[Math.floor(Math.random() * INVALID_BODIES.length)];
  const response = http.post(`${API_BASE_URL}/api/job/`, body, {
    headers: DEFAULT_HEADERS,
    tags: { endpoint: 'create_job_invalid' },
  });

  check(response, {
    'invalid job rejected with 4xx': (r) => r.status >= 400 && r.status < 500,
  });

  return response;
}

export function getDlqJobs() {
  const response = http.get(`${DLQ_BASE_URL}/api/dlq/failed-jobs`, {
    tags: { endpoint: 'dlq_failed_jobs' },
  });

  check(response, {
    'dlq status is 200': (r) => r.status === 200,
  });

  return response;
}
