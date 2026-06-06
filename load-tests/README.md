# Нагрузочное тестирование T-RESERVE

## Инструмент: k6

### Установка k6

**Windows (Chocolatey):**
```bash
choco install k6
```

**Windows (прямая установка):**  
Скачать с https://github.com/grafana/k6/releases и добавить в PATH

---

## Запуск тестов

### 1. Поднять всё окружение
```bash
docker compose -f docker-compose.prod.yml up --build -d
```

### 2. Запустить нагрузочный тест
```bash
k6 run load-tests/k6-load-test.js
```

### 3. Запустить с кастомным базовым URL (например, продакшен)
```bash
k6 run -e BASE_URL=http://your-server load-tests/k6-load-test.js
```

---

## Сценарий нагрузки

| Фаза        | Время | Пользователи |
|-------------|-------|--------------|
| Рампап      | 30s   | 0 → 50       |
| Пик         | 2m    | 50           |
| Стресс      | 30s   | 50 → 100     |
| Удержание   | 1m    | 100          |
| Рампдаун    | 30s   | 100 → 0      |

**Итого: ~4.5 минуты**

---

## Пороги (SLA)

| Метрика                | Порог       |
|------------------------|-------------|
| p99 latency (общий)    | < 1000ms    |
| p95 latency (seats)    | < 500ms     |
| Error rate             | < 1%        |

---

## Мониторинг в реальном времени

Пока тест идёт — открыть Grafana:
```
http://localhost:3000
login: admin / admin
```

Dashboard: **T-RESERVE — High Load Monitor**

---

## Что тестируем

1. `GET /api/events` — просмотр событий (высокая нагрузка)
2. `GET /api/events/{id}/seats` — карта мест (Redis кэш hit rate)
3. `POST /api/bookings/lock` — pessimistic locking под нагрузкой
4. `POST /api/bookings/{id}/confirm` — подтверждение брони

---

## Результаты

Сохраняются в `load-tests/results/summary.json` после каждого запуска.
