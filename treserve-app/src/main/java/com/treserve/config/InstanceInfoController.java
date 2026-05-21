package com.treserve.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Эндпоинт для демонстрации работы load balancer.
 * Показывает какой именно инстанс обработал запрос.
 *
 * Пример ответа:
 * {
 *   "instanceId": "app-1",
 *   "timestamp": "2026-05-18T17:00:00Z"
 * }
 */
@RestController
public class InstanceInfoController {

    @Value("${INSTANCE_ID:standalone}")
    private String instanceId;

    @GetMapping("/api/instance")
    public Map<String, Object> getInstanceInfo() {
        return Map.of(
            "instanceId", instanceId,
            "timestamp", Instant.now().toString()
        );
    }
}
