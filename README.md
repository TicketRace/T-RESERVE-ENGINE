# T-RESERVE ENGINE 🚀

Высоконагруженная система бронирования билетов с защитой от Race Condition.

[Figma](https://www.figma.com/design/SjI0zvNK74xYOAYqUwlnl3/%D0%A1%D0%B8%D1%81%D1%82%D0%B5%D0%BC%D0%B0-%D0%B1%D1%80%D0%BE%D0%BD%D0%B8%D1%80%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D1%8F?node-id=0-1&t=zJmoVd0dsGO4Khtd-1)

## 🛠 Технологический стек
- **Backend:** Java 21 (Virtual Threads), Spring Boot 3.3
- **Database:** PostgreSQL 16 (Pessimistic Locking `FOR UPDATE NOWAIT`)
- **Cache & Locks:** Redis 7
- **Message Broker:** RabbitMQ 3.13 (зарезервировано для v2)
- **Background Tasks:** Spring `@Scheduled` (SafetyNet для отмены броней каждые 30 сек)
- **Security:** Stateless JWT (Access + Refresh tokens)
- **Database Migrations:** Flyway

---

## 🚀 Быстрый старт

### 1. Предусловия
- Установите и запустите **Docker Desktop**
- Установите **Java 21 (LTS)**
- Склонируйте репозиторий и откройте терминал в корневой папке.

### 2. Поднятие инфраструктуры (БД, Redis, RabbitMQ)
```powershell
docker compose up -d
```
Проверьте статус через `docker ps` — все контейнеры должны быть `healthy`.

### 3. Сборка и запуск приложения
```powershell
# Сборка JAR
mvn package -DskipTests -q

# Запуск
java -Xmx256m -jar target/t-reserve-engine-0.1.0-SNAPSHOT.jar
```
Приложение запустится на порту **8080**.

---

## 📖 API Документация (Swagger)

После запуска перейдите по адресу:
👉 **http://localhost:8080/swagger-ui/index.html**

Здесь можно протестировать все эндпоинты (регистрация, логин, бронирование) прямо из браузера.

---

## 🧪 Тестирование (curl / PowerShell)

Полный список команд для тестирования регистрации, логина и всего цикла бронирования (lock -> confirm -> cancel) находится в файле:
[API_ENDPOINTS.md](./API_ENDPOINTS.md)

### Пример быстрой проверки бронирования:
1. Зарегистрируйся (`POST /api/auth/register`)
2. Получи Access Token из ответа.
3. Посмотри карту мест: `GET /api/events/1/seats`
4. Залочь место:
```powershell
curl.exe -X POST http://localhost:8080/api/bookings/lock `
  -H "Content-Type: application/json" `
  -H "Authorization: Bearer YOUR_TOKEN" `
  -d '{"eventId":1,"seatId":1}'
```

### 1. Регистрация
```powershell
# Записываем JSON в файл (PowerShell плохо дружит с кавычками в curl)
[System.IO.File]::WriteAllText("$pwd\test.json", '{"email":"test@test.com","password":"123456","name":"Тест"}')

curl.exe -s http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "@test.json"
```

Ответ:
```json
{
  "token": "eyJ...(access, 24 часа)",
  "refreshToken": "eyJ...(refresh, 7 дней)",
  "user": {"id": 3, "email": "test@test.com", "name": "Тест", "role": "USER"}
}
```

### 2. Сохрани токен в переменную
```powershell
$token = "ВСТАВЬ_СЮДА_ЗНАЧЕНИЕ_token_ИЗ_ОТВЕТА"
```

### 3. Посмотреть ивенты (без авторизации)
```powershell
curl.exe -s http://localhost:8080/api/events
```

### 4. Посмотреть карту мест
```powershell
curl.exe -s http://localhost:8080/api/events/1/seats
```

### 5. Забронировать место A-1
```powershell
[System.IO.File]::WriteAllText("$pwd\lock.json", '{"eventId":1,"seatId":1}')
curl.exe -s http://localhost:8080/api/bookings/lock -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d "@lock.json"
```

Ответ: `{"lockId":1,"expiresAt":"2026-..."}` — место заблокировано на 10 мин.

### 6. Подтвердить бронь (mock оплата)
```powershell
# lockId из предыдущего ответа
curl.exe -s -X POST http://localhost:8080/api/bookings/1/confirm -H "Authorization: Bearer $token"
```

Ответ: `Booking confirmed`

### 7. Забронировать и ОТМЕНИТЬ
```powershell
# Бронируем A-2
[System.IO.File]::WriteAllText("$pwd\lock.json", '{"eventId":1,"seatId":2}')
curl.exe -s http://localhost:8080/api/bookings/lock -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d "@lock.json"

# Отменяем (lockId из ответа)
curl.exe -s -X DELETE http://localhost:8080/api/bookings/2 -H "Authorization: Bearer $token" -w "\nHTTP: %{http_code}"
```

Ответ: `HTTP: 204` (успешная отмена)

### 8. Попробовать занять уже занятое место
```powershell
[System.IO.File]::WriteAllText("$pwd\lock.json", '{"eventId":1,"seatId":1}')
curl.exe -s http://localhost:8080/api/bookings/lock -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d "@lock.json"
```

Ответ: `409 Conflict` — место уже забронировано!

### 9. Обновить access token (refresh)
```powershell
$refresh = "ВСТАВЬ_СЮДА_ЗНАЧЕНИЕ_refreshToken_ИЗ_РЕГИСТРАЦИИ"
[System.IO.File]::WriteAllText("$pwd\refresh.json", "{`"refreshToken`":`"$refresh`"}")
curl.exe -s http://localhost:8080/api/auth/refresh -H "Content-Type: application/json" -d "@refresh.json"
```

---

## Остановить всё

```powershell
# Остановить Spring Boot: Ctrl+C в терминале где он запущен

# Остановить Docker (сохранить данные):
docker compose stop

# Остановить Docker (удалить данные):
docker compose down -v
```

---

## 📂 Дополнительная документация
- [Карта эндпоинтов (API)](./API_ENDPOINTS.md)
- [Схема Базы Данных (DB)](./DB.md)
