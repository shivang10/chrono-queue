export const API_BASE_URL = __ENV.API_BASE_URL || 'http://localhost:8081';
export const DLQ_BASE_URL = __ENV.DLQ_BASE_URL || 'http://localhost:8084';

export const DEFAULT_HEADERS = {
  'Content-Type': 'application/json',
};

export const JOB_TYPES = [
  'EMAIL',
  'WEBHOOK',
  'PAYMENT_PROCESSING',
  'ORDER_CANCELLATION',
];
