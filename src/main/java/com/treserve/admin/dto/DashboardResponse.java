package com.treserve.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardResponse {
    private long totalEvents;
    private long totalBookings;
    private double revenue; // mock
}