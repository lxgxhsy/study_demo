import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

export const options = {
  vus: Number(__ENV.VUS || 16),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
    duplicate_ids: ['count==0'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const BIZ_TAG = __ENV.BIZ_TAG || 'order';
const BATCH_SIZE = Number(__ENV.BATCH_SIZE || 100);
const SLEEP_SECONDS = Number(__ENV.SLEEP_BETWEEN_REQUESTS || 0.1);

const duplicateIds = new Counter('duplicate_ids');
const generatedIds = new Counter('generated_ids');
const batchDuration = new Trend('id_batch_duration_ms');

export default function () {
  const payload = JSON.stringify({ count: BATCH_SIZE });
  const started = Date.now();
  const res = http.post(`${BASE_URL}/api/lab/id/${BIZ_TAG}/batch`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  batchDuration.add(Date.now() - started);

  const ok = check(res, {
    'id batch status is 200': (r) => r.status === 200,
    'id batch has no duplicate inside response': (r) => Number(r.json('duplicateCount') || 0) === 0,
    'id batch count matches request': (r) => Number(r.json('count') || 0) === BATCH_SIZE,
  });

  if (ok) {
    generatedIds.add(Number(res.json('count') || 0));
    duplicateIds.add(Number(res.json('duplicateCount') || 0));
  }

  sleep(SLEEP_SECONDS);
}

export function teardown() {
  const metrics = http.get(`${BASE_URL}/api/lab/id/${BIZ_TAG}/metrics`);
  console.log(`LEAF_ID_METRICS=${metrics.body}`);
}
