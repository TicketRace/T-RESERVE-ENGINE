# T-RESERVE ENGINE 🚀

> Высоконагруженная система бронирования билетов с защитой от Race Condition.

[Figma](https://www.figma.com/design/SjI0zvNK74xYOAYqUwlnl3/%D0%A1%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B0-%D0%B1%D1%80%D0%BE%D0%BD%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F?node-id=0-1&t=zJmoVd0dsGO4Khtd-1)

## 🛠 Технологический стек
Java 21 · Spring Boot 3.3 · PostgreSQL 16 · Redis 7 · RabbitMQ · JWT · Flyway · Docker · GitHub Actions CI

Подробное обоснование выбора технологий → [ARCHITECTURE.md](./ARCHITECTURE.md)

---

## 🚀 Быстрый старт

> **Нужен только Docker Desktop.** Java, Maven и всё остальное — внутри контейнера.

```bash
git clone https://github.com/TicketRace/T-RESERVE-ENGINE.git
cd T-RESERVE-ENGINE
docker compose up
```

Дождитесь строки `Started TReserveApplication` (~30 сек при первом запуске).

Приложение запустится на **http://localhost:8080**

---

## 📖 API Документация (Swagger)

👉 **http://localhost:8080/swagger-ui/index.html**

Все эндпоинты с описанием, кодами ответов и примерами.

---

## 🧪 Быстрая проверка

### 1. Регистрация
```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456","name":"Тест"}'
```
Ответ:
```json
{
  "token": "eyJ...(access, 24 часа)",
  "refreshToken": "eyJ...(refresh, 7 дней)",
  "user": {"id": 3, "email": "test@test.com", "name": "Тест", "role": "USER"}
}
```
### 2. Посмотреть ивенты (без авторизации)
```bash
curl -s http://localhost:8080/api/events
```
### 3. Карта мест (без авторизации)
```bash
curl -s http://localhost:8080/api/events/1/seats
```
### 4. Бронирование (с токеном из шага 1)
```bash
curl -s -X POST http://localhost:8080/api/bookings/lock \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"eventId":1,"seatId":1}'
```
Ответ: 
```
{"lockId":1,"expiresAt":"2026-..."} — место заблокировано на 10 мин.
```
### 5. Race Condition — повтори шаг 4 с тем же местом
```
→ 409 Conflict: место уже занято!
```

Полный набор тестовых команд: [API_ENDPOINTS.md](./API_ENDPOINTS.md)

---

## 🛑 Остановка

```bash
docker compose down       # остановить (данные сохранятся)
docker compose down -v    # остановить + удалить данные
```

---

## 📂 Дополнительная документация
- [Архитектура и роадмап](./ARCHITECTURE.md)
- [Карта эндпоинтов (API)](./API_ENDPOINTS.md)
- [Схема Базы Данных (DB)](./DB.md)
- [Руководство разработчика](./CONTRIBUTING.md)
