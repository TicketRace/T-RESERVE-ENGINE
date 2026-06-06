package com.treserve.support;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@SpringBootTest(properties = {
    "spring.flyway.clean-disabled=false",
    "spring.jpa.show-sql=false",
    "logging.level.com.treserve=INFO",
    "logging.level.org.springframework.security=WARN",
    "management.health.rabbit.enabled=false",
    "management.health.mail.enabled=false",
    "app.safety-net.enabled=false"
})
@ActiveProfiles("it")
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("treserve_it")
        .withUsername("treserve")
        .withPassword("treserve_dev");

    static final org.testcontainers.containers.RabbitMQContainer RABBITMQ = new org.testcontainers.containers.RabbitMQContainer("rabbitmq:3-management").withPluginsEnabled("rabbitmq_stomp").withExposedPorts(5672, 15672, 61613);

    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
        RABBITMQ.start();
    }

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("websocket.rabbitmq.port", () -> RABBITMQ.getMappedPort(61613));
    }

    @Autowired
    protected Flyway flyway;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    
    

    @BeforeEach
    void resetDatabase() {
        flyway.clean();
        flyway.migrate();
        try (var connection = redisConnectionFactory.getConnection()) {
            connection.serverCommands().flushAll();
        }
    }
}




