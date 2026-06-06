# ═══ Stage 1: Build ═══
# Maven 3 + JDK 21
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# 1. Копирование файлов pom.xml для кэширования зависимостей
COPY pom.xml .
COPY treserve-common/pom.xml treserve-common/
COPY treserve-booking/pom.xml treserve-booking/
COPY treserve-app/pom.xml treserve-app/

# Загрузка внешних зависимостей
RUN mvn dependency:go-offline -B -DexcludeGroupIds=com.treserve

# 2. Копирование исходного кода и сборка без тестов
COPY . .
RUN mvn clean package -DskipTests

# 3. Слои Spring Boot для оптимального кэширования Docker
WORKDIR /app/treserve-app/target
RUN java -Djarmode=layertools -jar *-SNAPSHOT.jar extract

# ═══ Stage 2: Run ═══
# Легковесный JRE-образ для продакшена
FROM eclipse-temurin:21-jre
WORKDIR /app

# curl для healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

RUN apt-get update && \
    apt-get install -y --no-install-recommends fontconfig fonts-dejavu && \
    rm -rf /var/lib/apt/lists/*

# Non-root пользователь
RUN groupadd -r treserve && useradd -r -g treserve treserve && \
    chown -R treserve:treserve /app

# Копирование извлеченных слоев Spring Boot по отдельности.
COPY --from=build --chown=treserve:treserve /app/treserve-app/target/dependencies/ ./
COPY --from=build --chown=treserve:treserve /app/treserve-app/target/spring-boot-loader/ ./
COPY --from=build --chown=treserve:treserve /app/treserve-app/target/snapshot-dependencies/ ./
COPY --from=build --chown=treserve:treserve /app/treserve-app/target/application/ ./

# Переключение на безопасного пользователя
USER treserve

EXPOSE 8080

# Запуск через JarLauncher со сбалансированным выделением RAM в % от лимита контейнера
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:MinRAMPercentage=50.0", "org.springframework.boot.loader.launch.JarLauncher"]