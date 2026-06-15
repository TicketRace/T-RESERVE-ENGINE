package com.treserve.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketBookedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long ticketId;
    private Long userId;
    private String userEmail;
    private String userName;
    private String eventTitle;
    private Long eventId;
    private Long seatId;
    private String seatLabel;
    private BigDecimal price;
    private Instant bookedAt;
    private String verifyToken;
}