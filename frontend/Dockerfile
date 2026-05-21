#build angular
FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./

RUN npm ci

COPY . /app

RUN npm run build

#serve static via nginx
FROM nginx:1.25-alpine

COPY --from=builder /app/dist/t-reserve-frontend/browser /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80