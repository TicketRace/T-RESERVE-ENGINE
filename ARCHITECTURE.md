# Архитектура T-RESERVE ENGINE

> Высоконагруженная система бронирования билетов с защитой от Race Condition

> Команда: 5 чел | Срок: 6–7 недель

---

## Технологический стек

| Слой | Технология | Обоснование |
|---|---|---|
| Runtime | **Java 21** (Virtual Threads) | 1000+ concurrent без reactive |
| Framework | **Spring Boot 3.3** | Security, Data JPA, Actuator |
| Database | **PostgreSQL 16** | `SELECT FOR UPDATE NOWAIT` — ядро Race Condition protection |
| Cache | **Redis 7** | `@Cacheable` для карты мест (TTL 10 сек) |
| Message Broker | **RabbitMQ 3.13** | Зарезервировано для v2 (уведомления) |
| Auth | **JWT** (jjwt) | Stateless, роли USER/ADMIN |
| Frontend | **Angular** | SPA, polling карты мест каждые 3 сек |
| Migrations | **Flyway** | Version-controlled schema |
| CI | **GitHub Actions** | `mvn verify` + PG + Redis на каждый PR |
| Контейнеризация | **Docker Compose** | Один `docker compose up` для запуска |

---

## PostgreSQL vs Redis Lock

**Выбрали:** PostgreSQL `SELECT FOR UPDATE NOWAIT`
 вместо Redis `SETNX` (distributed lock)

### Почему

```
Юзер кликнул место A-1 → Spring Boot → BEGIN TRANSACTION
  → SELECT * FROM tickets WHERE event_id=1 AND seat_id=1 AND status='AVAILABLE'
    FOR UPDATE NOWAIT
  → Строка свободна? → UPDATE status='LOCKED', user_id=X
  → COMMIT

Другой юзер в это же время → FOR UPDATE NOWAIT
  → PostgreSQL: "строка заблокирована" → мгновенная ошибка → 409 Conflict
```

**Преимущества PG (MVP):**
- Один источник правды (ACID)
- Нет рассинхрона Redis ↔ PG
- `NOWAIT` = мгновенный ответ, без ожидания

**В v2 (Redis SETNX):**
Если нагрузочный тест покажет bottleneck на PG — Redis `SETNX` станет первым барьером (0.5ms vs 5ms), PG останется как финальная запись. Двухуровневая блокировка: Redis фильтрует, PG фиксирует.

---

## Жизненный цикл билета (State Machine)

```
AVAILABLE ──[lock]──→ LOCKED ──[confirm]──→ BOOKED
                        │
                        ├──[cancel]──→ AVAILABLE
                        └──[timeout 10 мин]──→ AVAILABLE (SafetyNet)
```

| Переход | Триггер | Механизм |
|---|---|---|
| AVAILABLE → LOCKED | `POST /bookings/lock` | PG `FOR UPDATE NOWAIT` |
| LOCKED → BOOKED | `POST /bookings/{id}/confirm` | Проверка user + TTL |
| LOCKED → AVAILABLE | `DELETE /bookings/{id}` | Ручная отмена |
| LOCKED → AVAILABLE | SafetyNet (30 сек) | `@Scheduled` авто-отмена |

---

## Кэширование (Redis)

```
GET /api/events/{id}/seats — polling каждые 3 сек

Без кэша: 100 юзеров × 0.33 RPS = 33 SQL запроса/сек к PG
С кэшем:  1 SQL запрос каждые 10 сек на ивент (TTL)
```

**Стратегия:** Cache-Aside
- `@Cacheable("seats")` — при cache miss → PG → Redis
- `@CacheEvict("seats")` — при lock/confirm/cancel/safety-net

---

## Структура проекта

```
src/main/java/com/treserve/
├── auth/           # JWT: регистрация, логин, refresh
├── booking/        # ЯДРО: lock, confirm, cancel, SafetyNet
│   ├── BookingService.java    — бизнес-логика бронирования
│   ├── SeatService.java       — кэш карты мест (@Cacheable)
│   ├── SafetyNetScheduler.java — авто-отмена просроченных локов
│   └── dto/                   — LockRequest, LockResponse, SeatInfo
├── event/          # Мероприятия (CRUD)
├── venue/          # Площадки и места
├── admin/          # Админ-панель (создание ивентов, дашборд)
├── user/           # Профиль и история бронирований
├── config/         # Security, Redis Cache, Swagger
└── common/         # Exceptions, GlobalExceptionHandler
```

---

## API Endpoints

| Метод | Endpoint | Доступ | Описание |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Регистрация |
| POST | `/api/auth/login` | Public | Логин |
| POST | `/api/auth/refresh` | Public | Обновить токен |
| GET | `/api/events` | Public | Список мероприятий |
| GET | `/api/events/{id}` | Public | Детали мероприятия |
| GET | `/api/events/{id}/seats` | Public | Карта мест (cached) |
| POST | `/api/bookings/lock` | User | Заблокировать место |
| POST | `/api/bookings/{id}/confirm` | User | Подтвердить бронь |
| DELETE | `/api/bookings/{id}` | User | Отменить блокировку |
| GET | `/api/users/me` | User | Профиль |
| GET | `/api/users/me/bookings` | User | История бронирований |
| POST | `/api/admin/events` | Admin | Создать мероприятие |
| PUT | `/api/admin/events/{id}` | Admin | Редактировать |
| DELETE | `/api/admin/events/{id}` | Admin | Удалить |
| GET | `/api/admin/dashboard` | Admin | Статистика |

---

## Тестирование

| Тип | Инструмент | Покрытие |
|---|---|---|
| Unit | JUnit 5 + Mockito | BookingService (12 кейсов), AdminService |
| Integration | Testcontainers + PG | Race Condition (10 потоков → 1 победитель) |
| API | Postman коллекция | Все эндпоинты |
| CI | GitHub Actions | `mvn verify` на каждый PR |

---

## Роадмап

| Неделя | Фокус | Статус |
|---|---|---|
| 1-2 | Core: Auth, Booking (PG lock), Admin CRUD | ✅ Done |
| 3 | Redis Cache, Unit Tests, CI, Swagger | ✅ Done |
| 4 | Dockerization, Integration Tests, Frontend | 🔄 In Progress |
| 5 | Nginx, Load Testing (JMeter), Polish | ⏳ Planned |
| 6 | Документация, Презентация, Защита | ⏳ Planned |
