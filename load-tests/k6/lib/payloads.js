import { JOB_TYPES } from './config.js';

const payloadTemplates = JSON.parse(open('../../data/job-payloads.json'));

function randomInt(maxExclusive) {
  return Math.floor(Math.random() * maxExclusive);
}

function pickRandomJobType() {
  return JOB_TYPES[randomInt(JOB_TYPES.length)];
}

export function buildJobRequest() {
  const jobType = pickRandomJobType();
  const template = payloadTemplates[jobType] || {};

  return {
    jobType,
    payload: {
      ...template,
      requestId: `lt-${Date.now()}-${randomInt(1_000_000)}`,
      sentAt: new Date().toISOString(),
    },
  };
}
