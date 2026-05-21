package com.treserve.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LockRequest {

    @NotNull
    private Long eventId;

    @NotNull
    private Long seatId;
}
