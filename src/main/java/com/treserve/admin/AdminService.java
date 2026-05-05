package com.treserve.admin;

import com.treserve.booking.TicketRepository;
import com.treserve.booking.TicketStatus;
import com.treserve.common.exception.ResourceNotFoundException;
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
import java.time.Instant;
import java.util.ArrayList;
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
                .orElseThrow(() -> new ResourceNotFoundException("Venue", request.getVenueId()));
        
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", adminId));

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

        List<Seat> seats = seatRepository.findByVenueId(venue.getId());
        List<com.treserve.booking.Ticket> tickets = new ArrayList<>();
        
        for (Seat seat : seats) {
            com.treserve.booking.Ticket ticket = com.treserve.booking.Ticket.builder()
                    .event(event)
                    .seat(seat)
                    .status(TicketStatus.AVAILABLE)
                    .price(request.getBasePrice())
                    .build();
            tickets.add(ticket);
        }
        
        ticketRepository.saveAll(tickets);

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
        // 1. Находим существующее событие
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        // 2. Проверяем, можно ли редактировать (актуальное время)
        Instant now = Instant.now();
        if (event.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Cannot edit event after sales started");
        }

        // 3. Обновляем только переданные поля
        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }
        if (request.getStartTime() != null) {
            event.setStartTime(request.getStartTime());
        }
        if (request.getBasePrice() != null) {
            event.setBasePrice(request.getBasePrice());
        }
        if (request.getImageUrl() != null) {
            event.setImageUrl(request.getImageUrl());
        }
        if (request.getAgeRestriction() != null) {
            event.setAgeRestriction(request.getAgeRestriction());
        }
        if (request.getCategory() != null) {
            event.setCategory(request.getCategory());
        }
        if (request.getDurationMinutes() != null) {
            event.setDurationMinutes(request.getDurationMinutes());
        }

        // 4. Сохраняем
        Event savedEvent = eventRepository.save(event);
        
        // 5. Возвращаем ответ
        return new EventResponse(
                savedEvent.getId(),
                savedEvent.getTitle(),
                savedEvent.getDescription(),
                savedEvent.getImageUrl(),
                savedEvent.getAgeRestriction(),
                savedEvent.getCategory(),
                savedEvent.getDurationMinutes(),
                savedEvent.getStartTime(),
                savedEvent.getBasePrice(),
                savedEvent.getStatus(),
                savedEvent.getVenue().getId(),
                savedEvent.getVenue().getName()
        );
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        
        if (eventRepository.hasBookedTickets(eventId)) {
            throw new IllegalArgumentException("Cannot delete event with BOOKED tickets");
        }
        
        ticketRepository.deleteAll(ticketRepository.findByEventIdWithSeat(eventId));
        eventRepository.deleteById(eventId);
    }
}