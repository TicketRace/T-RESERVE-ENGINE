package com.treserve.booking;

import com.treserve.booking.dto.LockResponse;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.port.UserLookup;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.booking.service.BookingService;
import com.treserve.booking.service.SeatService;
import com.treserve.common.exception.ForbiddenOperationException;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.common.exception.SeatAlreadyLockedException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для BookingService.
 * Тестируем только бизнес-логику сервиса, не Spring/JPA.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserLookup userLookup;   // ← интерфейс вместо UserRepository
    @Mock
    private SeatService seatService;
    @Mock
    private org.springframework.transaction.support.TransactionTemplate transactionTemplate;

    @InjectMocks
    private BookingService bookingService;

    private Ticket availableTicket;
    private Ticket lockedTicket;

    private static final Long EVENT_ID  = 1L;
    private static final Long SEAT_ID   = 1L;
    private static final Long USER_ID   = 1L;
    private static final Long TICKET_ID = 10L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "lockDurationMinutes", 10);

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        // AVAILABLE билет
        availableTicket = Ticket.builder()
                .eventId(EVENT_ID)
                .seatId(SEAT_ID)
                .status(TicketStatus.AVAILABLE)
                .build();
        ReflectionTestUtils.setField(availableTicket, "id", TICKET_ID);

        // LOCKED билет — активный лок (не истёк)
        lockedTicket = Ticket.builder()
                .eventId(EVENT_ID)
                .seatId(SEAT_ID)
                .status(TicketStatus.LOCKED)
                .userId(USER_ID)
                .lockExpiresAt(Instant.now().plusSeconds(600))
                .build();
        ReflectionTestUtils.setField(lockedTicket, "id", TICKET_ID);
    }

    // ─── tryLock ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("tryLock: свободное место → успешная блокировка + expiresAt ≈ now+10min")
    void tryLock_success() {
        when(ticketRepository.findAvailableForUpdate(eq(EVENT_ID), eq(SEAT_ID), any(Instant.class)))
                .thenReturn(Optional.of(availableTicket));
        when(userLookup.existsById(USER_ID)).thenReturn(true);
        when(ticketRepository.save(any())).thenReturn(availableTicket);

        Instant before = Instant.now();
        LockResponse response = bookingService.tryLock(EVENT_ID, SEAT_ID, USER_ID);
        Instant after = Instant.now();

        assertThat(response).isNotNull();
        assertThat(response.getLockId()).isEqualTo(TICKET_ID);
        assertThat(availableTicket.getStatus()).isEqualTo(TicketStatus.LOCKED);
        assertThat(availableTicket.getUserId()).isEqualTo(USER_ID);

        // Проверка expiresAt ≈ now + 10 min
        assertThat(response.getExpiresAt())
                .isBetween(
                        before.plus(10, ChronoUnit.MINUTES),
                        after.plus(10, ChronoUnit.MINUTES));
        assertThat(availableTicket.getLockExpiresAt()).isEqualTo(response.getExpiresAt());

        verify(seatService).evictSeatsCache(EVENT_ID);
    }

    @Test
    @DisplayName("tryLock: место не AVAILABLE (уже занято) → SeatAlreadyLockedException")
    void tryLock_alreadyLocked() {
        when(userLookup.existsById(USER_ID)).thenReturn(true);
        when(ticketRepository.findAvailableForUpdate(eq(EVENT_ID), eq(SEAT_ID), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.tryLock(EVENT_ID, SEAT_ID, USER_ID))
                .isInstanceOf(SeatAlreadyLockedException.class);

        verify(userLookup, times(1)).existsById(USER_ID);
        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    @Test
    @DisplayName("tryLock: PG блокировка (строка занята другой транзакцией) → SeatAlreadyLockedException")
    void tryLock_pgLockContention() {
        when(userLookup.existsById(USER_ID)).thenReturn(true);
        when(ticketRepository.findAvailableForUpdate(eq(EVENT_ID), eq(SEAT_ID), any(Instant.class)))
                .thenThrow(new PessimisticLockingFailureException("FOR UPDATE NOWAIT failed"));

        assertThatThrownBy(() -> bookingService.tryLock(EVENT_ID, SEAT_ID, USER_ID))
                .isInstanceOf(SeatAlreadyLockedException.class);

        verify(userLookup, times(1)).existsById(USER_ID);
        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    // ─── confirm ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("confirm: LOCKED билет → BOOKED + bookedAt установлен")
    void confirm_success() {
        when(ticketRepository.findByIdForUpdate(TICKET_ID))
                .thenReturn(Optional.of(lockedTicket));
        when(ticketRepository.save(any())).thenReturn(lockedTicket);

        bookingService.confirm(TICKET_ID, USER_ID);

        assertThat(lockedTicket.getStatus()).isEqualTo(TicketStatus.BOOKED);
        assertThat(lockedTicket.getLockExpiresAt()).isNull();
        assertThat(lockedTicket.getBookedAt()).isNotNull();
        assertThat(lockedTicket.getUserId()).isEqualTo(USER_ID);
        verify(ticketRepository).save(lockedTicket);
        verify(seatService).evictSeatsCache(EVENT_ID);
    }

    @Test
    @DisplayName("confirm: истёкший лок → IllegalArgumentException")
    void confirm_expired() {
        lockedTicket.setLockExpiresAt(Instant.now().minusSeconds(60));
        when(ticketRepository.findByIdForUpdate(TICKET_ID))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.confirm(TICKET_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lock expired");

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    @Test
    @DisplayName("confirm: чужой лок → ForbiddenOperationException")
    void confirm_wrongUser() {
        when(ticketRepository.findByIdForUpdate(TICKET_ID))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.confirm(TICKET_ID, 99L))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("not locked by you");

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
        assertThat(lockedTicket.getStatus()).isEqualTo(TicketStatus.LOCKED);
    }

    @Test
    @DisplayName("confirm: билет не LOCKED → IllegalArgumentException")
    void confirm_notLocked() {
        lockedTicket.setStatus(TicketStatus.AVAILABLE);
        when(ticketRepository.findByIdForUpdate(TICKET_ID))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.confirm(TICKET_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in LOCKED state");

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    @Test
    @DisplayName("confirm: билет не найден → ResourceNotFoundException")
    void confirm_notFound() {
        when(ticketRepository.findByIdForUpdate(TICKET_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirm(TICKET_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    // ─── cancel ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel: LOCKED билет → AVAILABLE, пользователь сброшен")
    void cancel_success() {
        when(ticketRepository.findByIdForUpdate(TICKET_ID))
                .thenReturn(Optional.of(lockedTicket));
        when(ticketRepository.save(any())).thenReturn(lockedTicket);

        bookingService.cancel(TICKET_ID, USER_ID);

        assertThat(lockedTicket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(lockedTicket.getUserId()).isNull();
        assertThat(lockedTicket.getLockExpiresAt()).isNull();
        verify(seatService).evictSeatsCache(EVENT_ID);
    }

    @Test
    @DisplayName("cancel: чужой лок → ForbiddenOperationException")
    void cancel_wrongUser() {
        when(ticketRepository.findByIdForUpdate(TICKET_ID))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.cancel(TICKET_ID, 99L))
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessageContaining("not locked by you");

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
        assertThat(lockedTicket.getStatus()).isEqualTo(TicketStatus.LOCKED);
    }

    @Test
    @DisplayName("cancel: билет не LOCKED → IllegalArgumentException")
    void cancel_notLocked() {
        lockedTicket.setStatus(TicketStatus.BOOKED);
        when(ticketRepository.findByIdForUpdate(TICKET_ID))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.cancel(TICKET_ID, USER_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can only cancel LOCKED");

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    @Test
    @DisplayName("cancel: билет не найден → ResourceNotFoundException")
    void cancel_notFound() {
        when(ticketRepository.findByIdForUpdate(TICKET_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancel(TICKET_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }
}
