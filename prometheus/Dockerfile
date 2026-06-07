# Custom Prometheus image with baked-in config for Railway deployment
FROM prom/prometheus:v2.51.0

COPY prometheus.railway.yml /etc/prometheus/prometheus.yml

EXPOSE 9090
