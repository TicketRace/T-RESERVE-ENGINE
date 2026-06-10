/**
 * T-RESERVE — нагрузочное тестирование
 *
 * Сценарий:
 *   1. Рампап: 0 → 50 пользователей за 30 сек
 *   2. Пик: 50 пользователей × 2 минуты (основная нагрузка)
 *   3. Стресс: 0 → 100 пользователей за 30 сек
 *   4. Удержание: 100 пользователей × 1 минута
 *   5. Завершение: рампдаун 30 сек
 *
 * Запуск:
 *   Все тесты (полный режим):   k6 run load-tests/k6-load-test.js
 *   Только race condition:      k6 run -e SCENARIO=race load-tests/k6-load-test.js
 *   Только mixed flow:          k6 run -e SCENARIO=mixed load-tests/k6-load-test.js
 *   Локальный (лёгкий):         k6 run -e LOCAL=true load-tests/k6-load-test.js
 *   Кастомный URL:              k6 run -e BASE_URL=http://myserver load-tests/k6-load-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── Конфигурация окружения ────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const IS_LOCAL = __ENV.LOCAL === 'true';
// LOCAL=true → лёгкий режим для локальной разработки (1 инстанс, нет Redis)
// По умолчанию → полная нагрузка для docker-compose стека

// ─── Кастомные метрики ────────────────────────────────────────────────────────
const lockSuccess  = new Counter('booking_lock_success');
const lockConflict = new Counter('booking_lock_conflict');
const errorRate    = new Rate('error_rate');
const lockDuration = new Trend('booking_lock_duration_ms', true);

// ─── Нагрузочные параметры ────────────────────────────────────────────────────
const TARGET_SCENARIO = __ENV.SCENARIO || 'all'; // 'all', 'mixed', 'race'

let activeScenarios = {};
let activeThresholds = {};

if (TARGET_SCENARIO === 'all' || TARGET_SCENARIO === 'mixed') {
  activeScenarios.mixed_flow = {
    executor: 'ramping-vus',
    startVUs: 0,
    // Лёгкий режим: 1 инстанс без Redis. 
    // Полный режим: docker-compose (2 инстанса + Redis + RabbitMQ)
    stages: IS_LOCAL
      ? [ { duration: '30s', target: 10 }, { duration: '1m', target: 20 }, { duration: '30s', target: 0 } ]
      : [ { duration: '30s', target: 50 }, { duration: '2m', target: 50 }, { duration: '30s', target: 100 }, { duration: '1m', target: 100 }, { duration: '30s', target: 0 } ],
    exec: 'mixedFlow',
  };
  activeThresholds['http_req_duration'] = IS_LOCAL ? ['p(99)<3000'] : ['p(99)<1000'];
  activeThresholds['error_rate'] = IS_LOCAL ? ['rate<0.05'] : ['rate<0.01'];
  if (!IS_LOCAL) {
    activeThresholds['http_req_duration{name:seats}'] = ['p(95)<500'];
  }
}

if ((TARGET_SCENARIO === 'all' && !IS_LOCAL) || TARGET_SCENARIO === 'race') {
  activeScenarios.race_condition = {
    executor: 'shared-iterations',
    vus: 1000,
    iterations: 1000,
    maxDuration: '10s',
    exec: 'raceCondition',
  };
  activeThresholds['http_req_duration{name:lock}'] = ['p(99)<50'];
  activeThresholds['booking_lock_success{scenario:race_condition}'] = ['count===1'];
  activeThresholds['booking_lock_conflict{scenario:race_condition}'] = ['count===999'];
}

export const options = {
  scenarios: activeScenarios,
  thresholds: activeThresholds,
};

// ─── Логин один раз перед всеми тестами ───────────────────────────────────────
// setup() запускается один раз, возвращает данные доступные всем VU
export function setup() {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email: 'user@treserve.com', password: 'user123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const ok = check(res, {
    'setup: login 200': r => r.status === 200,
    'setup: has token': r => r.json('token') !== null, // ответ содержит "token"
  });

  if (!ok) {
    console.error(`Логин провалился: ${res.status} ${res.body}`);
  }

  return { token: res.json('token') }; // пробрасываем токен для всех VU
}

// ─── Сценарий 1: Смешанная нагрузка (поиск, карта мест, бронь) ───────────────
export function mixedFlow(data) {
  // Используем токен из setup() — без логина на каждую итерацию
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // ─ 1. Просмотр событий ─
  group('browse events', () => {
    const res = http.get(`${BASE_URL}/api/events`, {
      headers,
      tags: { name: 'events' },
    });
    check(res, { 'events 200': r => r.status === 200 });
    errorRate.add(res.status !== 200);
  });

  sleep(0.3);

  // ─ 2. Карта мест (кэшируется Redis) ─
  group('seat map', () => {
    const eventId = Math.floor(Math.random() * 3) + 1; // события 1-3
    const res = http.get(`${BASE_URL}/api/events/${eventId}/seats`, {
      headers,
      tags: { name: 'seats' },
    });
    check(res, { 'seats 200': r => r.status === 200 });
    errorRate.add(res.status !== 200);
  });

  sleep(0.3);

  // ─ 3. Попытка блокировки места (POST) — pessimistic locking ─
  group('lock seat', () => {
    const eventId = Math.floor(Math.random() * 3) + 1;
    let seatId;
    if (eventId === 3) {
      seatId = Math.floor(Math.random() * 200) + 51; // 51-250
    } else {
      seatId = Math.floor(Math.random() * 50) + 1; // 1-50
    }

    const start = Date.now();
    const res = http.post(
      `${BASE_URL}/api/bookings/lock`,
      JSON.stringify({ eventId, seatId }),
      { headers, tags: { name: 'lock' } }
    );
    lockDuration.add(Date.now() - start);

    if (res.status === 200) {
      lockSuccess.add(1);
      errorRate.add(false);

      // ─ 4. Отмена бронирования (чтобы не портить seed БД) ─
      const lockId = res.json('lockId');
      if (lockId) {
        sleep(0.2);
        const cancelRes = http.del(
          `${BASE_URL}/api/bookings/${lockId}`,
          null,
          { headers, tags: { name: 'cancel' } }
        );
        check(cancelRes, { 'cancel ok': r => r.status === 200 || r.status === 204 });
        errorRate.add(cancelRes.status >= 500); // только 5xx — ошибки, 4xx — бизнес-логика
      }
    } else if (res.status === 409) {
      // Место уже занято — это нормально при высокой нагрузке
      lockConflict.add(1);
      errorRate.add(false); // конфликт — это нормально
    } else if (res.status === 400 || res.status === 404) {
      errorRate.add(false); // бизнес-логика, не ошибка сервера
    } else {
      errorRate.add(true);  // только 5xx считаем ошибками
    }
  });

  sleep(0.8);
}

// ─── Сценарий 2: Гонка данных (Race Condition 1000 VU) ───────────────────────
export function raceCondition(data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // Все 1000 пользователей одновременно ломятся на Event 1, Seat 1
  const eventId = 1;
  const seatId = 1;

  const start = Date.now();
  const res = http.post(
    `${BASE_URL}/api/bookings/lock`,
    JSON.stringify({ eventId, seatId }),
    { headers, tags: { name: 'lock' } }
  );
  lockDuration.add(Date.now() - start);

  if (res.status === 200) {
    lockSuccess.add(1, { scenario: 'race_condition' });
    errorRate.add(false);
  } else if (res.status === 409) {
    lockConflict.add(1, { scenario: 'race_condition' });
    errorRate.add(false);
  } else {
    errorRate.add(true);
  }
}

// ─── Итоговый отчёт ───────────────────────────────────────────────────────────
export function handleSummary(data) {
  return {
    'load-tests/results/summary.json': JSON.stringify(data, null, 2),
  };
}
