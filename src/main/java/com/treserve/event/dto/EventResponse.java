package com.treserve.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String ageRestriction;
    private String category;
    private Integer durationMinutes;
    private Instant startTime;
    private BigDecimal basePrice;
    private String status;
    private Long venueId;
    private String venueName;
}