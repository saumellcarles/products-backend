import http from 'k6/http';
import { check } from 'k6';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

// Mirrors the scenarios described in the README's resilience section
// (normal, a similar product that 404s, one that 5xxs, one that exceeds
// the response timeout, and the root product lookup itself failing)
// against the WireMock stub mappings in k6/wiremock/mappings.
const BASE_URL = __ENV.BASE_URL || 'http://localhost:5000';

const SCENARIOS = [
  { productId: '1', expectedStatus: 200, name: 'normal' },
  { productId: '5', expectedStatus: 200, name: 'similar-not-found' },
  { productId: '6', expectedStatus: 200, name: 'similar-server-error' },
  { productId: '7', expectedStatus: 200, name: 'similar-timeout' },
  { productId: '999', expectedStatus: 404, name: 'root-not-found' },
  { productId: '998', expectedStatus: 502, name: 'root-upstream-error' },
];

export const options = {
  scenarios: {
    similar_products: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 20 },
        { duration: '20s', target: 20 },
        { duration: '5s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<2500'],
    checks: ['rate>0.99'],
  },
};

http.setResponseCallback(http.expectedStatuses(200, 404, 502));

export default function () {
  const scenario = SCENARIOS[Math.floor(Math.random() * SCENARIOS.length)];
  const response = http.get(`${BASE_URL}/product/${scenario.productId}/similar`, {
    tags: { scenario: scenario.name },
  });

  check(response, {
    [`${scenario.name}: status is ${scenario.expectedStatus}`]: (r) => r.status === scenario.expectedStatus,
  });
}

export function handleSummary(data) {
  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
    'k6/summary.json': JSON.stringify(data, null, 2),
    'k6/summary.html': htmlReport(data),
  };
}
