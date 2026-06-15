# ═══ Stage 1: Build Angular ═══
FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . /app
RUN npm run build

# ═══ Stage 2: Serve static via nginx ═══
FROM nginx:1.25-alpine

COPY --from=builder /app/dist/t-reserve-frontend/browser /usr/share/nginx/html

# nginx.conf использует envsubst — копируем как шаблон
COPY nginx.conf /etc/nginx/templates/nginx.conf.template

# Дефолты для локального docker-compose
ENV PORT=80
ENV BACKEND_URL=http://treserve-nginx

EXPOSE 80

# Подставляем env vars в конфиг nginx и запускаем
CMD ["/bin/sh", "-c", "envsubst '${PORT} ${BACKEND_URL}' < /etc/nginx/templates/nginx.conf.template > /etc/nginx/conf.d/default.conf && nginx -g 'daemon off;'"]
