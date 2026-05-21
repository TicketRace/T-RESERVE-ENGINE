package com.treserve.booking.service;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketAdminService {

    private final TicketRepository ticketRepository;

    @Transactional
    public void generateTicketsForEvent(Long eventId, BigDecimal basePrice, List<Long> seatIds) {
        List<Ticket> tickets = new ArrayList<>();
        for (Long seatId : seatIds) {
            Ticket ticket = Ticket.builder()
                    .eventId(eventId)
                    .seatId(seatId)
                    .status(TicketStatus.AVAILABLE)
                    .price(basePrice)
                    .build();
            tickets.add(ticket);
        }
        ticketRepository.saveAll(tickets);
    }

    @Transactional
    public void deleteTicketsForEvent(Long eventId) {
        ticketRepository.deleteByEventId(eventId);
    }
}
