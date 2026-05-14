package com.treserve.integration;

import com.treserve.booking.service.BookingService;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.dto.LockResponse;
import com.treserve.common.exception.SeatAlreadyLockedException;
import com.treserve.support.AbstractPostgresIntegrationTest;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class BookingConcurrencyIT extends AbstractPostgresIntegrationTest {

    private static final long EVENT_ID = 1L;
    private static final int ATTEMPTS = 8;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("concurrency: только один пользователь может заблокировать одно место")
    void onlyOneConcurrentLockWinsForTheSameSeat() throws Exception {
        Ticket targetTicket = ticketRepository.findByEventIdWithSeat(EVENT_ID).get(0);
        List<Long> userIds = createUsers(ATTEMPTS);

        CountDownLatch readyGate = new CountDownLatch(ATTEMPTS);
        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(ATTEMPTS);

        try {
            List<Callable<AttemptResult>> tasks = userIds.stream()
                .map(userId -> (Callable<AttemptResult>) () -> tryLockAtTheSameTime(
                    readyGate,
                    startGate,
                    EVENT_ID,
                    targetTicket.getSeat().getId(),
                    userId
                ))
                .toList();

            List<Future<AttemptResult>> futures = tasks.stream()
                .map(executor::submit)
                .toList();

            assertThat(readyGate.await(5, TimeUnit.SECONDS)).isTrue();
            startGate.countDown();

            List<AttemptResult> results = new ArrayList<>();
            for (Future<AttemptResult> future : futures) {
                results.add(future.get(10, TimeUnit.SECONDS));
            }

            assertThat(results).filteredOn(AttemptResult::success).hasSize(1);
            assertThat(results).filteredOn(result -> result.type() == AttemptType.CONFLICT).hasSize(ATTEMPTS - 1);
            assertThat(results).filteredOn(result -> result.type() == AttemptType.UNEXPECTED_ERROR).isEmpty();

            AttemptResult winner = results.stream()
                .filter(AttemptResult::success)
                .findFirst()
                .orElseThrow();

            TicketState state = readTicketState(targetTicket.getId());
            assertThat(state.status()).isEqualTo(TicketStatus.LOCKED.name());
            assertThat(state.userId()).isEqualTo(winner.userId());
            assertThat(state.lockExpiresAtIsSet()).isTrue();

            Integer ticketCopies = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM tickets
                    WHERE event_id = ? AND seat_id = ?
                    """, Integer.class, EVENT_ID, targetTicket.getSeat().getId());
            assertThat(ticketCopies).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private AttemptResult tryLockAtTheSameTime(
        CountDownLatch readyGate,
        CountDownLatch startGate,
        long eventId,
        long seatId,
        long userId
    ) throws InterruptedException {
        readyGate.countDown();
        assertThat(startGate.await(5, TimeUnit.SECONDS)).isTrue();

        try {
            LockResponse response = bookingService.tryLock(eventId, seatId, userId);
            return AttemptResult.success(userId, response.getLockId());
        } catch (SeatAlreadyLockedException expectedConflict) {
            return AttemptResult.conflict(userId);
        } catch (RuntimeException unexpected) {
            return AttemptResult.unexpectedError(userId, unexpected);
        }
    }

    private List<Long> createUsers(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = userRepository.saveAndFlush(User.builder()
                .email("concurrent-" + i + "-" + UUID.randomUUID() + "@treserve.test")
                .passwordHash("test-hash")
                .name("Concurrent User " + i)
                .role("USER")
                .build());
            ids.add(user.getId());
        }
        return ids;
    }

    private TicketState readTicketState(long ticketId) {
        return jdbcTemplate.queryForObject("""
                SELECT status, user_id, lock_expires_at IS NOT NULL AS lock_expires_at_is_set
                FROM tickets
                WHERE id = ?
                """,
            (rs, rowNum) -> new TicketState(
                rs.getString("status"),
                rs.getObject("user_id", Long.class),
                rs.getBoolean("lock_expires_at_is_set")
            ),
            ticketId
        );
    }

    private enum AttemptType {
        SUCCESS,
        CONFLICT,
        UNEXPECTED_ERROR
    }

    private record AttemptResult(AttemptType type, Long userId, Long lockId, String errorClass, String errorMessage) {

        static AttemptResult success(Long userId, Long lockId) {
            return new AttemptResult(AttemptType.SUCCESS, userId, lockId, null, null);
        }

        static AttemptResult conflict(Long userId) {
            return new AttemptResult(AttemptType.CONFLICT, userId, null, null, null);
        }

        static AttemptResult unexpectedError(Long userId, RuntimeException exception) {
            return new AttemptResult(
                AttemptType.UNEXPECTED_ERROR,
                userId,
                null,
                exception.getClass().getName(),
                exception.getMessage()
            );
        }

        boolean success() {
            return type == AttemptType.SUCCESS;
        }
    }

    private record TicketState(String status, Long userId, boolean lockExpiresAtIsSet) {
    }
}
