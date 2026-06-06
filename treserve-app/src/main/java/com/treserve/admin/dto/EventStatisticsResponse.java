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
@Schema(description = "Статистика по мероприятию для дашборда")
public class EventStatisticsResponse {

    @Schema(description = "ID мероприятия", example = "1")
    private Long eventId;

    @Schema(description = "Название мероприятия", example = "Рок-фестиваль 2026")
    private String title;

    @Schema(description = "Описание мероприятия", example = "Крутой рок-фестиваль")
    private String description;

    @Schema(description = "Дата и время начала", example = "2026-07-15T19:00:00Z")
    private Instant startTime;

    @Schema(description = "Название площадки", example = "Октябрь — Зал 1")
    private String venueName;

    @Schema(description = "Общее количество мест", example = "500")
    private long totalSeats;

    @Schema(description = "Количество проданных билетов (BOOKED)", example = "350")
    private long soldCount;

    @Schema(description = "Количество использованных билетов (USED)", example = "120")
    private long usedCount;

    @Schema(description = "Количество свободных мест (AVAILABLE)", example = "150")
    private long availableCount;

    @Schema(description = "Общая выручка по мероприятию", example = "525000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "Процент продаж", example = "70.0")
    private double sellThroughRate;
}