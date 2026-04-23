package com.treserve.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.Instant;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class UserBookingResponse {
    private Long ticketId;
    private String eventTitle;
    private String seatLabel;
    private String status; // LOCKED or BOOKED
    private BigDecimal price;
    private Instant bookedAt; // для BOOKED, для LOCKED = null
}