package com.treserve.booking.service;

import com.treserve.booking.dto.SeatInfo;
import com.treserve.booking.port.EventLookup;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.messaging.simp.SimpMessagingTemplate;

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
    private final EventLookup eventLookup;
    private final SimpMessagingTemplate messagingTemplate;
    /**
     * Получить карту мест для ивента.
     * Кэшируется в Redis с TTL 10 сек (настроено в RedisCacheConfig).
     *
     * Первый запрос → PG query → Redis SET → ответ.
     * Следующие запросы (в течение 10 сек) → Redis GET → ответ (без PG).
     */
    @Cacheable(value = "seats", key = "#eventId")
    public List<SeatInfo> getSeats(Long eventId) {
        // Проверяем существование мероприятия перед получением билетов
        if (!eventLookup.existsById(eventId)) {
            throw new ResourceNotFoundException("Event", eventId);
        }

        log.debug("Cache MISS for seats:{} — querying PG", eventId);
        return ticketRepository.findSeatsByEventId(eventId).stream()
            .map(r -> new SeatInfo(
                r.getSeatId(),
                r.getSeatLabel(),
                r.getRowLabel(),
                r.getSeatNumber(),
                r.getStatus(),
                r.getPrice()
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

    @CacheEvict(value = "seats", key = "#eventId", beforeInvocation = true)
    public void pushSeatsUpdate(Long eventId) {
        List<SeatInfo> seats = getSeats(eventId);

        messagingTemplate.convertAndSend(
            "/topic/seats/" + eventId,
            seats
        );

        log.debug("WS push seats update for event {}", eventId);
    }
}
