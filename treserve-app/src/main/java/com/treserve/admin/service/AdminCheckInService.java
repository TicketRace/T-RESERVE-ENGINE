package com.treserve.admin.service;

import com.treserve.admin.dto.CheckInResponse;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.common.exception.TicketAlreadyUsedException;
import com.treserve.event.entity.Event;
import com.treserve.event.repository.EventRepository;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import com.treserve.venue.entity.Seat;
import com.treserve.venue.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCheckInService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    @Transactional
    public CheckInResponse checkIn(UUID token) {
        Ticket ticket = ticketRepository.findByVerifyToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with token: " + token, 0L));
        return performCheckIn(ticket);
    }

    @Transactional
    public CheckInResponse checkInById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        return performCheckIn(ticket);
    }

    private CheckInResponse performCheckIn(Ticket ticket) {
        // Сохраняем предыдущий статус для ответа
        TicketStatus previousStatus = ticket.getStatus();
        
        if (ticket.getStatus() == TicketStatus.USED) {
            log.warn("Ticket {} already used", ticket.getId());
            throw new TicketAlreadyUsedException("Ticket already used");
        }

        if (ticket.getStatus() != TicketStatus.BOOKED) {
            throw new IllegalStateException("Ticket is not in BOOKED state (current: " + ticket.getStatus() + ")");
        }

        // Меняем статус на USED
        ticket.setStatus(TicketStatus.USED);
        ticketRepository.save(ticket);

        log.info("Ticket {} checked in successfully", ticket.getId());

        // === Собираем дополнительную информацию ===
        Event event = eventRepository.findById(ticket.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", ticket.getEventId()));
        
        Seat seat = seatRepository.findById(ticket.getSeatId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat", ticket.getSeatId()));
        
        User user = userRepository.findById(ticket.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", ticket.getUserId()));

        return CheckInResponse.builder()
                .message("Checked in successfully")
                .status(TicketStatus.USED.name())
                .ticketId(ticket.getId())
                .previousStatus(previousStatus.name())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventStartTime(event.getStartTime())
                .venueName(event.getVenue().getName())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .seatLabel(seat.getSeatLabel())
                .price(ticket.getPrice())
                .customerName(user.getName())
                .customerEmail(user.getEmail())
                .build();
    }
}