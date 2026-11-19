// Prueba de rendimiento del backend core con k6.
// Uso: k6 run -e BASE_URL=http://localhost:8080 tests/k6/carga-api.js
// Requiere el backend en perfil dev (admin sembrado). Sube el rate limit del
// backend (RATE_LIMIT_CAPACITY / RATE_LIMIT_REFILL_PER_MINUTE) o el test medirá 429.
import http from 'k6/http'
import { check, sleep } from 'k6'

const BASE = __ENV.BASE_URL || 'http://localhost:8080'

export const options = {
  stages: [
    { duration: '15s', target: 10 },
    { duration: '30s', target: 20 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800'],
  },
}

export function setup() {
  const r = http.post(
    `${BASE}/api/v1/auth/login`,
    JSON.stringify({ correo: __ENV.ADMIN_CORREO || 'admin@dubss.com', contrasena: __ENV.ADMIN_PASSWORD || '123123123' }),
    { headers: { 'Content-Type': 'application/json' } },
  )
  check(r, { 'login 200': (x) => x.status === 200 })
  return { token: r.json('data.accessToken') }
}

export default function (data) {
  const h = { headers: { Authorization: `Bearer ${data.token}` } }
  const res = http.batch([
    ['GET', `${BASE}/health`],
    ['GET', `${BASE}/api/v1/carreras`],
    ['GET', `${BASE}/api/v1/convocatorias?page=1&pageSize=10`, null, h],
    ['GET', `${BASE}/api/v1/dashboard/indicadores`, null, h],
    ['GET', `${BASE}/api/v1/bitacora?page=1&pageSize=10`, null, h],
  ])
  res.forEach((r) => check(r, { 'status 200': (x) => x.status === 200 }))
  sleep(1)
}
