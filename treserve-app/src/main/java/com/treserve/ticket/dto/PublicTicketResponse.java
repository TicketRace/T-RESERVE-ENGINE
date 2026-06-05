package com.treserve.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
@Schema(description = "Публичная информация о билете по QR-коду")
public class PublicTicketResponse {

    @Schema(description = "ID билета", example = "100")
    private Long ticketId;

    @Schema(description = "Название мероприятия", example = "Рок-фестиваль")
    private String eventTitle;

    @Schema(description = "Номер места", example = "A-12")
    private String seatLabel;

    @Schema(description = "Статус билета", example = "BOOKED", allowableValues = {"BOOKED", "USED"})
    private String status;

    @Schema(description = "Время начала мероприятия", example = "2026-07-15T19:00:00Z")
    private Instant eventStartTime;
}