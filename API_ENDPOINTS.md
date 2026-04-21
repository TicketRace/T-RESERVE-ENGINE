# T-RESERVE ENGINE — API Endpoint Map (MVP)

> Все эндпоинты доступны в Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Auth

| Method | Path | Auth | Body | Response |
|---|---|---|---|---|
| `POST` | `/api/auth/register` | ❌ | `{"email","password","name"}` | `{token, refreshToken, user}` |
| `POST` | `/api/auth/login` | ❌ | `{"email","password"}` | `{token, refreshToken, user}` |
| `POST` | `/api/auth/refresh` | ❌ | `{"refreshToken"}` | `{token, refreshToken, user}` |

## Events (публичные, без авторизации)

| Method | Path | Auth | Response |
|---|---|---|---|
| `GET` | `/api/events` | ❌ | Пагинированный список ивентов |
| `GET` | `/api/events/{id}` | ❌ | Детали ивента + venue |
| `GET` | `/api/events/{id}/seats` | ❌ | Карта мест: `[{seatId, seatLabel, rowLabel, seatNumber, status, price}]` |

## Booking (нужен JWT)

| Method | Path | Auth | Body | Response | HTTP Code |
|---|---|---|---|---|---|
| `POST` | `/api/bookings/lock` | 🔒 Bearer | `{"eventId","seatId"}` | `{lockId, expiresAt}` | 200 / 409 |
| `POST` | `/api/bookings/{id}/confirm` | 🔒 Bearer | — | `"Booking confirmed"` | 200 |
| `DELETE` | `/api/bookings/{id}` | 🔒 Bearer | — | — | 204 |


### Пример полного flow (curl)
```bash
# 1. Register
POST /api/auth/register → получаем token

# 2. Lock
POST /api/bookings/lock + Bearer token
Body: {"eventId": 1, "seatId": 1}
→ {"lockId": 1, "expiresAt": "2026-04-22T..."}

# 3. Confirm
POST /api/bookings/1/confirm + Bearer token
→ "Booking confirmed"

# 4. Или Cancel
DELETE /api/bookings/1 + Bearer token
→ 204 No Content
```

## Error Responses

| HTTP Code | Когда | Body |
|---|---|---|
| 400 | Невалидный запрос | `{"error": "...", "status": 400}` |
| 401 | Нет/невалидный JWT | `{"error": "Unauthorized", "status": 401}` |
| 404 | Ресурс не найден | `{"error": "Ticket not found: 999", "status": 404}` |
| 409 | Место уже занято | `{"error": "Seat already locked: Seat 1 for event 1", "status": 409}` |

## System

| Method | Path | Описание |
|---|---|---|
| `GET` | `/swagger-ui/index.html` | Swagger UI |
| `GET` | `/actuator/health` | `{"status":"UP"}` |
