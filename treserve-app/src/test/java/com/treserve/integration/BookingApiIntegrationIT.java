package com.treserve.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.treserve.auth.service.JwtService;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.support.AbstractPostgresIntegrationTest;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class BookingApiIntegrationIT extends AbstractPostgresIntegrationTest {

    private static final long EVENT_ID = 1L;
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("GET /api/events доступен без токена")
    void publicEventsEndpointIsAvailableWithoutToken() throws Exception {
        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].id", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/bookings/lock без JWT возвращает 401 и не меняет БД")
    void lockEndpointWithoutTokenReturnsUnauthorizedAndDoesNotChangeDatabase() throws Exception {
        Ticket ticket = firstTicketForEvent(EVENT_ID);

        mockMvc.perform(post("/api/bookings/lock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(lockJson(EVENT_ID, ticket.getSeatId())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        TicketState state = readTicketState(ticket.getId());
        assertThat(state.status()).isEqualTo(TicketStatus.AVAILABLE.name());
        assertThat(state.userId()).isNull();
        assertThat(state.lockExpiresAt()).isNull();
        assertThat(state.bookedAt()).isNull();
    }

    @Test
    @DisplayName("POST /api/bookings/lock с invalid JWT возвращает 401 и не меняет БД")
    void lockEndpointWithInvalidJwtReturnsUnauthorizedAndDoesNotChangeDatabase() throws Exception {
        Ticket ticket = firstTicketForEvent(EVENT_ID);

        mockMvc.perform(post("/api/bookings/lock")
                .header("Authorization", bearer("invalid-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(lockJson(EVENT_ID, ticket.getSeatId())))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));

        TicketState state = readTicketState(ticket.getId());
        assertThat(state.status()).isEqualTo(TicketStatus.AVAILABLE.name());
        assertThat(state.userId()).isNull();
        assertThat(state.lockExpiresAt()).isNull();
        assertThat(state.bookedAt()).isNull();
    }

    @Test
    @DisplayName("ADMIN endpoint запрещён пользователю с ролью USER")
    void adminEndpointRejectsUserRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", bearer(userToken())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("ADMIN endpoint доступен пользователю с ролью ADMIN")
    void adminEndpointAcceptsAdminRole() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard")
                .header("Authorization", bearer(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalEvents").isNumber())
            .andExpect(jsonPath("$.totalSoldTickets").isNumber())
            .andExpect(jsonPath("$.totalRevenue").isNumber());
    }

    @Test
    @DisplayName("HTTP flow: lock → confirm сохраняет BOOKED в БД")
    void lockAndConfirmViaHttpPersistsBookedState() throws Exception {
        Ticket ticket = firstTicketForEvent(EVENT_ID);

        Long lockId = jsonNumber(mockMvc.perform(post("/api/bookings/lock")
                    .header("Authorization", bearer(userToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(lockJson(EVENT_ID, ticket.getSeatId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lockId", notNullValue()))
                .andExpect(jsonPath("$.expiresAt", notNullValue()))
                .andReturn()
                .getResponse()
                .getContentAsString(),
            "lockId"
        );

        mockMvc.perform(post("/api/bookings/{ticketId}/confirm", lockId)
                .header("Authorization", bearer(userToken())))
            .andExpect(status().isOk());

        TicketState state = readTicketState(lockId);
        assertThat(state.status()).isEqualTo(TicketStatus.BOOKED.name());
        assertThat(state.userId()).isEqualTo(testUserId());
        assertThat(state.lockExpiresAt()).isNull();
        assertThat(state.bookedAt()).isNotNull();
    }

    @Test
    @DisplayName("Попытка подтвердить чужую блокировку возвращает 403 и не меняет БД")
    void confirmForeignLockReturnsForbiddenAndLeavesDatabaseUnchanged() throws Exception {
        Ticket ticket = firstTicketForEvent(EVENT_ID);
        Long lockId = lockSeat(ticket.getSeatId(), userToken());

        mockMvc.perform(post("/api/bookings/{ticketId}/confirm", lockId)
                .header("Authorization", bearer(adminToken())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));

        TicketState state = readTicketState(lockId);
        assertThat(state.status()).isEqualTo(TicketStatus.LOCKED.name());
        assertThat(state.userId()).isEqualTo(testUserId());
        assertThat(state.lockExpiresAt()).isNotNull();
        assertThat(state.bookedAt()).isNull();
    }

    @Test
    @DisplayName("Попытка отменить чужую блокировку возвращает 403 и не меняет БД")
    void cancelForeignLockReturnsForbiddenAndLeavesDatabaseUnchanged() throws Exception {
        Ticket ticket = firstTicketForEvent(EVENT_ID);
        Long lockId = lockSeat(ticket.getSeatId(), userToken());

        mockMvc.perform(delete("/api/bookings/{ticketId}", lockId)
                .header("Authorization", bearer(adminToken())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));

        TicketState state = readTicketState(lockId);
        assertThat(state.status()).isEqualTo(TicketStatus.LOCKED.name());
        assertThat(state.userId()).isEqualTo(testUserId());
        assertThat(state.lockExpiresAt()).isNotNull();
        assertThat(state.bookedAt()).isNull();
    }

    @Test
    @DisplayName("Повторный lock того же места возвращает 409, а не 5xx")
    void repeatedLockForSameSeatReturnsConflict() throws Exception {
        Ticket ticket = firstTicketForEvent(EVENT_ID);

        mockMvc.perform(post("/api/bookings/lock")
                .header("Authorization", bearer(userToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(lockJson(EVENT_ID, ticket.getSeatId())))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/bookings/lock")
                .header("Authorization", bearer(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(lockJson(EVENT_ID, ticket.getSeatId())))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Валидационная ошибка lock возвращает 400 и не меняет билеты")
    void lockValidationErrorReturnsBadRequestAndLeavesTicketsUnchanged() throws Exception {
        mockMvc.perform(post("/api/bookings/lock")
                .header("Authorization", bearer(userToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":" + EVENT_ID + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        Integer changedTickets = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tickets
                WHERE event_id = ? AND status <> 'AVAILABLE'
                """, Integer.class, EVENT_ID);
        assertThat(changedTickets).isZero();
    }

    private Ticket firstTicketForEvent(long eventId) {
        return ticketRepository.findByEventId(eventId).get(0);
    }

    private Long lockSeat(long seatId, String token) throws Exception {
        String response = mockMvc.perform(post("/api/bookings/lock")
                .header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(lockJson(EVENT_ID, seatId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.lockId", notNullValue()))
            .andReturn()
            .getResponse()
            .getContentAsString();
        return jsonNumber(response, "lockId");
    }

    private String lockJson(long eventId, long seatId) {
        return "{\"eventId\":" + eventId + ",\"seatId\":" + seatId + "}";
    }

    private String userToken() {
        User user = userByEmail("user@treserve.com");
        return jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
    }

    private String adminToken() {
        User admin = userByEmail("admin@treserve.com");
        return jwtService.generateAccessToken(admin.getId(), admin.getEmail(), admin.getRole());
    }

    private Long testUserId() {
        return userByEmail("user@treserve.com").getId();
    }

    private User userByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private Long jsonNumber(String json, String fieldName) throws Exception {
        return objectMapper.readTree(json).get(fieldName).asLong();
    }

    private TicketState readTicketState(long ticketId) {
        return jdbcTemplate.queryForObject("""
                SELECT status, user_id, lock_expires_at, booked_at
                FROM tickets
                WHERE id = ?
                """,
            (rs, rowNum) -> new TicketState(
                rs.getString("status"),
                rs.getObject("user_id", Long.class),
                rs.getTimestamp("lock_expires_at"),
                rs.getTimestamp("booked_at")
            ),
            ticketId
        );
    }

    private record TicketState(String status, Long userId, Timestamp lockExpiresAt, Timestamp bookedAt) {
    }
}
