package com.treserve.booking.service;

import com.treserve.booking.dto.LockResponse;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.port.UserLookup;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ForbiddenOperationException;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.common.exception.SeatAlreadyLockedException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.transaction.support.TransactionTemplate;

import com.treserve.booking.pdf.PdfGenerator;
import com.treserve.booking.notification.EmailSender;
import com.treserve.booking.event.EventTitleProvider;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final TicketRepository ticketRepository;
    /** Port interface — implemented in treserve-app by JpaUserLookup. */
    private final UserLookup userLookup;
    private final SeatService seatService;
    private final TransactionTemplate transactionTemplate;

    private final PdfGenerator pdfGenerator;
    private final EmailSender emailSender;
    private final EventTitleProvider eventTitleProvider;

    @Value("${app.booking.lock-duration-minutes:10}")
    private int lockDurationMinutes;

    /**
     * ЯДРО: Попытка заблокировать место.
     *
     * Flow:
     * 1. Проверяем что user существует через port interface (ВНЕ транзакции БД)
     * 2. Выполняем транзакцию с пессимистической блокировкой БД (TransactionTemplate)
     * 3. SELECT * FROM tickets WHERE ... FOR UPDATE NOWAIT
     *    - Строка свободна → получаем её, PG блокирует строку
     *    - Строка уже заблокирована другой транзакцией → PessimisticLockingFailureException
     * 4. UPDATE status='LOCKED', user_id, lock_expires_at
     * 5. COMMIT → строка разблокируется
     *
     * При 1000 конкурентных запросах на одно место:
     * - 1 получает 200 OK + lockId
     * - 999 получают 409 Conflict (мгновенно, без ожидания)
     */
    public LockResponse tryLock(Long eventId, Long seatId, Long userId) {
        // Проверяем существование пользователя ВНЕ транзакции базы данных
        if (!userLookup.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }

        try {
            return transactionTemplate.execute(status -> {
                // SELECT FOR UPDATE NOWAIT — атомарно захватываем строку
                Ticket ticket = ticketRepository.findAvailableForUpdate(eventId, seatId, Instant.now())
                    .orElseThrow(() -> new SeatAlreadyLockedException(
                        "Seat " + seatId + " for event " + eventId));

                Instant expiresAt = Instant.now().plus(lockDurationMinutes, ChronoUnit.MINUTES);

                ticket.setStatus(TicketStatus.LOCKED);
                ticket.setUserId(userId);
                ticket.setLockExpiresAt(expiresAt);

                ticketRepository.save(ticket);

                log.info("LOCKED seat {} for event {} by user {} (expires {})",
                    seatId, eventId, userId, expiresAt);

                seatService.evictSeatsCache(eventId);
                return new LockResponse(ticket.getId(), expiresAt);
            });
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
        try {
            Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

            if (ticket.getUserId() == null || !ticket.getUserId().equals(userId)) {
                throw new ForbiddenOperationException("This ticket is not locked by you");
            }
            if (ticket.getStatus() != TicketStatus.LOCKED) {
                throw new IllegalArgumentException("Ticket is not in LOCKED state (current: " + ticket.getStatus() + ")");
            }
            if (ticket.getLockExpiresAt().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Lock expired, please try again");
            }

            ticket.setStatus(TicketStatus.BOOKED);
            ticket.setBookedAt(Instant.now());
            ticket.setLockExpiresAt(null);
            
            if (ticket.getVerifyToken() == null) {
                ticket.setVerifyToken(UUID.randomUUID());
            }

            ticketRepository.save(ticket);

            seatService.evictSeatsCache(ticket.getEventId());
            log.info("BOOKED ticket {} for user {}", ticketId, userId);

            sendTicketEmail(ticket, userId);

        } catch (PessimisticLockingFailureException e) {
            log.debug("Conflict on confirm ticket {} — seat is currently being processed", ticketId);
            throw new SeatAlreadyLockedException("Ticket is currently locked by another process, please try again");
        }
    }

    private void sendTicketEmail(Ticket ticket, Long userId) {
        log.info("=== USING UPDATED VERSION WITH PDF DISABLED ===");
        try {
            // Получаем пользователя через порт UserLookup
            var userInfo = userLookup.findById(userId);
            if (userInfo == null) {
                log.warn("User {} not found, cannot send email for ticket {}", userId, ticket.getId());
                return;
            }

            // Получаем название мероприятия через интерфейс EventTitleProvider
            String eventTitle = eventTitleProvider.getEventTitle(ticket.getEventId());
            
            // Отправляем email 
            byte[] pdfBytes = pdfGenerator.generatePdf(ticket);
            emailSender.sendTicketEmail(userInfo.email(), userInfo.name(), pdfBytes, ticket.getId(), eventTitle);
            
            log.info("Ticket email sent to {} for ticket {}", userInfo.email(), ticket.getId());
        } catch (Exception e) {
            // Не бросаем исключение — бронирование уже подтверждено
            log.error("Failed to send email for ticket {}: {}", ticket.getId(), e.getMessage(), e);
        }
    }


    /**
     * Ручная отмена блокировки.
     * LOCKED → AVAILABLE
     */
    @Transactional
    public void cancel(Long ticketId, Long userId) {
        try {
            Ticket ticket = ticketRepository.findByIdForUpdate(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

            if (ticket.getUserId() == null || !ticket.getUserId().equals(userId)) {
                throw new ForbiddenOperationException("This ticket is not locked by you");
            }
            if (ticket.getStatus() != TicketStatus.LOCKED) {
                throw new IllegalArgumentException("Can only cancel LOCKED tickets (current: " + ticket.getStatus() + ")");
            }

            ticket.setStatus(TicketStatus.AVAILABLE);
            ticket.setUserId(null);
            ticket.setLockExpiresAt(null);

            ticketRepository.save(ticket);

            seatService.evictSeatsCache(ticket.getEventId());
            log.info("CANCELLED lock on ticket {} by user {}", ticketId, userId);
        } catch (PessimisticLockingFailureException e) {
            log.debug("Conflict on cancel ticket {} — seat is currently being processed", ticketId);
            throw new SeatAlreadyLockedException("Ticket is currently locked by another process, please try again");
        }
    }

    /**
     * Проверяет, есть ли у мероприятия оплаченные билеты.
     * Нужно для защиты от удаления ивента с проданными билетами.
     * Использует один COUNT запрос вместо загрузки всех билетов в память.
     */
    @Transactional(readOnly = true)
    public boolean hasBookedTickets(Long eventId) {
        return ticketRepository.existsByEventIdAndStatus(eventId, TicketStatus.BOOKED);
    }
}