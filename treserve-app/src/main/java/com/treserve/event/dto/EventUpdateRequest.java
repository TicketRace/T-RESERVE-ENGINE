package com.treserve.event.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class EventUpdateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotBlank
    private String status; // ACTIVE, DRAFT, CANCELLED

    private Instant startTime;

    private BigDecimal basePrice;

    private String imageUrl;
    private String ageRestriction;
    private String category;
    private Integer durationMinutes;
}