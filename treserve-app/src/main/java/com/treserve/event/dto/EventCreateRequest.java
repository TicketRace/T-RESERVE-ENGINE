package com.treserve.event.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class EventCreateRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long venueId;

    @NotNull
    private Instant startTime;

    @NotNull
    @Positive
    private BigDecimal basePrice;

    private String imageUrl;
    private String ageRestriction;
    private String category;
    private Integer durationMinutes;
}