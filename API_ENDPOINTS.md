# T-RESERVE ENGINE — API Endpoint Map

> Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Auth — `/api/auth`

| Method | Path | Auth | Описание | Body | Response | Status |
|---|---|---|---|---|---|---|
| `POST` | `/api/auth/register` | ❌ | Регистрация нового пользователя. Создаёт аккаунт, хэширует пароль BCrypt, возвращает пару JWT токенов | `{"email","password","name"}` | `{token, refreshToken, user}` | ✅ |
| `POST` | `/api/auth/login` | ❌ | Вход по email/password. Проверяет хэш, выдаёт пару JWT | `{"email","password"}` | `{token, refreshToken, user}` | ✅ |
| `POST` | `/api/auth/refresh` | ❌ | Обновление access токена. Принимает refresh (7 дней) → выдаёт новую пару. Проверяет claim type="refresh" | `{"refreshToken"}` | `{token, refreshToken, user}` | ✅ |

## Events — `/api/events` (публичные, без авторизации)

| Method | Path | Auth | Описание | Response | Status |
|---|---|---|---|---|---|
| `GET` | `/api/events` | ❌ | Каталог мероприятий. Пагинация (page, size). Включает venue, дату, цену | Пагинированный список | ✅ |
| `GET` | `/api/events/{id}` | ❌ | Детали конкретного мероприятия с информацией о площадке | Event + Venue | ✅ |
| `GET` | `/api/events/{id}/seats` | ❌ | Карта мест для ивента. Фронт дёргает каждые 3 сек (polling) для обновления статусов | `[{seatId, seatLabel, rowLabel, seatNumber, status, price}]` | ✅ |

## Booking — `/api/bookings`

| Method | Path | Auth | Описание | Body | Response | Status |
|---|---|---|---|---|---|---|
| `POST` | `/api/bookings/lock` | 🔒 USER | Заблокировать место на 10 минут. Использует PG `SELECT FOR UPDATE NOWAIT`. При конкурентном доступе — мгновенный 409 | `{"eventId","seatId"}` | `{lockId, expiresAt}` / 409 | ✅ |
| `POST` | `/api/bookings/{id}/confirm` | 🔒 USER | Подтвердить бронирование (mock оплата). LOCKED → BOOKED. Проверяет: твой ли лок, не истёк ли таймер | — | `"Booking confirmed"` | ✅ |
| `DELETE` | `/api/bookings/{id}` | 🔒 USER | Ручная отмена блокировки до истечения 10 мин. LOCKED → AVAILABLE. Место снова доступно для других | — | 204 No Content | ✅ |

## User Profile — `/api/users/me`

| Method | Path | Auth | Описание | Response | Status |
|---|---|---|---|---|---|
| `GET` | `/api/users/me` | 🔒 USER | Профиль текущего пользователя | `{id, email, name, role}` | TODO |
| `GET` | `/api/users/me/bookings` | 🔒 USER | История бронирований пользователя (LOCKED + BOOKED) | `[{ticketId, event, seat, status, bookedAt}]` | TODO |

## Admin — `/api/admin`

| Method | Path | Auth | Описание | Body | Response | Status |
|---|---|---|---|---|---|---|
| `GET` | `/api/venues` | 🔒 ADMIN | Список всех площадок с количеством мест | Venues list | ✅ |
| `POST` | `/api/admin/events` | 🔒 ADMIN | Создание мероприятия. Привязка к venue, генерация билетов | `{title, description, venueId, startsAt, price}` | Created event | TODO |
| `PUT` | `/api/admin/events/{id}` | 🔒 ADMIN | Редактирование мероприятия (до начала продаж) | `{title, ...}` | Updated event | TODO |
| `DELETE` | `/api/admin/events/{id}` | 🔒 ADMIN | Удаление мероприятия (каскадно удаляет билеты) | — | 204 | TODO |
| `GET` | `/api/admin/dashboard` | 🔒 ADMIN | Дашборд: общая статистика продаж, кол-во ивентов, выручка | — | `{totalEvents, totalBookings, revenue}` | TODO |

## System

| Method | Path | Описание |
|---|---|---|
| `GET` | `/swagger-ui/index.html` | Интерактивная API документация (SpringDoc OpenAPI) |
| `GET` | `/actuator/health` | Health check для мониторинга: `{"status":"UP"}` |

---

## Автоматические процессы (не API)

| Процесс | Частота | Описание |
|---|---|---|
| **SafetyNet** | каждые 30 сек | `SELECT WHERE status='LOCKED' AND lock_expires_at < NOW()` → автоотмена просроченных блокировок |

---

## Error Responses

| HTTP | Когда | Body |
|---|---|---|
| 400 | Невалидный запрос (пустой email, короткий пароль) | `{"error": "...", "status": 400}` |
| 401 | Нет JWT / истёк / невалидный | `{"error": "Unauthorized", "status": 401}` |
| 404 | Ресурс не найден (event, ticket) | `{"error": "Ticket not found: 999", "status": 404}` |
| 409 | Место уже заблокировано/забронировано | `{"error": "Seat already locked", "status": 409}` |

---

## Пример полного flow
```bash
# 1. Register
POST /api/auth/register
Body: {"email":"user@test.com", "password":"123456", "name":"Иван"}
→ {token: "eyJ...", refreshToken: "eyJ...", user: {id: 3, role: "USER"}}

# 2. Browse events (no auth needed)
GET /api/events
→ [{id: 1, title: "Рок-фестиваль", startsAt: "2025-07-15T19:00"}]

# 3. View seat map
GET /api/events/1/seats
→ [{seatId: 1, seatLabel: "A-1", status: "AVAILABLE", price: 500}]

# 4. Lock seat
POST /api/bookings/lock + Bearer token
Body: {"eventId": 1, "seatId": 1}
→ {"lockId": 1, "expiresAt": "2026-04-22T..."}

# 5. Confirm booking
POST /api/bookings/1/confirm + Bearer token
→ "Booking confirmed"

# 6. View my bookings
GET /api/users/me/bookings + Bearer token
→ [{ticketId: 1, event: "Рок-фестиваль", seat: "A-1", status: "BOOKED"}]
```
