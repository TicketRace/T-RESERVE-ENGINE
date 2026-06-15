package com.treserve.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Уровень 1 распределённой блокировки — Redis SETNX.
 *
 * Назначение:
 *   Атомарно отбивать "проигравших" конкурентов ещё до обращения к PostgreSQL.
 *   При 1000 конкурентных запросах на одно место — 999 получают 409 за ~2мс (из Redis),
 *   только 1 победитель доходит до FOR UPDATE NOWAIT в PostgreSQL.
 *
 * Graceful degradation:
 *   Если Redis недоступен — методы не бросают исключений, а логируют WARN
 *   и возвращают безопасные значения (true/void), позволяя системе
 *   упасть на уровень PostgreSQL-only без сбоя всего сервиса.
 *
 * Формат ключа: "lock:seat:{eventId}:{seatId}"
 * TTL         : lockDurationMinutes * 60 + 30 сек (буфер, чтобы SafetyNet
 *               успел почистить PG раньше, чем Redis-ключ истечёт сам).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DistributedLockService {

    private static final String PREFIX = "lock:seat:";

    private final StringRedisTemplate redis;

    /**
     * Попытка атомарно выставить Redis-лок (SET key value NX EX ttl).
     *
     * @param eventId  ID мероприятия
     * @param seatId   ID места
     * @param userId   ID пользователя (сохраняется как value для отладки)
     * @param ttl      Время жизни ключа
     * @return true  — лок получен, можно идти в PostgreSQL
     *         false — место уже заблокировано другим пользователем (409!)
     *         true  — если Redis недоступен (graceful degradation, PG решит)
     */
    public boolean tryAcquire(Long eventId, Long seatId, Long userId, Duration ttl) {
        String key = buildKey(eventId, seatId);
        try {
            Boolean acquired = redis.opsForValue()
                    .setIfAbsent(key, userId.toString(), ttl);
            boolean result = Boolean.TRUE.equals(acquired);
            if (result) {
                log.debug("Redis lock ACQUIRED: key={}, userId={}", key, userId);
            } else {
                log.debug("Redis lock REJECTED (already locked): key={}", key);
            }
            return result;
        } catch (Exception e) {
            // Redis недоступен — не падаем, пропускаем на уровень PostgreSQL
            log.warn("Redis unavailable in tryAcquire, falling through to PG. key={}", key, e);
            return true;
        }
    }

    /**
     * Освобождение Redis-лока.
     * Вызывается при confirm(), cancel() и SafetyNetScheduler.
     *
     * Fail-safe: если Redis недоступен — логируем WARN.
     * Место будет разблокировано автоматически по истечении TTL.
     *
     * Трейд-офф: если cancel() записал AVAILABLE в PG,
     * но Redis временно недоступен — место зависнет на остаток TTL.
     * Это допустимый бизнес-риск при Redis-аутаже (документировано).
     */
    public void release(Long eventId, Long seatId) {
        String key = buildKey(eventId, seatId);
        try {
            Boolean deleted = redis.delete(key);
            log.debug("Redis lock RELEASED: key={}, deleted={}", key, deleted);
        } catch (Exception e) {
            log.warn("Redis unavailable in release, TTL will clean up. key={}", key, e);
        }
    }

    /**
     * Формирует ключ: "lock:seat:{eventId}:{seatId}"
     */
    public String buildKey(Long eventId, Long seatId) {
        return PREFIX + eventId + ":" + seatId;
    }
}
