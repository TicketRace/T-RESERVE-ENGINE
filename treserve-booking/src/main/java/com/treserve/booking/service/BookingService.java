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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final TicketRepository ticketRepository;
    /** Port interface — implemented in treserve-app by JpaUserLookup. */
    private final UserLookup userLookup;
    private final SeatService seatService;
    private final TransactionTemplate transactionTemplate;
    /** Уровень 1: Redis SETNX — быстрый атомарный фильтр перед PG. */
    private final DistributedLockService distributedLock;

    @Value("${app.booking.lock-duration-minutes:10}")
    private int lockDurationMinutes;

    /**
     * ЯДРО: Попытка заблокировать место. Двухуровневая защита:
     *
     * Уровень 1 — Redis SETNX (fast filter, ~2ms):
     *   При 1000 конкурентных запросах — 999 получают 409 здесь, не доходя до PG.
     *   Если Redis недоступен — graceful degradation, переходим сразу на уровень 2.
     *
     * Уровень 2 — PostgreSQL FOR UPDATE NOWAIT (source of truth):
     *   Только 1 победитель из Redis проходит сюда.
     *   Гарантирует ACID и консистентность данных.
     *
     * finally-паттерн гарантирует очистку Redis-ключа при ЛЮБОЙ ошибке PG
     * (включая CannotGetJdbcConnectionException, OOM, network failure).
     */
    public LockResponse tryLock(Long eventId, Long seatId, Long userId) {
        // ─── Уровень 1: Redis SETNX ───────────────────────────────────────────
        // TTL = lock_minutes + 30 сек буфер (SafetyNet чистит PG раньше истечения Redis-ключа)
        // Сначала проверяем кэш. Если место занято, отбиваем сразу (за 1-2 мс).
        Duration redisTtl = Duration.ofSeconds((long) lockDurationMinutes * 60 + 30);
        boolean redisAcquired = distributedLock.tryAcquire(eventId, seatId, userId, redisTtl);
        if (!redisAcquired) {
            log.debug("Redis fast-reject: seat {} event {} — already locked", seatId, eventId);
            throw new SeatAlreadyLockedException("Seat " + seatId + " for event " + eventId);
        }
        // ─────────────────────────────────────────────────────────────────────

        // Проверяем существование пользователя ПОСЛЕ получения блокировки Redis,
        // чтобы делать этот запрос к БД только 1 раз (для победителя).
        if (!userLookup.existsById(userId)) {
            distributedLock.release(eventId, seatId);
            throw new ResourceNotFoundException("User", userId);
        }

        // ─── Уровень 2: PostgreSQL FOR UPDATE NOWAIT ──────────────────────────
        // finally гарантирует rollback Redis-ключа при ЛЮБОЙ ошибке PG
        boolean pgSuccess = false;
        try {
            LockResponse result = transactionTemplate.execute(status -> {
                // SELECT FOR UPDATE NOWAIT — атомарно захватываем строку
                Ticket ticket = ticketRepository.findAvailableForUpdate(eventId, seatId, Instant.now())
                    .orElseThrow(() -> new SeatAlreadyLockedException(
                        "Seat " + seatId + " for event " + eventId));

                Instant expiresAt = Instant.now().plus(lockDurationMinutes, ChronoUnit.MINUTES);

                ticket.setStatus(TicketStatus.LOCKED);
                ticket.setUserId(userId);
                ticket.setLockExpiresAt(expiresAt);

                ticketRepository.save(ticket);

                log.info("LOCKED seat {} event {} user {} (expires {})",
                    seatId, eventId, userId, expiresAt);

                seatService.evictSeatsCache(eventId);
                return new LockResponse(ticket.getId(), expiresAt);
            });
            pgSuccess = true;
            return result;
        } catch (PessimisticLockingFailureException e) {
            log.debug("PG lock contention on seat {} event {} — should not happen if Redis works", seatId, eventId);
            throw new SeatAlreadyLockedException("Seat " + seatId + " for event " + eventId);
        } finally {
            // Откатить Redis-ключ если PG не смог завершить операцию
            if (!pgSuccess) {
                distributedLock.release(eventId, seatId);
            }
        }
        // ─────────────────────────────────────────────────────────────────────
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

            ticketRepository.save(ticket);

            // Освобождаем Redis-ключ: место теперь BOOKED, лок больше не нужен
            distributedLock.release(ticket.getEventId(), ticket.getSeatId());

            seatService.evictSeatsCache(ticket.getEventId());
            log.info("BOOKED ticket {} for user {}", ticketId, userId);
        } catch (PessimisticLockingFailureException e) {
            log.debug("Conflict on confirm ticket {} — seat is currently being processed", ticketId);
            throw new SeatAlreadyLockedException("Ticket is currently locked by another process, please try again");
        }
    }

    /**
     * Ручная отмена блокировки.
     * LOCKED → AVAILABLE
     *
     * Трейд-офф: если Redis временно недоступен при release() —
     * место зависнет на остаток TTL. Это допустимый бизнес-риск
     * при Redis-аутаже (PG уже AVAILABLE, SafetyNet не поможет).
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

            // Освобождаем Redis-ключ: место снова AVAILABLE
            distributedLock.release(ticket.getEventId(), ticket.getSeatId());

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