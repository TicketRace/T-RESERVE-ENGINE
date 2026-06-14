package com.treserve.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;

import java.time.Instant;
import java.util.List;

/**
 * Safety Net — автоматическая отмена просроченных LOCKED билетов.
 *
 * Каждые 30 секунд:
 * 1. SELECT * FROM tickets WHERE status='LOCKED' AND lock_expires_at < NOW()
 * 2. Для каждого: status → AVAILABLE, user → null
 * 3. Удаляем Redis-ключи (lock:seat:{eventId}:{seatId}) для освобождённых мест
 *
 * Partial index idx_tickets_locked_expires делает этот запрос мгновенным
 * даже при миллионах билетов — он сканирует только LOCKED строки.
 *
 * Redis-очистка важна: если TTL ещё не истёк, а PG уже AVAILABLE —
 * без явного delete() новые пользователи получат Redis-отказ (фантомный лок).
 */
@Component
@ConditionalOnProperty(name = "app.safety-net.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class SafetyNetScheduler {

    private final TicketRepository ticketRepository;
    private final SeatService seatService;
    private final DistributedLockService distributedLock;

    @Scheduled(fixedRate = 30_000) // каждые 30 секунд
    @Transactional
    public void releaseExpiredLocks() {
        List<Ticket> expired = ticketRepository.findExpiredLocks(Instant.now());

        if (expired.isEmpty()) {
            return;
        }

        for (Ticket ticket : expired) {
            ticket.setStatus(TicketStatus.AVAILABLE);
            ticket.setUserId(null);
            ticket.setLockExpiresAt(null);
        }

        ticketRepository.saveAll(expired);

        // Удаляем Redis-ключи для освобождённых мест
        // Предотвращает "фантомные локи" если Redis TTL ещё не истёк
        expired.forEach(t -> distributedLock.release(t.getEventId(), t.getSeatId()));

        // Инвалидировать кэш для каждого затронутого ивента
        expired.stream()
            .map(Ticket::getEventId)
            .distinct()
            .forEach(seatService::evictSeatsCache);

        log.info("SafetyNet: released {} expired locks (PG + Redis)", expired.size());
    }
}
