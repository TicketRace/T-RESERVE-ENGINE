package com.treserve.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatInfo {
    private Long seatId;
    private String seatLabel;    // "A-12"
    private String rowLabel;     // "A"
    private Integer seatNumber;  // 12
    private String status;       // AVAILABLE | LOCKED | BOOKED
    private BigDecimal price;
}
