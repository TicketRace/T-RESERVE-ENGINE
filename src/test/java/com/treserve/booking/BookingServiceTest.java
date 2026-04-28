package com.treserve.booking;

import com.treserve.booking.dto.LockResponse;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.common.exception.SeatAlreadyLockedException;
import com.treserve.event.Event;
import com.treserve.user.User;
import com.treserve.user.UserRepository;
import com.treserve.venue.Seat;
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
 *
 * Тестируем только бизнес-логику сервиса, не Spring/JPA.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SeatService seatService;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Event testEvent;
    private Ticket availableTicket;
    private Ticket lockedTicket;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookingService, "lockDurationMinutes", 10);

        testUser = User.builder().email("test@test.com").passwordHash("hash").build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testEvent = new Event();
        ReflectionTestUtils.setField(testEvent, "id", 1L);

        Seat testSeat = new Seat();
        ReflectionTestUtils.setField(testSeat, "id", 1L);

        // AVAILABLE билет
        availableTicket = Ticket.builder()
                .event(testEvent)
                .seat(testSeat)
                .status(TicketStatus.AVAILABLE)
                .build();
        ReflectionTestUtils.setField(availableTicket, "id", 10L);

        // LOCKED билет — активный лок (не истёк)
        lockedTicket = Ticket.builder()
                .event(testEvent)
                .seat(testSeat)
                .status(TicketStatus.LOCKED)
                .lockExpiresAt(Instant.now().plusSeconds(600))
                .build();
        ReflectionTestUtils.setField(lockedTicket, "id", 10L);
        lockedTicket.setUser(testUser);
    }

    // ─── tryLock ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("tryLock: свободное место → успешная блокировка + expiresAt ≈ now+10min")
    void tryLock_success() {
        when(ticketRepository.findAvailableForUpdate(1L, 1L))
                .thenReturn(Optional.of(availableTicket));
        when(userRepository.getReferenceById(1L))
                .thenReturn(testUser);
        when(ticketRepository.save(any())).thenReturn(availableTicket);

        Instant before = Instant.now();
        LockResponse response = bookingService.tryLock(1L, 1L, 1L);
        Instant after = Instant.now();

        assertThat(response).isNotNull();
        assertThat(response.getLockId()).isEqualTo(10L);
        assertThat(availableTicket.getStatus()).isEqualTo(TicketStatus.LOCKED);
        assertThat(availableTicket.getUser()).isEqualTo(testUser);

        // Проверка expiresAt ≈ now + 10 min (#3)
        assertThat(response.getExpiresAt())
                .isBetween(
                        before.plus(10, ChronoUnit.MINUTES),
                        after.plus(10, ChronoUnit.MINUTES));
        assertThat(availableTicket.getLockExpiresAt()).isEqualTo(response.getExpiresAt());

        verify(seatService).evictSeatsCache(1L);
    }

    @Test
    @DisplayName("tryLock: место не AVAILABLE (уже занято) → SeatAlreadyLockedException")
    void tryLock_alreadyLocked() {
        when(ticketRepository.findAvailableForUpdate(1L, 1L))
                .thenReturn(Optional.empty()); // нет AVAILABLE билета

        assertThatThrownBy(() -> bookingService.tryLock(1L, 1L, 1L))
                .isInstanceOf(SeatAlreadyLockedException.class);

        // Побочные эффекты: ничего не сохранено (#5)
        verify(userRepository, never()).getReferenceById(any());
        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    @Test
    @DisplayName("tryLock: PG блокировка (строка занята другой транзакцией) → SeatAlreadyLockedException")
    void tryLock_pgLockContention() {
        when(ticketRepository.findAvailableForUpdate(1L, 1L))
                .thenThrow(new PessimisticLockingFailureException("FOR UPDATE NOWAIT failed"));

        assertThatThrownBy(() -> bookingService.tryLock(1L, 1L, 1L))
                .isInstanceOf(SeatAlreadyLockedException.class);

        // Побочные эффекты: ничего не сохранено (#5)
        verify(userRepository, never()).getReferenceById(any());
        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    // ─── confirm ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("confirm: LOCKED билет → BOOKED + bookedAt установлен")
    void confirm_success() {
        when(ticketRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(lockedTicket));
        when(ticketRepository.save(any())).thenReturn(lockedTicket);

        bookingService.confirm(10L, 1L);

        assertThat(lockedTicket.getStatus()).isEqualTo(TicketStatus.BOOKED);
        assertThat(lockedTicket.getLockExpiresAt()).isNull();
        // Проверка bookedAt и user (#6)
        assertThat(lockedTicket.getBookedAt()).isNotNull();
        assertThat(lockedTicket.getUser()).isEqualTo(testUser);
        verify(ticketRepository).save(lockedTicket);
        verify(seatService).evictSeatsCache(1L);
    }

    @Test
    @DisplayName("confirm: истёкший лок → IllegalArgumentException")
    void confirm_expired() {
        lockedTicket.setLockExpiresAt(Instant.now().minusSeconds(60)); // истёк
        when(ticketRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.confirm(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Lock expired");

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    @Test
    @DisplayName("confirm: чужой лок → IllegalArgumentException")
    void confirm_wrongUser() {
        // lockedTicket принадлежит testUser (id=1)
        // Юзер 99 пытается подтвердить чужой лок
        when(ticketRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.confirm(10L, 99L)) // userId=99, не владелец
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not locked by you");

        // Побочные эффекты: ничего не изменилось (#7)
        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
        assertThat(lockedTicket.getStatus()).isEqualTo(TicketStatus.LOCKED);
    }
    
    // #1
    @Test
    @DisplayName("confirm: билет не LOCKED → IllegalArgumentException")
    void confirm_notLocked() {

        lockedTicket.setStatus(TicketStatus.AVAILABLE);
        when(ticketRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.confirm(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not in LOCKED state");

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    // #2
    @Test
    @DisplayName("confirm: билет не найден → ResourceNotFoundException")
    void confirm_notFound() {
        when(ticketRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.confirm(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    // ─── cancel ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancel: LOCKED билет → AVAILABLE, пользователь сброшен")
    void cancel_success() {
        when(ticketRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(lockedTicket));
        when(ticketRepository.save(any())).thenReturn(lockedTicket);

        bookingService.cancel(10L, 1L);

        assertThat(lockedTicket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(lockedTicket.getUser()).isNull();
        assertThat(lockedTicket.getLockExpiresAt()).isNull();
        verify(seatService).evictSeatsCache(1L);
    }

    @Test
    @DisplayName("cancel: чужой лок → IllegalArgumentException")
    void cancel_wrongUser() {
        // #8
        when(ticketRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.cancel(10L, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not locked by you");

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
        assertThat(lockedTicket.getStatus()).isEqualTo(TicketStatus.LOCKED);
    }

    @Test
    @DisplayName("cancel: билет не LOCKED → IllegalArgumentException")
    void cancel_notLocked() {
        // #8
        lockedTicket.setStatus(TicketStatus.BOOKED);
        when(ticketRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(lockedTicket));

        assertThatThrownBy(() -> bookingService.cancel(10L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Can only cancel LOCKED");

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }

    @Test
    @DisplayName("cancel: билет не найден → ResourceNotFoundException")
    void cancel_notFound() {
        // #8
        when(ticketRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.cancel(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }
}
