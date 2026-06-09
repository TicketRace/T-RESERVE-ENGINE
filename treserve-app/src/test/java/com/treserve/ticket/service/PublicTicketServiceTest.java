package com.treserve.ticket.service;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.event.entity.Event;
import com.treserve.event.repository.EventRepository;
import com.treserve.ticket.dto.PublicTicketResponse;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import com.treserve.venue.entity.Seat;
import com.treserve.venue.entity.Venue;
import com.treserve.venue.repository.SeatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicTicketServiceTest {

    private static final UUID TOKEN = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private UserRepository userRepository;

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"BOOKED", "USED"})
    @DisplayName("getTicketByToken: публично доступны только BOOKED и USED билеты")
    void getTicketByToken_whenBookedOrUsed_returnsPublicTicket(TicketStatus status) {
        PublicTicketService service = service();
        when(ticketRepository.findByVerifyToken(TOKEN)).thenReturn(Optional.of(ticket(status)));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event()));
        when(seatRepository.findById(20L)).thenReturn(Optional.of(seat()));
        when(userRepository.findById(30L)).thenReturn(Optional.of(user()));

        PublicTicketResponse response = service.getTicketByToken(TOKEN);

        assertThat(response.getTicketId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(status.name());
        assertThat(response.getEventTitle()).isEqualTo("Spring Fest");
        assertThat(response.getEventDescription()).isEqualTo("Festival");
        assertThat(response.getVenueName()).isEqualTo("Main Hall");
        assertThat(response.getVenueAddress()).isEqualTo("Central street");
        assertThat(response.getSeatLabel()).isEqualTo("A-12");
        assertThat(response.getPrice()).isEqualByComparingTo("2500.00");
        assertThat(response.getCustomerName()).isEqualTo("Guest");
    }

    @ParameterizedTest
    @EnumSource(value = TicketStatus.class, names = {"AVAILABLE", "LOCKED"})
    @DisplayName("getTicketByToken: AVAILABLE/LOCKED билеты недоступны для публичной проверки")
    void getTicketByToken_whenUnavailableStatus_throwsIllegalState(TicketStatus status) {
        PublicTicketService service = service();
        when(ticketRepository.findByVerifyToken(TOKEN)).thenReturn(Optional.of(ticket(status)));

        assertThatThrownBy(() -> service.getTicketByToken(TOKEN))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ticket is not available for verification");

        verifyNoInteractions(eventRepository, seatRepository, userRepository);
    }

    @Test
    @DisplayName("getTicketByToken: неизвестный token → ResourceNotFoundException")
    void getTicketByToken_whenTokenNotFound_throwsResourceNotFound() {
        PublicTicketService service = service();
        when(ticketRepository.findByVerifyToken(TOKEN)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTicketByToken(TOKEN))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).findById(10L);
        verifyNoInteractions(seatRepository, userRepository);
    }

    private PublicTicketService service() {
        return new PublicTicketService(ticketRepository, eventRepository, seatRepository, userRepository);
    }

    private static Ticket ticket(TicketStatus status) {
        return Ticket.builder()
                .id(100L)
                .eventId(10L)
                .seatId(20L)
                .userId(30L)
                .status(status)
                .price(new BigDecimal("2500.00"))
                .verifyToken(TOKEN)
                .build();
    }

    private static Event event() {
        return Event.builder()
                .id(10L)
                .title("Spring Fest")
                .description("Festival")
                .startTime(Instant.parse("2026-05-01T19:00:00Z"))
                .basePrice(new BigDecimal("2500.00"))
                .venue(Venue.builder()
                        .id(5L)
                        .name("Main Hall")
                        .address("Central street")
                        .rowsCount(10)
                        .colsCount(20)
                        .build())
                .build();
    }

    private static Seat seat() {
        return Seat.builder()
                .id(20L)
                .rowLabel("A")
                .seatNumber(12)
                .build();
    }

    private static User user() {
        return User.builder()
                .id(30L)
                .email("guest@example.com")
                .name("Guest")
                .role("USER")
                .build();
    }
}
