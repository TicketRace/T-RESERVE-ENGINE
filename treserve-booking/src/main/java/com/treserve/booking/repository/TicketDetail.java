package com.treserve.booking.repository;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Spring Data проекция для native SQL запроса билетов пользователя с деталями.
 * Делает JOIN tickets + events + seats без зависимости от JPA @ManyToOne ассоциаций.
 */
public interface TicketDetail {
    Long getId();
    Long getEventId();
    Instant getEventStartTime();
    String getEventTitle();
    Long getSeatId();
    String getSeatLabel();
    String getStatus();
    BigDecimal getPrice();
    Instant getBookedAt();
    Instant getLockExpiresAt();
}
