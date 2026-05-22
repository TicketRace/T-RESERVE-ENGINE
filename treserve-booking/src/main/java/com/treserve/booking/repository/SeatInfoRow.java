package com.treserve.booking.repository;

import java.math.BigDecimal;

/**
 * Spring Data проекция для native SQL запроса карты мест.
 * Используется в SeatService, чтобы не создавать зависимость на entity Seat.
 */
public interface SeatInfoRow {
    Long getSeatId();
    String getSeatLabel();
    String getRowLabel();
    Integer getSeatNumber();
    String getStatus();
    BigDecimal getPrice();
}
