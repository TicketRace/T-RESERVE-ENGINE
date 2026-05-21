package com.treserve.integration;

import com.treserve.admin.service.AdminService;
import com.treserve.auth.service.JwtService;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.event.repository.EventRepository;
import com.treserve.event.dto.EventCreateRequest;
import com.treserve.event.dto.EventResponse;
import com.treserve.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AdminEventApiIntegrationIT extends AbstractPostgresIntegrationTest {

    private static final long ADMIN_ID = 1L;
    private static final long VENUE_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("DELETE /api/admin/events/{id} с BOOKED-билетом возвращает 409 и не меняет БД")
    void deleteEventWithBookedTicketReturnsConflictAndLeavesDataUnchanged() throws Exception {
        EventResponse event = adminService.createEvent(createEventRequest(), ADMIN_ID);
        Ticket ticket = ticketRepository.findByEventId(event.getId()).get(0);
        jdbcTemplate.update("""
                UPDATE tickets
                SET status = 'BOOKED', user_id = 2, booked_at = NOW()
                WHERE id = ?
                """, ticket.getId());

        mockMvc.perform(delete("/api/admin/events/{id}", event.getId())
                .header("Authorization", bearer(adminToken())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));

        assertThat(eventRepository.findById(event.getId())).isPresent();
        Integer ticketCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM tickets WHERE event_id = ?",
            Integer.class,
            event.getId()
        );
        assertThat(ticketCount).isEqualTo(50);
    }

    private EventCreateRequest createEventRequest() {
        EventCreateRequest request = new EventCreateRequest();
        request.setTitle("Event With Sold Ticket via API");
        request.setDescription("Created by admin API integration test");
        request.setVenueId(VENUE_ID);
        request.setStartTime(Instant.now().plusSeconds(86_400));
        request.setBasePrice(new BigDecimal("1000.00"));
        request.setCategory("CINEMA");
        request.setAgeRestriction("12+");
        request.setDurationMinutes(120);
        return request;
    }

    private String adminToken() {
        return jwtService.generateAccessToken(ADMIN_ID, "admin@treserve.com", "ADMIN");
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
