# T-Reserve Frontend

Frontend часть системы бронирования билетов.

---

## Технологии

- Angular
- TypeScript
- RxJS
- CSS
- Mock API

---

## Установка, запуск на http://localhost:4200

```bash
npm install
ng serve
```

## E2E-тесты

E2E-тесты находятся в `frontend/e2e` и запускаются через Playwright. В проекте есть два набора:

- `npm run e2e` / `npm run e2e:mocked` — быстрые browser-тесты с моками backend API. Они проверяют UI-сценарии фронтенда без запуска Spring Boot, PostgreSQL, Redis и RabbitMQ.
- `npm run e2e:real` — полные full-stack E2E без моков. Они работают через настоящий Angular dev-server, Spring Boot backend, PostgreSQL, Redis, RabbitMQ и реальные API.

Установка зависимостей и браузера:

```bash
npm ci
npx playwright install chromium
```

Быстрый mocked-набор:

```bash
npm run e2e
```

Полный real-набор нужно запускать при поднятом backend API на `http://localhost:8080`. Самый простой вариант из корня репозитория:

```bash
docker compose up -d postgres redis rabbitmq
mvn -pl treserve-app -am spring-boot:run
```

Во втором терминале из `frontend`:

```bash
npm run e2e:real
```

Real E2E используют seed-аккаунты Flyway:

```text
admin@treserve.com / admin123
```

Пользовательские аккаунты и тестовые мероприятия создаются тестами автоматически через реальные `/api/auth/register` и `/api/admin/events`. Для повторяемости названия и email генерируются уникальными.

Полезные команды:

```bash
npm run e2e:headed
npm run e2e:ui
npm run e2e:real:headed
npm run e2e:real:ui
npm run e2e:all
npm run e2e:report
```

Если backend запущен не на стандартном адресе, передайте URL API:

```bash
E2E_API_URL=http://localhost:8081 npm run e2e:real
```

Если Angular dev-server уже запущен вручную, можно не поднимать его из Playwright:

```bash
E2E_SKIP_FRONTEND_SERVER=1 npm run e2e:real
```
