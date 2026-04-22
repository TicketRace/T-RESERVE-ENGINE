package com.treserve.booking;

import com.treserve.booking.dto.LockResponse;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.common.exception.SeatAlreadyLockedException;
import com.treserve.user.User;
import com.treserve.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Value("${app.booking.lock-duration-minutes:10}")
    private int lockDurationMinutes;

    /**
     * ЯДРО: Попытка заблокировать место.
     *
     * Flow:
     * 1. BEGIN TRANSACTION (Spring @Transactional)
     * 2. SELECT * FROM tickets WHERE ... FOR UPDATE NOWAIT
     *    - Строка свободна → получаем её, PG блокирует строку
     *    - Строка уже заблокирована другой транзакцией → PessimisticLockingFailureException
     *    - Status != AVAILABLE → пустой Optional
     * 3. UPDATE status='LOCKED', user_id, lock_expires_at
     * 4. COMMIT → строка разблокируется
     *
     * При 1000 конкурентных запросах на одно место:
     * - 1 получает 200 OK + lockId
     * - 999 получают 409 Conflict (мгновенно, без ожидания)
     */
    @Transactional
    public LockResponse tryLock(Long eventId, Long seatId, Long userId) {
        try {
            // SELECT FOR UPDATE NOWAIT — атомарно захватываем строку
            Ticket ticket = ticketRepository.findAvailableForUpdate(eventId, seatId)
                .orElseThrow(() -> new SeatAlreadyLockedException(
                    "Seat " + seatId + " for event " + eventId));

            // Нашли AVAILABLE билет — блокируем
            User user = userRepository.getReferenceById(userId);
            Instant expiresAt = Instant.now().plus(lockDurationMinutes, ChronoUnit.MINUTES);

            ticket.setStatus(TicketStatus.LOCKED);
            ticket.setUser(user);
            ticket.setLockExpiresAt(expiresAt);

            ticketRepository.save(ticket);

            log.info("LOCKED seat {} for event {} by user {} (expires {})",
                seatId, eventId, userId, expiresAt);

            return new LockResponse(ticket.getId(), expiresAt);

        } catch (PessimisticLockingFailureException e) {
            // FOR UPDATE NOWAIT → строка уже заблокирована другой транзакцией
            log.debug("Lock contention on seat {} event {} — already being processed", seatId, eventId);
            throw new SeatAlreadyLockedException("Seat " + seatId + " for event " + eventId);
        }
    }

    /**
     * Подтвердить бронирование (оплата mock).
     * LOCKED → BOOKED
     */
    @Transactional
    public void confirm(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        // Проверки
        if (ticket.getUser() == null || !ticket.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("This ticket is not locked by you");
        }
        if (ticket.getStatus() != TicketStatus.LOCKED) {
            throw new IllegalArgumentException("Ticket is not in LOCKED state (current: " + ticket.getStatus() + ")");
        }
        if (ticket.getLockExpiresAt().isBefore(Instant.now())) {
            // Лок истёк — место уже могло быть перехвачено
            throw new IllegalArgumentException("Lock expired, please try again");
        }

        // LOCKED → BOOKED
        ticket.setStatus(TicketStatus.BOOKED);
        ticket.setBookedAt(Instant.now());
        ticket.setLockExpiresAt(null); // больше не нужен таймер

        ticketRepository.save(ticket);

        log.info("BOOKED ticket {} for user {}", ticketId, userId);
    }

    /**
     * Ручная отмена блокировки.
     * LOCKED → AVAILABLE
     */
    @Transactional
    public void cancel(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        if (ticket.getUser() == null || !ticket.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("This ticket is not locked by you");
        }
        if (ticket.getStatus() != TicketStatus.LOCKED) {
            throw new IllegalArgumentException("Can only cancel LOCKED tickets (current: " + ticket.getStatus() + ")");
        }

        // LOCKED → AVAILABLE
        ticket.setStatus(TicketStatus.AVAILABLE);
        ticket.setUser(null);
        ticket.setLockExpiresAt(null);

        ticketRepository.save(ticket);

        log.info("CANCELLED lock on ticket {} by user {}", ticketId, userId);
    }
}
