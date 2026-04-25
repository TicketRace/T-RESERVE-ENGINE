package com.treserve.booking;

import com.treserve.booking.dto.SeatInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис карты мест с Redis кэшем.
 *
 * GET /api/events/{id}/seats вызывается polling'ом каждые 3 сек.
 * При 100 юзерах на одном ивенте = ~33 RPS на один endpoint.
 * Redis кэш (TTL 10 сек) снижает нагрузку на PG в ~3 раза.
 *
 * Cache invalidation: evictSeatsCache() вызывается при lock/confirm/cancel.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SeatService {

    private final TicketRepository ticketRepository;

    /**
     * Получить карту мест для ивента.
     * Кэшируется в Redis с TTL 10 сек (настроено в RedisCacheConfig).
     *
     * Первый запрос → PG query → Redis SET → ответ.
     * Следующие запросы (в течение 10 сек) → Redis GET → ответ (без PG).
     */
    @Cacheable(value = "seats", key = "#eventId")
    public List<SeatInfo> getSeats(Long eventId) {
        log.debug("Cache MISS for seats:{} — querying PG", eventId);
        return ticketRepository.findByEventIdWithSeat(eventId).stream()
            .map(t -> new SeatInfo(
                t.getSeat().getId(),
                t.getSeat().getSeatLabel(),
                t.getSeat().getRowLabel(),
                t.getSeat().getSeatNumber(),
                t.getStatus().name(),
                t.getPrice()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Инвалидировать кэш карты мест.
     * Вызывается после любого изменения статуса билета (lock/confirm/cancel/safety-net).
     */
    @CacheEvict(value = "seats", key = "#eventId")
    public void evictSeatsCache(Long eventId) {
        log.debug("Cache EVICT for seats:{}", eventId);
    }
}
