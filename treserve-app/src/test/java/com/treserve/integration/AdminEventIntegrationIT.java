package com.treserve.integration;

import com.treserve.admin.service.AdminService;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.common.exception.BusinessConflictException;
import com.treserve.event.repository.EventRepository;
import com.treserve.event.dto.EventCreateRequest;
import com.treserve.event.dto.EventResponse;
import com.treserve.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminEventIntegrationIT extends AbstractPostgresIntegrationTest {

    private static final long ADMIN_ID = 1L;
    private static final long VENUE_ID = 1L;

    @Autowired
    private AdminService adminService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("createEvent создаёт мероприятие и билеты для всех мест площадки в одной транзакции")
    void createEventGeneratesTicketsForEverySeatInVenue() {
        EventCreateRequest request = new EventCreateRequest();
        request.setTitle("Integration Test Event");
        request.setDescription("Created by integration test");
        request.setVenueId(VENUE_ID);
        request.setStartTime(Instant.now().plusSeconds(86_400));
        request.setBasePrice(new BigDecimal("750.00"));
        request.setCategory("CINEMA");
        request.setAgeRestriction("12+");
        request.setDurationMinutes(120);

        EventResponse response = adminService.createEvent(request, ADMIN_ID);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getVenueId()).isEqualTo(VENUE_ID);

        assertThat(eventRepository.findById(response.getId())).isPresent();
        assertThat(ticketRepository.findByEventId(response.getId()))
            .hasSize(50)
            .allSatisfy(ticket -> {
                assertThat(ticket.getStatus()).isEqualTo(TicketStatus.AVAILABLE);
                assertThat(ticket.getPrice()).isEqualByComparingTo("750.00");
            });

        Integer ticketsForVenue = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tickets t
                JOIN seats s ON s.id = t.seat_id
                WHERE t.event_id = ? AND s.venue_id = ?
                """, Integer.class, response.getId(), VENUE_ID);
        assertThat(ticketsForVenue).isEqualTo(50);
    }

    @Test
    @DisplayName("deleteEvent не удаляет мероприятие, если есть BOOKED-билет")
    void deleteEventWithBookedTicketsFailsAndLeavesDataUnchanged() {
        EventCreateRequest request = new EventCreateRequest();
        request.setTitle("Event With Sold Ticket");
        request.setVenueId(VENUE_ID);
        request.setStartTime(Instant.now().plusSeconds(86_400));
        request.setBasePrice(new BigDecimal("1000.00"));

        EventResponse response = adminService.createEvent(request, ADMIN_ID);
        Ticket ticket = ticketRepository.findByEventId(response.getId()).get(0);
        jdbcTemplate.update("""
                UPDATE tickets
                SET status = 'BOOKED', user_id = 2, booked_at = NOW()
                WHERE id = ?
                """, ticket.getId());

        assertThatThrownBy(() -> adminService.deleteEvent(response.getId()))
            .isInstanceOf(BusinessConflictException.class)
            .hasMessageContaining("BOOKED");

        assertThat(eventRepository.findById(response.getId())).isPresent();
        Integer ticketCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tickets WHERE event_id = ?",
            Integer.class,
            response.getId()
        );
        assertThat(ticketCount).isEqualTo(50);
    }
}
