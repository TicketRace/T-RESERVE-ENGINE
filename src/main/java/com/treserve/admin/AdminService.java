package com.treserve.admin;

import com.treserve.booking.TicketRepository;
import com.treserve.booking.TicketStatus;
import com.treserve.event.Event;
import com.treserve.event.EventRepository;
import com.treserve.event.dto.EventCreateRequest;
import com.treserve.event.dto.EventResponse;
import com.treserve.event.dto.EventUpdateRequest;
import com.treserve.user.User;
import com.treserve.user.UserRepository;
import com.treserve.venue.Seat;
import com.treserve.venue.SeatRepository;
import com.treserve.venue.Venue;
import com.treserve.venue.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    @Transactional
    public EventResponse createEvent(EventCreateRequest request, Long adminId) {
        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new RuntimeException("Venue not found"));
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .venue(venue)
                .startTime(request.getStartTime())
                .basePrice(request.getBasePrice())
                .imageUrl(request.getImageUrl())
                .ageRestriction(request.getAgeRestriction())
                .category(request.getCategory())
                .durationMinutes(request.getDurationMinutes())
                .status("ACTIVE")
                .createdBy(admin)
                .build();

        event = eventRepository.save(event);

        // Генерация билетов (tickets) для всех мест площадки
        List<Seat> seats = seatRepository.findByVenueId(venue.getId());
        for (Seat seat : seats) {
            com.treserve.booking.Ticket ticket = com.treserve.booking.Ticket.builder()
                    .event(event)
                    .seat(seat)
                    .status(com.treserve.booking.TicketStatus.AVAILABLE)
                    .price(request.getBasePrice())
                    .build();
            ticketRepository.save(ticket);
        }

        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getImageUrl(),
                event.getAgeRestriction(),
                event.getCategory(),
                event.getDurationMinutes(),
                event.getStartTime(),
                event.getBasePrice(),
                event.getStatus(),
                venue.getId(),
                venue.getName()
        );
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventUpdateRequest request, Long adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Проверка: можно редактировать только до начала продаж (startTime > now)
        if (event.getStartTime().isBefore(java.time.Instant.now())) {
            throw new RuntimeException("Cannot edit event after sales started");
        }

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setStatus(request.getStatus());
        if (request.getStartTime() != null) event.setStartTime(request.getStartTime());
        if (request.getBasePrice() != null) event.setBasePrice(request.getBasePrice());
        if (request.getImageUrl() != null) event.setImageUrl(request.getImageUrl());
        if (request.getAgeRestriction() != null) event.setAgeRestriction(request.getAgeRestriction());
        if (request.getCategory() != null) event.setCategory(request.getCategory());
        if (request.getDurationMinutes() != null) event.setDurationMinutes(request.getDurationMinutes());

        event = eventRepository.save(event);

        Venue venue = event.getVenue();
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getImageUrl(),
                event.getAgeRestriction(),
                event.getCategory(),
                event.getDurationMinutes(),
                event.getStartTime(),
                event.getBasePrice(),
                event.getStatus(),
                venue.getId(),
                venue.getName()
        );
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new RuntimeException("Event not found");
        }
        if (eventRepository.hasBookedTickets(eventId)) {
            throw new RuntimeException("Cannot delete event with booked tickets");
        }
        ticketRepository.deleteAll(ticketRepository.findByEventIdWithSeat(eventId));
        eventRepository.deleteById(eventId);
    }
}