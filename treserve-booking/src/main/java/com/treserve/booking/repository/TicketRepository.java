package com.treserve.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /** Все билеты ивента — используется при удалении ивента */
    List<Ticket> findByEventId(Long eventId);

    /**
     * Карта мест для ивента — native SQL JOIN с таблицей seats.
     * Ticket.seatId теперь Long (не @ManyToOne), поэтому JOIN выполняется на уровне SQL.
     * Shared DB позволяет такой JOIN без нарушения микросервисной границы данных.
     */
    @Query(value = """
        SELECT t.seat_id    AS seatId,
               CONCAT(s.row_label, '-', s.seat_number) AS seatLabel,
               s.row_label  AS rowLabel,
               s.seat_number AS seatNumber,
               t.status,
               t.price
        FROM tickets t
        JOIN seats s ON t.seat_id = s.id
        WHERE t.event_id = :eventId
        ORDER BY s.row_label, s.seat_number
    """, nativeQuery = true)
    List<SeatInfoRow> findSeatsByEventId(@Param("eventId") Long eventId);

    /**
     * SELECT FOR UPDATE NOWAIT — пессимистическая блокировка.
     * Захватывает строку, если статус AVAILABLE или LOCKED с истекшим временем блокировки.
     * Если строка уже заблокирована другой транзакцией → PG бросает ошибку мгновенно.
     */
    @Query(value = """
        SELECT id, event_id, seat_id, status, price, user_id, lock_expires_at, booked_at, pdf_url, verify_token
        FROM tickets
        WHERE event_id = :eventId AND seat_id = :seatId 
          AND (status = 'AVAILABLE' OR (status = 'LOCKED' AND lock_expires_at < :now))
        FOR UPDATE NOWAIT
    """, nativeQuery = true)
    Optional<Ticket> findAvailableForUpdate(
        @Param("eventId") Long eventId,
        @Param("seatId") Long seatId,
        @Param("now") Instant now
    );

    /** Найти билет по ID с блокировкой строки */
    @Query(value = """
        SELECT id, event_id, seat_id, status, price, user_id, lock_expires_at, booked_at, pdf_url, verify_token
        FROM tickets WHERE id = :id FOR UPDATE NOWAIT
    """, nativeQuery = true)
    Optional<Ticket> findByIdForUpdate(@Param("id") Long id);
    Optional<Ticket> findByVerifyToken(UUID verifyToken);
    
    @Modifying
    @Query("UPDATE Ticket t SET t.pdfUrl = :pdfUrl WHERE t.id = :id AND t.pdfUrl IS NULL")
    int updatePdfUrlIfNull(@Param("id") Long id, @Param("pdfUrl") String pdfUrl);

    /** Bulk Delete всех билетов мероприятия */
    @Modifying
    @Query("DELETE FROM Ticket t WHERE t.eventId = :eventId")
    void deleteByEventId(@Param("eventId") Long eventId);

    /** Проверка наличия оплаченных билетов — один COUNT запрос, без загрузки в память */
    boolean existsByEventIdAndStatus(Long eventId, TicketStatus status);

    /**
     * Safety net: просроченные LOCKED билеты.
     * event_id доступен напрямую — JOIN FETCH больше не нужен.
     */
    @Query("""
        SELECT t FROM Ticket t
        WHERE t.status = com.treserve.booking.entity.TicketStatus.LOCKED
          AND t.lockExpiresAt < :now
    """)
    List<Ticket> findExpiredLocks(@Param("now") Instant now);

    /** Билеты юзера по статусу */
    List<Ticket> findByUserIdAndStatus(Long userId, TicketStatus status);

    @Query("SELECT e.title FROM Event e WHERE e.id = :eventId")
    String findEventTitleByEventId(@Param("eventId") Long eventId);

    /**
     * Билеты пользователя с деталями ивента и места (native SQL JOIN).
     * Возвращает проекцию TicketDetail без JPA @ManyToOne зависимостей.
     */
    @Query(value = """
        SELECT t.id,
               t.event_id        AS eventId,
               e.start_time      AS eventStartTime,
               e.title           AS eventTitle,
               t.seat_id         AS seatId,
               CONCAT(s.row_label, '-', s.seat_number) AS seatLabel,
               t.status,
               t.price,
               t.booked_at       AS bookedAt,
               t.lock_expires_at AS lockExpiresAt,
               t.pdf_url         AS pdfUrl
        FROM tickets t
        JOIN events e ON t.event_id = e.id
        JOIN seats  s ON t.seat_id  = s.id
        WHERE t.user_id = :userId
        ORDER BY t.booked_at DESC NULLS LAST, t.lock_expires_at DESC
    """, nativeQuery = true)
    List<TicketDetail> findTicketDetailsByUserId(@Param("userId") Long userId);
}