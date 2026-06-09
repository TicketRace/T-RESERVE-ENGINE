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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TicketAdminServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("generateTicketsForEvent: создаёт AVAILABLE билеты для всех переданных мест")
    void generateTicketsForEvent_createsAvailableTicketsForEachSeat() {
        TicketAdminService service = new TicketAdminService(ticketRepository);
        BigDecimal basePrice = new BigDecimal("1500.00");

        service.generateTicketsForEvent(42L, basePrice, List.of(101L, 102L, 103L));

        ArgumentCaptor<Iterable<Ticket>> captor = iterableCaptor();
        verify(ticketRepository).saveAll(captor.capture());

        List<Ticket> saved = StreamSupport.stream(captor.getValue().spliterator(), false).toList();
        assertThat(saved).hasSize(3);
        assertThat(saved).extracting(Ticket::getEventId).containsExactly(42L, 42L, 42L);
        assertThat(saved).extracting(Ticket::getSeatId).containsExactly(101L, 102L, 103L);
        assertThat(saved).extracting(Ticket::getStatus)
                .containsExactly(TicketStatus.AVAILABLE, TicketStatus.AVAILABLE, TicketStatus.AVAILABLE);
        assertThat(saved).extracting(Ticket::getPrice)
                .allSatisfy(price -> assertThat(price).isEqualByComparingTo(basePrice));
    }

    @Test
    @DisplayName("deleteTicketsForEvent: удаляет билеты события одним bulk-вызовом")
    void deleteTicketsForEvent_delegatesToBulkDelete() {
        TicketAdminService service = new TicketAdminService(ticketRepository);

        service.deleteTicketsForEvent(42L);

        verify(ticketRepository).deleteByEventId(42L);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Iterable<Ticket>> iterableCaptor() {
        return ArgumentCaptor.forClass(Iterable.class);
    }
}
