package com.treserve.admin.service;

import com.treserve.admin.dto.CheckInResponse;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.common.exception.TicketAlreadyUsedException;
import com.treserve.event.entity.Event;
import com.treserve.event.repository.EventRepository;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import com.treserve.venue.entity.Seat;
import com.treserve.venue.entity.Venue;
import com.treserve.venue.repository.SeatRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class AdminCheckInServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("checkInById: BOOKED билет переводится в USED и возвращает данные для контроля входа")
    void checkInById_whenBooked_marksTicketUsedAndBuildsResponse() {
        AdminCheckInService service = service();
        Ticket ticket = bookedTicket();
        Event event = event();
        Seat seat = seat();
        User user = user();
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(seatRepository.findById(20L)).thenReturn(Optional.of(seat));
        when(userRepository.findById(30L)).thenReturn(Optional.of(user));

        CheckInResponse response = service.checkInById(100L);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TicketStatus.USED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.USED);

        assertThat(response.getMessage()).isEqualTo("Checked in successfully");
        assertThat(response.getStatus()).isEqualTo("USED");
        assertThat(response.getPreviousStatus()).isEqualTo("BOOKED");
        assertThat(response.getTicketId()).isEqualTo(100L);
        assertThat(response.getEventId()).isEqualTo(10L);
        assertThat(response.getEventTitle()).isEqualTo("Spring Fest");
        assertThat(response.getVenueName()).isEqualTo("Main Hall");
        assertThat(response.getSeatLabel()).isEqualTo("A-12");
        assertThat(response.getCustomerEmail()).isEqualTo("guest@example.com");
        assertThat(response.getPrice()).isEqualByComparingTo("2500.00");
    }

    @Test
    @DisplayName("checkIn: билет ищется по verifyToken")
    void checkIn_usesVerifyTokenLookup() {
        AdminCheckInService service = service();
        UUID token = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Ticket ticket = bookedTicket();
        when(ticketRepository.findByVerifyToken(token)).thenReturn(Optional.of(ticket));
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event()));
        when(seatRepository.findById(20L)).thenReturn(Optional.of(seat()));
        when(userRepository.findById(30L)).thenReturn(Optional.of(user()));

        CheckInResponse response = service.checkIn(token);

        assertThat(response.getTicketId()).isEqualTo(100L);
        verify(ticketRepository).findByVerifyToken(token);
    }

    @Test
    @DisplayName("checkInById: USED билет нельзя использовать повторно")
    void checkInById_whenAlreadyUsed_throwsTicketAlreadyUsed() {
        AdminCheckInService service = service();
        Ticket ticket = bookedTicket();
        ticket.setStatus(TicketStatus.USED);
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.checkInById(100L))
                .isInstanceOf(TicketAlreadyUsedException.class)
                .hasMessageContaining("Ticket already used");

        verify(ticketRepository, never()).save(ticket);
        verifyNoInteractions(eventRepository, seatRepository, userRepository);
    }

    @Test
    @DisplayName("checkInById: только BOOKED билет можно отметить как USED")
    void checkInById_whenNotBooked_throwsIllegalState() {
        AdminCheckInService service = service();
        Ticket ticket = bookedTicket();
        ticket.setStatus(TicketStatus.LOCKED);
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.checkInById(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ticket is not in BOOKED state");

        verify(ticketRepository, never()).save(ticket);
        verifyNoInteractions(eventRepository, seatRepository, userRepository);
    }

    @Test
    @DisplayName("checkInById: неизвестный билет → ResourceNotFoundException")
    void checkInById_whenTicketNotFound_throwsResourceNotFound() {
        AdminCheckInService service = service();
        when(ticketRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkInById(404L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(eventRepository, seatRepository, userRepository);
    }

    private AdminCheckInService service() {
        return new AdminCheckInService(ticketRepository, eventRepository, seatRepository, userRepository);
    }

    private static Ticket bookedTicket() {
        return Ticket.builder()
                .id(100L)
                .eventId(10L)
                .seatId(20L)
                .userId(30L)
                .status(TicketStatus.BOOKED)
                .price(new BigDecimal("2500.00"))
                .verifyToken(UUID.fromString("11111111-1111-1111-1111-111111111111"))
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
