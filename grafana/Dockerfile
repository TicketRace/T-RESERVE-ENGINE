# Custom Grafana image with baked-in provisioning for Railway deployment
FROM grafana/grafana:10.4.2

# Railway-specific datasource (internal Prometheus hostname)
COPY provisioning-railway/datasources/ /etc/grafana/provisioning/datasources/
# Dashboard config и JSON — одинаковые для Railway и локали
COPY provisioning/dashboards/ /etc/grafana/provisioning/dashboards/

EXPOSE 3000
