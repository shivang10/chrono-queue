import http from 'k6/http';
import { check } from 'k6';
import { API_BASE_URL, DEFAULT_HEADERS } from './config.js';
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
