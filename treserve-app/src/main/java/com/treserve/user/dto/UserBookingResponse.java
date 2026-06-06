package com.treserve.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Schema(description = "Билет пользователя в истории бронирований")
public class UserBookingResponse {

    @Schema(description = "ID билета", example = "100")
    private Long ticketId;

    @Schema(description = "ID мероприятия", example = "1")
    private Long eventId;

    @Schema(description = "Дата и время мероприятия", example = "2026-05-20T19:00:00Z")
    private Instant eventStartTime;

    @Schema(description = "Название мероприятия", example = "Рок-фестиваль")
    private String eventTitle;

    @Schema(description = "Номер места", example = "A-12")
    private String seatLabel;

    @Schema(description = "Статус бронирования", example = "BOOKED", allowableValues = {"LOCKED", "BOOKED"})
    private String status;

    @Schema(description = "Цена билета", example = "1500.00")
    private BigDecimal price;

    @Schema(description = "Дата подтверждения брони (для BOOKED)", example = "2026-04-22T10:00:00Z")
    private Instant bookedAt;
}