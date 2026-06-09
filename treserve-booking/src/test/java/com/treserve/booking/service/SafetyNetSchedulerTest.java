package com.treserve.booking.service;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SafetyNetSchedulerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private SeatService seatService;

    @Test
    @DisplayName("releaseExpiredLocks: нет просроченных блокировок → не сохраняет и не трогает кэш")
    void releaseExpiredLocks_whenNothingExpired_doesNothing() {
        SafetyNetScheduler scheduler = new SafetyNetScheduler(ticketRepository, seatService);
        when(ticketRepository.findExpiredLocks(any(Instant.class))).thenReturn(List.of());

        scheduler.releaseExpiredLocks();

        verify(ticketRepository, never()).saveAll(any());
        verifyNoInteractions(seatService);
    }

    @Test
    @DisplayName("releaseExpiredLocks: освобождает просроченные LOCKED билеты и инвалидирует каждый event один раз")
    void releaseExpiredLocks_releasesExpiredTicketsAndEvictsEachEventOnce() {
        SafetyNetScheduler scheduler = new SafetyNetScheduler(ticketRepository, seatService);
        Ticket first = expiredTicket(1L, 10L, 101L);
        Ticket secondSameEvent = expiredTicket(2L, 10L, 102L);
        Ticket thirdOtherEvent = expiredTicket(3L, 11L, 103L);
        List<Ticket> expired = List.of(first, secondSameEvent, thirdOtherEvent);
        when(ticketRepository.findExpiredLocks(any(Instant.class))).thenReturn(expired);

        scheduler.releaseExpiredLocks();

        assertReleased(first);
        assertReleased(secondSameEvent);
        assertReleased(thirdOtherEvent);

        ArgumentCaptor<Iterable<Ticket>> captor = iterableCaptor();
        verify(ticketRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).containsExactlyElementsOf(expired);
        verify(seatService, times(1)).evictSeatsCache(10L);
        verify(seatService, times(1)).evictSeatsCache(11L);
    }

    private static Ticket expiredTicket(Long id, Long eventId, Long userId) {
        return Ticket.builder()
                .id(id)
                .eventId(eventId)
                .seatId(id + 100)
                .status(TicketStatus.LOCKED)
                .userId(userId)
                .lockExpiresAt(Instant.parse("2026-05-01T10:00:00Z"))
                .build();
    }

    private static void assertReleased(Ticket ticket) {
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
        assertThat(ticket.getUserId()).isNull();
        assertThat(ticket.getLockExpiresAt()).isNull();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Iterable<Ticket>> iterableCaptor() {
        return ArgumentCaptor.forClass(Iterable.class);
    }
}
