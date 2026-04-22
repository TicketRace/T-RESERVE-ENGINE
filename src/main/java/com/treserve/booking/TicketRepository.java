package com.treserve.booking;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Все билеты на ивент (для карты мест) */
    @Query("""
        SELECT t FROM Ticket t
        JOIN FETCH t.seat s
        WHERE t.event.id = :eventId
        ORDER BY s.rowLabel, s.seatNumber
    """)
    List<Ticket> findByEventIdWithSeat(@Param("eventId") Long eventId);

    /**
     * SELECT FOR UPDATE NOWAIT — пессимистическая блокировка.
     * Если строка уже заблокирована другой транзакцией → PG бросает ошибку мгновенно.
     * Если status != AVAILABLE → вернёт пустой Optional.
     */
    @Query(value = """
        SELECT * FROM tickets
        WHERE event_id = :eventId AND seat_id = :seatId AND status = 'AVAILABLE'
        FOR UPDATE NOWAIT
    """, nativeQuery = true)
    Optional<Ticket> findAvailableForUpdate(
        @Param("eventId") Long eventId,
        @Param("seatId") Long seatId
    );

    /** Найти билет по ID с блокировкой строки */
    @Query(value = "SELECT * FROM tickets WHERE id = :id FOR UPDATE NOWAIT", nativeQuery = true)
    Optional<Ticket> findByIdForUpdate(@Param("id") Long id);

    /** Safety net: просроченные LOCKED билеты */
    @Query("SELECT t FROM Ticket t WHERE t.status = com.treserve.booking.TicketStatus.LOCKED AND t.lockExpiresAt < :now")
    List<Ticket> findExpiredLocks(@Param("now") Instant now);

    /** Билеты юзера */
    List<Ticket> findByUserIdAndStatus(Long userId, TicketStatus status);

    /** Найти все билеты пользователя с подгрузкой мероприятия и места.*/
    @Query("""
        SELECT t FROM Ticket t
        JOIN FETCH t.event e
        JOIN FETCH t.seat s
        WHERE t.user.id = :userId
        ORDER BY t.bookedAt DESC NULLS LAST, t.lockExpiresAt DESC
    """)
    List<Ticket> findByUserIdWithDetails(@Param("userId") Long userId);
}