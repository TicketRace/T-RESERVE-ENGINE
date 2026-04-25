package com.treserve.booking;

import com.treserve.booking.dto.LockResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
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

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private SeatService seatService;

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

    @Test
    @DisplayName("tryLock: свободное место → успешная блокировка")
    void tryLock_success() {
        when(ticketRepository.findAvailableForUpdate(1L, 1L))
            .thenReturn(Optional.of(availableTicket));
        when(userRepository.getReferenceById(1L))
            .thenReturn(testUser);
        when(ticketRepository.save(any())).thenReturn(availableTicket);

        LockResponse response = bookingService.tryLock(1L, 1L, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getLockId()).isEqualTo(10L);
        assertThat(availableTicket.getStatus()).isEqualTo(TicketStatus.LOCKED);
        assertThat(availableTicket.getUser()).isEqualTo(testUser);
        verify(seatService).evictSeatsCache(1L);
    }

    @Test
    @DisplayName("tryLock: место не AVAILABLE (уже занято) → 409")
    void tryLock_alreadyLocked() {
        when(ticketRepository.findAvailableForUpdate(1L, 1L))
            .thenReturn(Optional.empty()); // нет AVAILABLE билета

        assertThatThrownBy(() -> bookingService.tryLock(1L, 1L, 1L))
            .isInstanceOf(SeatAlreadyLockedException.class);

        verify(ticketRepository, never()).save(any());
        verify(seatService, never()).evictSeatsCache(any());
    }
}
