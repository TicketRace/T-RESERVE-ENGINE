package com.treserve.event.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@Schema(description = "Ответ с информацией о мероприятии")
public class EventResponse {

    @Schema(description = "ID мероприятия", example = "1")
    private Long id;

    @Schema(description = "Название мероприятия", example = "Рок-фестиваль 2026")
    private String title;

    @Schema(description = "Описание мероприятия", example = "Крутой рок-фестиваль с участием звёзд")
    private String description;

    @Schema(description = "URL изображения", example = "https://example.com/poster.jpg")
    private String imageUrl;

    @Schema(description = "Возрастное ограничение", example = "16+")
    private String ageRestriction;

    @Schema(description = "Категория мероприятия", example = "CONCERT", allowableValues = {"CINEMA", "CONCERT", "SPORT", "THEATER"})
    private String category;

    @Schema(description = "Длительность в минутах", example = "120")
    private Integer durationMinutes;

    @Schema(description = "Дата и время начала", example = "2026-07-15T19:00:00Z")
    private Instant startTime;

    @Schema(description = "Базовая цена билета", example = "1500.00")
    private BigDecimal basePrice;

    @Schema(description = "Статус мероприятия", example = "ACTIVE", allowableValues = {"DRAFT", "ACTIVE", "CANCELLED", "COMPLETED"})
    private String status;

    @Schema(description = "ID площадки", example = "1")
    private Long venueId;

    @Schema(description = "Название площадки", example = "Октябрь — Зал 1")
    private String venueName;
}