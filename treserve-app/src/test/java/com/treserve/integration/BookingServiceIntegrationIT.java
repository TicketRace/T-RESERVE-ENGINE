package com.treserve.integration;

import com.treserve.booking.service.BookingService;
import com.treserve.booking.service.SeatService;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.dto.LockResponse;
import com.treserve.booking.dto.SeatInfo;
import com.treserve.support.AbstractPostgresIntegrationTest;
import com.treserve.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BookingServiceIntegrationIT extends AbstractPostgresIntegrationTest {

    private static final long EVENT_ID = 1L;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private SeatService seatService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("tryLock сохраняет LOCKED-состояние в PostgreSQL")
    void tryLockPersistsLockedStateInDatabase() {
        Ticket ticket = firstTicketForEvent(EVENT_ID);
        Long userId = testUserId();
        assertThat(seatStatusFromService(EVENT_ID, ticket.getSeatId())).isEqualTo(TicketStatus.AVAILABLE.name());

        LockResponse response = bookingService.tryLock(EVENT_ID, ticket.getSeatId(), userId);

        TicketState state = readTicketState(response.getLockId());
        assertThat(state.status()).isEqualTo(TicketStatus.LOCKED.name());
        assertThat(state.userId()).isEqualTo(userId);
        assertThat(state.lockExpiresAt()).isNotNull();
        assertThat(state.bookedAt()).isNull();
        assertThat(seatStatusFromService(EVENT_ID, ticket.getSeatId())).isEqualTo(TicketStatus.LOCKED.name());
    }

    @Test
    @DisplayName("confirm переводит LOCKED → BOOKED и очищает lock_expires_at")
    void confirmPersistsBookedStateInDatabase() {
        Ticket ticket = firstTicketForEvent(EVENT_ID);
        Long userId = testUserId();
        LockResponse lock = bookingService.tryLock(EVENT_ID, ticket.getSeatId(), userId);

        bookingService.confirm(lock.getLockId(), userId);

        TicketState state = readTicketState(lock.getLockId());
        assertThat(state.status()).isEqualTo(TicketStatus.BOOKED.name());
        assertThat(state.userId()).isEqualTo(userId);
        assertThat(state.lockExpiresAt()).isNull();
        assertThat(state.bookedAt()).isNotNull();
    }

    @Test
    @DisplayName("cancel переводит LOCKED → AVAILABLE и сбрасывает пользователя")
    void cancelReturnsTicketToAvailableAndClearsUser() {
        Ticket ticket = firstTicketForEvent(EVENT_ID);
        Long userId = testUserId();
        LockResponse lock = bookingService.tryLock(EVENT_ID, ticket.getSeatId(), userId);

        bookingService.cancel(lock.getLockId(), userId);

        TicketState state = readTicketState(lock.getLockId());
        assertThat(state.status()).isEqualTo(TicketStatus.AVAILABLE.name());
        assertThat(state.userId()).isNull();
        assertThat(state.lockExpiresAt()).isNull();
        assertThat(state.bookedAt()).isNull();
    }

    @Test
    @DisplayName("confirm просроченного LOCKED-билета не меняет состояние БД")
    void confirmExpiredLockLeavesDatabaseStateUnchanged() {
        Ticket ticket = firstTicketForEvent(EVENT_ID);
        Long userId = testUserId();
        Instant expiredAt = Instant.now().minusSeconds(60);
        jdbcTemplate.update("""
                UPDATE tickets
                SET status = 'LOCKED', user_id = ?, lock_expires_at = ?, booked_at = NULL
                WHERE id = ?
                """, userId, Timestamp.from(expiredAt), ticket.getId());

        assertThatThrownBy(() -> bookingService.confirm(ticket.getId(), userId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Lock expired");

        TicketState state = readTicketState(ticket.getId());
        assertThat(state.status()).isEqualTo(TicketStatus.LOCKED.name());
        assertThat(state.userId()).isEqualTo(userId);
        assertThat(state.lockExpiresAt()).isNotNull();
        assertThat(state.bookedAt()).isNull();
    }

    private String seatStatusFromService(long eventId, long seatId) {
        List<SeatInfo> seats = seatService.getSeats(eventId);
        return seats.stream()
            .filter(seat -> seat.getSeatId().equals(seatId))
            .findFirst()
            .orElseThrow()
            .getStatus();
    }

    private Long testUserId() {
        return userRepository.findByEmail("user@treserve.com").orElseThrow().getId();
    }

    private Ticket firstTicketForEvent(long eventId) {
        return ticketRepository.findByEventId(eventId).get(0);
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
