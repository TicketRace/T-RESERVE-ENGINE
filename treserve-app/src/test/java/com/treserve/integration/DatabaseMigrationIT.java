package com.treserve.integration;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseMigrationIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TicketRepository ticketRepository;

    @Test
    @DisplayName("Flyway применяет миграции и загружает seed-данные")
    void flywayMigrationsCreateSchemaAndSeedData() {
        assertThat(countRows("users")).isEqualTo(2);
        assertThat(countRows("venues")).isEqualTo(2);
        assertThat(countRows("seats")).isEqualTo(250);
        assertThat(countRows("events")).isEqualTo(3);
        assertThat(countRows("tickets")).isEqualTo(300);

        assertThat(ticketRepository.findSeatsByEventId(1L))
            .hasSize(50)
            .first()
            .extracting(row -> row.getSeatLabel())
            .isEqualTo("A-1");

        assertThat(indexExists("idx_tickets_event_status")).isTrue();
        assertThat(indexExists("idx_tickets_user")).isTrue();
        assertThat(indexExists("idx_tickets_locked_expires")).isTrue();
    }

    @Test
    @DisplayName("БД запрещает два билета на одно место одного мероприятия")
    void databaseRejectsDuplicateTicketForSameEventAndSeat() {
        Ticket existingTicket = ticketRepository.findByEventId(1L).get(0);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO tickets (event_id, seat_id, status, price)
                VALUES (?, ?, 'AVAILABLE', 500.00)
                """, existingTicket.getEventId(), existingTicket.getSeatId()))
            .isInstanceOf(DataIntegrityViolationException.class);

        Integer duplicates = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM tickets
                WHERE event_id = ? AND seat_id = ?
                """, Integer.class, existingTicket.getEventId(), existingTicket.getSeatId());

        assertThat(duplicates).isEqualTo(1);
    }

    @Test
    @DisplayName("БД запрещает дубли физических мест внутри одной площадки")
    void databaseRejectsDuplicateSeatInsideVenue() {
        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO seats (venue_id, row_label, seat_number)
                VALUES (1, 'A', 1)
                """))
            .isInstanceOf(DataIntegrityViolationException.class);

        Integer duplicates = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM seats
                WHERE venue_id = 1 AND row_label = 'A' AND seat_number = 1
                """, Integer.class);

        assertThat(duplicates).isEqualTo(1);
    }

    private int countRows(String tableName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Integer.class);
        return count == null ? 0 : count;
    }

    private boolean indexExists(String indexName) {
        String regclass = jdbcTemplate.queryForObject(
            "SELECT to_regclass(?)::text",
            String.class,
            "public." + indexName
        );
        return regclass != null;
    }
}
