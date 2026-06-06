package com.treserve.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ на проверку билета")
public class CheckInResponse {

    @Schema(description = "Сообщение о результате", example = "Checked in successfully")
    private String message;

    @Schema(description = "Статус билета после проверки", example = "USED")
    private String status;

    @Schema(description = "ID билета", example = "100")
    private Long ticketId;

    @Schema(description = "Статус билета до проверки", example = "BOOKED")
    private String previousStatus;

    @Schema(description = "ID мероприятия", example = "1")
    private Long eventId;

    @Schema(description = "Название мероприятия", example = "Рок-фестиваль")
    private String eventTitle;

    @Schema(description = "Дата и время начала мероприятия", example = "2026-07-15T19:00:00Z")
    private Instant eventStartTime;

    @Schema(description = "Название площадки", example = "Октябрь — Зал 1")
    private String venueName;

    @Schema(description = "Ряд", example = "A")
    private String rowLabel;

    @Schema(description = "Место", example = "12")
    private Integer seatNumber;

    @Schema(description = "Полное место", example = "A-12")
    private String seatLabel;

    @Schema(description = "Цена билета", example = "1500.00")
    private BigDecimal price;

    @Schema(description = "Имя покупателя", example = "Иван")
    private String customerName;

    @Schema(description = "Email покупателя", example = "ivan@example.com")
    private String customerEmail;
}