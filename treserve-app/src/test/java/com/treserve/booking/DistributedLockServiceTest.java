package com.treserve.booking;

import com.treserve.booking.service.DistributedLockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для DistributedLockService.
 * Проверяем чистую логику Redis без Spring Context.
 */
@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTest {

    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> valueOps;

    @InjectMocks
    private DistributedLockService distributedLockService;

    private static final Long EVENT_ID = 1L;
    private static final Long SEAT_ID = 42L;
    private static final Long USER_ID = 99L;
    private static final String KEY = "lock:seat:1:42";
    private static final Duration TTL = Duration.ofMinutes(10);

    @BeforeEach
    void setUp() {
        // Ленточная настройка: по умолчанию если вызовут opsForValue, возвращаем мок.
        lenient().when(redis.opsForValue()).thenReturn(valueOps);
    }

    // ─── tryAcquire ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("tryAcquire: ключ установлен (success) → возвращает true")
    void tryAcquire_success() {
        when(valueOps.setIfAbsent(eq(KEY), eq(USER_ID.toString()), eq(TTL))).thenReturn(true);

        boolean result = distributedLockService.tryAcquire(EVENT_ID, SEAT_ID, USER_ID, TTL);

        assertThat(result).isTrue();
        verify(valueOps).setIfAbsent(KEY, USER_ID.toString(), TTL);
    }

    @Test
    @DisplayName("tryAcquire: ключ уже занят (alreadyLocked) → возвращает false")
    void tryAcquire_alreadyLocked() {
        when(valueOps.setIfAbsent(eq(KEY), eq(USER_ID.toString()), eq(TTL))).thenReturn(false);

        boolean result = distributedLockService.tryAcquire(EVENT_ID, SEAT_ID, USER_ID, TTL);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("tryAcquire: Redis вернул null → возвращает false")
    void tryAcquire_redisReturnsNull() {
        when(valueOps.setIfAbsent(eq(KEY), eq(USER_ID.toString()), eq(TTL))).thenReturn(null);

        boolean result = distributedLockService.tryAcquire(EVENT_ID, SEAT_ID, USER_ID, TTL);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("tryAcquire: Redis упал (exception) → graceful degradation (true)")
    void tryAcquire_redisDown() {
        when(valueOps.setIfAbsent(eq(KEY), eq(USER_ID.toString()), eq(TTL)))
                .thenThrow(new RuntimeException("Redis connection refused"));

        boolean result = distributedLockService.tryAcquire(EVENT_ID, SEAT_ID, USER_ID, TTL);

        // Должны пропустить на уровень PG
        assertThat(result).isTrue();
    }

    // ─── release ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("release: успешное удаление ключа")
    void release_success() {
        when(redis.delete(KEY)).thenReturn(true);

        distributedLockService.release(EVENT_ID, SEAT_ID);

        verify(redis).delete(KEY);
    }

    @Test
    @DisplayName("release: Redis упал (exception) → exception проглатывается (graceful degradation)")
    void release_redisDown() {
        when(redis.delete(KEY)).thenThrow(new RuntimeException("Redis connection refused"));

        // Не должно выбрасывать исключение наружу
        assertDoesNotThrow(() -> distributedLockService.release(EVENT_ID, SEAT_ID));

        verify(redis).delete(KEY);
    }

    // ─── buildKey ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("buildKey: формирует правильный ключ")
    void buildKey_correctFormat() {
        String key = distributedLockService.buildKey(EVENT_ID, SEAT_ID);
        assertThat(key).isEqualTo("lock:seat:1:42");
    }
}
