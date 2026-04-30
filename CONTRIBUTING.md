# Руководство разработчика

## Предусловия
- **Docker Desktop** — для PostgreSQL, Redis, RabbitMQ
- **Java 21 (LTS)** — [Microsoft Build of OpenJDK](https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-21)
- **Maven 3.9+** — или используйте `./mvnw` (если добавлен)

---

## Локальная разработка

### 1. Поднять только инфраструктуру (без приложения)
```powershell
docker compose up postgres redis rabbitmq -d
```

### 2. Запуск приложения (с hot-reload для разработки)
```powershell
mvn spring-boot:run
```
Или через JAR:
```powershell
mvn package -DskipTests -q
java -Xmx256m -jar target/t-reserve-engine-0.1.0-SNAPSHOT.jar
```

Приложение: **http://localhost:8080**
Swagger: **http://localhost:8080/swagger-ui/index.html**

---

## Тестирование

### Unit-тесты
```powershell
mvn test
```

### Конкретный тест-класс
```powershell
mvn test -Dtest=BookingServiceTest
```

### Все тесты + integration
```powershell
mvn verify
```

---

## Git Workflow

```
main       ← стабильный релиз (мержится из develop перед встречами/дедлайнами)
develop    ← основная ветка разработки
feature/*  ← фича-ветки (от develop, PR в develop)
```

### Создать фича-ветку
```powershell
git checkout develop
git pull origin develop
git checkout -b feature/my-feature
```

### Закончить фичу
```powershell
git push -u origin feature/my-feature
# Создать PR в develop на GitHub
```

---

## Тестирование API (PowerShell)

### Регистрация
```powershell
[System.IO.File]::WriteAllText("$pwd\test.json", '{"email":"test@test.com","password":"123456","name":"Тест"}')
curl.exe -s http://localhost:8080/api/auth/register -H "Content-Type: application/json" -d "@test.json"
```

### Сохрани токен
```powershell
$token = "ВСТАВЬ_СЮДА_ЗНАЧЕНИЕ_token"
```

### Посмотреть ивенты
```powershell
curl.exe -s http://localhost:8080/api/events
```

### Карта мест
```powershell
curl.exe -s http://localhost:8080/api/events/1/seats
```

### Забронировать место A-1
```powershell
[System.IO.File]::WriteAllText("$pwd\lock.json", '{"eventId":1,"seatId":1}')
curl.exe -s http://localhost:8080/api/bookings/lock -H "Content-Type: application/json" -H "Authorization: Bearer $token" -d "@lock.json"
```

### Подтвердить бронь
```powershell
curl.exe -s -X POST http://localhost:8080/api/bookings/1/confirm -H "Authorization: Bearer $token"
```

### Отменить бронь
```powershell
curl.exe -s -X DELETE http://localhost:8080/api/bookings/2 -H "Authorization: Bearer $token" -w "\nHTTP: %{http_code}"
```

### Refresh token
```powershell
$refresh = "ВСТАВЬ_REFRESH_TOKEN"
[System.IO.File]::WriteAllText("$pwd\refresh.json", "{`"refreshToken`":`"$refresh`"}")
curl.exe -s http://localhost:8080/api/auth/refresh -H "Content-Type: application/json" -d "@refresh.json"
```

---

## Остановка

```powershell
# Spring Boot: Ctrl+C

# Docker (сохранить данные):
docker compose stop

# Docker (удалить данные):
docker compose down -v
```
