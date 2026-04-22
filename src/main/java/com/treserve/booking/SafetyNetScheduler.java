package com.treserve.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Safety Net — автоматическая отмена просроченных LOCKED билетов.
 *
 * Каждые 30 секунд:
 * 1. SELECT * FROM tickets WHERE status='LOCKED' AND lock_expires_at < NOW()
 * 2. Для каждого: status → AVAILABLE, user → null
 *
 * Partial index idx_tickets_locked_expires делает этот запрос мгновенным
 * даже при миллионах билетов — он сканирует только LOCKED строки.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SafetyNetScheduler {

    private final TicketRepository ticketRepository;

    @Scheduled(fixedRate = 30_000) // каждые 30 секунд
    @Transactional
    public void releaseExpiredLocks() {
        List<Ticket> expired = ticketRepository.findExpiredLocks(Instant.now());

        if (expired.isEmpty()) {
            return;
        }

        for (Ticket ticket : expired) {
            ticket.setStatus(TicketStatus.AVAILABLE);
            ticket.setUser(null);
            ticket.setLockExpiresAt(null);
        }

        ticketRepository.saveAll(expired);

        log.info("SafetyNet: released {} expired locks", expired.size());
    }
}
