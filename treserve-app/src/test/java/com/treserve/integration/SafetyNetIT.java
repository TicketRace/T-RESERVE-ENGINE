package com.treserve.integration;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.booking.service.SafetyNetScheduler;
import com.treserve.booking.service.SeatService;
import com.treserve.support.AbstractPostgresIntegrationTest;
import com.treserve.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyNetIT extends AbstractPostgresIntegrationTest {

    private static final long EVENT_ID = 1L;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SeatService seatService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("SafetyNet освобождает просроченные LOCKED билеты")
    void releaseExpiredLocksReturnsExpiredTicketsToAvailable() {
        Ticket expiredTicket = ticketRepository.findByEventId(EVENT_ID).get(0);
        Ticket activeTicket = ticketRepository.findByEventId(EVENT_ID).get(1);
        Long userId = testUserId();

        jdbcTemplate.update("""
                UPDATE tickets
                SET status = 'LOCKED', user_id = ?, lock_expires_at = ?, booked_at = NULL
                WHERE id = ?
                """, userId, Timestamp.from(Instant.now().minusSeconds(60)), expiredTicket.getId());
        jdbcTemplate.update("""
                UPDATE tickets
                SET status = 'LOCKED', user_id = ?, lock_expires_at = ?, booked_at = NULL
                WHERE id = ?
                """, userId, Timestamp.from(Instant.now().plusSeconds(60)), activeTicket.getId());

        runSafetyNetJob();

        TicketState expiredState = readTicketState(expiredTicket.getId());
        assertThat(expiredState.status()).isEqualTo(TicketStatus.AVAILABLE.name());
        assertThat(expiredState.userId()).isNull();
        assertThat(expiredState.lockExpiresAt()).isNull();
        assertThat(expiredState.bookedAt()).isNull();

        TicketState activeState = readTicketState(activeTicket.getId());
        assertThat(activeState.status()).isEqualTo(TicketStatus.LOCKED.name());
        assertThat(activeState.userId()).isEqualTo(userId);
        assertThat(activeState.lockExpiresAt()).isNotNull();
        assertThat(activeState.bookedAt()).isNull();
    }

    private void runSafetyNetJob() {
        SafetyNetScheduler safetyNetScheduler = new SafetyNetScheduler(ticketRepository, seatService);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> safetyNetScheduler.releaseExpiredLocks());
    }

    private Long testUserId() {
        return userRepository.findByEmail("user@treserve.com").orElseThrow().getId();
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
