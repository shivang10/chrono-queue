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

export function getDlqJobs() {
  const response = http.get(`${DLQ_BASE_URL}/api/dlq/failed-jobs`, {
    tags: { endpoint: 'dlq_failed_jobs' },
  });

  check(response, {
    'dlq status is 200': (r) => r.status === 200,
  });

  return response;
}
