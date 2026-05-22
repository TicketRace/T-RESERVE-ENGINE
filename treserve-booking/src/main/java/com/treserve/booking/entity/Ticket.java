package com.treserve.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Ticket entity — uses plain Long IDs instead of @ManyToOne references.
 * This decouples the booking module from event/user/venue modules,
 * making it ready for extraction into a standalone microservice.
 *
 * DB schema is unchanged: event_id, seat_id, user_id columns still
 * have FK constraints in PostgreSQL — data integrity is preserved at DB level.
 */
@Entity
@Table(name = "tickets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"event_id", "seat_id"})
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.AVAILABLE;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "lock_expires_at")
    private Instant lockExpiresAt;

    @Column(name = "booked_at")
    private Instant bookedAt;

    @Column(name = "pdf_url", length = 512)
    private String pdfUrl;
}
