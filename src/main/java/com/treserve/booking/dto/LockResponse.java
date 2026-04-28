package com.treserve.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public class LockResponse {
    private Long lockId;
    private Instant expiresAt;
}
