package com.treserve.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Дашборд администратора")
public class DashboardResponse {

    @Schema(description = "Список мероприятий со статистикой")
    private List<EventStatisticsResponse> events;

    @Schema(description = "Общее количество мероприятий", example = "5")
    private long totalEvents;

    @Schema(description = "Общее количество проданных билетов", example = "1250")
    private long totalSoldTickets;

    @Schema(description = "Общее количество использованных билетов", example = "450")
    private long totalUsedTickets;

    @Schema(description = "Общая выручка", example = "1875000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "Общий процент продаж", example = "65.5")
    private double overallSellThroughRate;
}