package com.treserve.ticket.dto;

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
@Schema(description = "Публичная информация о билете по QR-коду")
public class PublicTicketResponse {

    @Schema(description = "ID билета", example = "100")
    private Long ticketId;

    @Schema(description = "Статус билета", example = "BOOKED", allowableValues = {"BOOKED", "USED"})
    private String status;

    @Schema(description = "ID мероприятия", example = "1")
    private Long eventId;

    @Schema(description = "Название мероприятия", example = "Рок-фестиваль")
    private String eventTitle;

    @Schema(description = "Описание мероприятия", example = "Крутой рок-фестиваль")
    private String eventDescription;

    @Schema(description = "Дата и время начала", example = "2026-07-15T19:00:00Z")
    private Instant eventStartTime;

    @Schema(description = "Название площадки", example = "Октябрь — Зал 1")
    private String venueName;

    @Schema(description = "Адрес площадки", example = "Москва, ул. Тестовая, 1")
    private String venueAddress;

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

    @Deprecated
    @Schema(hidden = true)
    public String getEventTitleLegacy() {
        return eventTitle;
    }
}