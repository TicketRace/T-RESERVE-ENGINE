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

E2E-тесты находятся в `frontend/e2e` и запускаются через Playwright. Тесты поднимают Angular dev-server автоматически и мокают backend API, поэтому для проверки пользовательских сценариев не нужно запускать Spring Boot, PostgreSQL или Redis.

```bash
npm ci
npx playwright install chromium
npm run e2e
```

Полезные команды:

```bash
npm run e2e:headed
npm run e2e:ui
npm run e2e:report
```
