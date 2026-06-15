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
 *   Локальный (лёгкий):   k6 run -e LOCAL=true load-tests/k6-load-test.js
 *   Docker-compose (полный): k6 run load-tests/k6-load-test.js
 *   Кастомный URL:        k6 run -e BASE_URL=http://myserver load-tests/k6-load-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── Конфигурация окружения ────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost';
const IS_LOCAL = __ENV.LOCAL === 'true';
// LOCAL=true → лёгкий режим для локальной разработки (1 инстанс, нет Redis)
// По умолчанию → полная нагрузка для docker-compose стека

// ─── Кастомные метрики ────────────────────────────────────────────────────────
const lockSuccess  = new Counter('booking_lock_success');
const lockConflict = new Counter('booking_lock_conflict');
const errorRate    = new Rate('error_rate');
const lockDuration = new Trend('booking_lock_duration_ms', true);

// ─── Нагрузочные параметры ────────────────────────────────────────────────────
export const options = IS_LOCAL
  ? {
      // Лёгкий режим: 1 инстанс без Redis
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m',  target: 20 },
        { duration: '30s', target: 0  },
      ],
      thresholds: {
        'http_req_duration': ['p(99)<3000'],
        'error_rate':        ['rate<0.05'],
      },
    }
  : {
      // Полный режим: docker-compose (2 инстанса + Redis + RabbitMQ)
      stages: [
        { duration: '30s', target: 50  },
        { duration: '2m',  target: 50  },
        { duration: '30s', target: 100 },
        { duration: '1m',  target: 100 },
        { duration: '30s', target: 0   },
      ],
      thresholds: {
        'http_req_duration':             ['p(99)<2000'],   // 2s — реалистично для нагрузки
        'error_rate':                    ['rate<0.02'],    // 2% — с учётом 409 конфликтов
        'http_req_duration{name:seats}': ['p(95)<1000'],  // 1s для карты мест
      },
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
    'setup: has token': r => r.json('token') !== null,  // ответ содержит "token", не "accessToken"
  });

  if (!ok) {
    console.error(`Логин провалился: ${res.status} ${res.body}`);
  }

  return { token: res.json('token') };  // исправлено: было accessToken
}

// ─── Основной сценарий ────────────────────────────────────────────────────────
export default function (data) {
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
    const seatId  = Math.floor(Math.random() * 50) + 1;

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

      // ─ 4. Подтверждение бронирования ─
      const lockId = res.json('lockId');
      if (lockId) {
        sleep(0.2);
        const confirmRes = http.post(
          `${BASE_URL}/api/bookings/${lockId}/confirm`,
          '',  // пустое тело, не null
          { headers, tags: { name: 'confirm' } }
        );
        check(confirmRes, { 'confirm ok': r => r.status === 200 || r.status === 204 });
        errorRate.add(confirmRes.status >= 500); // только 5xx — ошибки, 4xx — бизнес-логика
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

// ─── Итоговый отчёт ───────────────────────────────────────────────────────────
export function handleSummary(data) {
  return {
    'load-tests/results/summary.json': JSON.stringify(data, null, 2),
  };
}
