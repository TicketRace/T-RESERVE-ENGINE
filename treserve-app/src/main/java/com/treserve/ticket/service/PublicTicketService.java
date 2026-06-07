package com.treserve.ticket.service;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.event.entity.Event;
import com.treserve.event.repository.EventRepository;
import com.treserve.ticket.dto.PublicTicketResponse;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import com.treserve.venue.entity.Seat;
import com.treserve.venue.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicTicketService {

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;

    public PublicTicketResponse getTicketByToken(UUID token) {
        Ticket ticket = ticketRepository.findByVerifyToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with token: " + token, 0L));

        if (ticket.getStatus() != TicketStatus.BOOKED && ticket.getStatus() != TicketStatus.USED) {
            throw new IllegalStateException("Ticket is not available for verification");
        }

        // Получаем все данные
        Event event = eventRepository.findById(ticket.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", ticket.getEventId()));

        Seat seat = seatRepository.findById(ticket.getSeatId())
                .orElseThrow(() -> new ResourceNotFoundException("Seat", ticket.getSeatId()));

        User user = userRepository.findById(ticket.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", ticket.getUserId()));

        log.info("Public access to ticket {} via token {}", ticket.getId(), token);

        return PublicTicketResponse.builder()
                .ticketId(ticket.getId())
                .status(ticket.getStatus().name())
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .eventDescription(event.getDescription())
                .eventStartTime(event.getStartTime())
                .venueName(event.getVenue().getName())
                .venueAddress(event.getVenue().getAddress())
                .rowLabel(seat.getRowLabel())
                .seatNumber(seat.getSeatNumber())
                .seatLabel(seat.getSeatLabel())
                .price(ticket.getPrice())
                .customerName(user.getName())
                .build();
    }
}