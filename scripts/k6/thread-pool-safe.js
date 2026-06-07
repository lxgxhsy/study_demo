import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Counter } from 'k6/metrics';

export const options = {
  vus: Number(__ENV.VUS || 16),
  duration: __ENV.DURATION || '30s',
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TASK_COUNT = Number(__ENV.TASK_COUNT || 20);
const DURATION_MS = Number(__ENV.TASK_DURATION_MS || 100);

const acceptedTasks = new Counter('accepted_tasks');
const rejectedTasks = new Counter('rejected_tasks');
const taskSubmitDuration = new Trend('task_submit_duration_ms');

export function setup() {
  http.post(`${BASE_URL}/api/lab/thread-pool/metrics/reset`);

  const configPayload = JSON.stringify({
    corePoolSize: Number(__ENV.CORE_POOL_SIZE || 4),
    maximumPoolSize: Number(__ENV.MAX_POOL_SIZE || 8),
    queueCapacity: Number(__ENV.QUEUE_CAPACITY || 64),
    keepAliveSeconds: Number(__ENV.KEEP_ALIVE_SECONDS || 60),
    allowCoreThreadTimeOut: false,
    rejectionPolicy: __ENV.REJECTION_POLICY || 'ABORT',
  });

  const res = http.put(`${BASE_URL}/api/lab/thread-pool/config`, configPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  check(res, {
    'config update accepted': (r) => r.status === 200,
  });
}

export default function () {
  const payload = JSON.stringify({
    count: TASK_COUNT,
    durationMs: DURATION_MS,
  });

  const started = Date.now();
  const res = http.post(`${BASE_URL}/api/lab/thread-pool/tasks/sleep`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
  taskSubmitDuration.add(Date.now() - started);

  const ok = check(res, {
    'task submit status is 200': (r) => r.status === 200,
    'task submit has requestId': (r) => Boolean(r.json('requestId')),
  });

  if (ok) {
    acceptedTasks.add(Number(res.json('acceptedCount') || 0));
    rejectedTasks.add(Number(res.json('rejectedCount') || 0));
  }

  sleep(Number(__ENV.SLEEP_BETWEEN_REQUESTS || 0.2));
}

export function teardown() {
  const metrics = http.get(`${BASE_URL}/api/lab/thread-pool/metrics`);
  console.log(`THREAD_POOL_METRICS=${metrics.body}`);
}
