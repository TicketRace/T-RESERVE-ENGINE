package com.treserve.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Статистика дашборда администратора")
public class DashboardResponse {

    @Schema(description = "Общее количество мероприятий", example = "42")
    private long totalEvents;

    @Schema(description = "Общее количество бронирований", example = "128")
    private long totalBookings;

    @Schema(description = "Общая выручка", example = "12500.50")
    private double revenue;
}