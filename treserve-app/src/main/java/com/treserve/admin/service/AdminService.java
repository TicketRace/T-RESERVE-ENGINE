package com.treserve.admin.service;

import com.treserve.admin.dto.mapper.AdminMapper;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.BusinessConflictException;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.event.dto.EventCreateRequest;
import com.treserve.event.dto.EventResponse;
import com.treserve.event.dto.EventUpdateRequest;
import com.treserve.event.entity.Event;
import com.treserve.event.repository.EventRepository;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import com.treserve.venue.entity.Seat;
import com.treserve.venue.entity.Venue;
import com.treserve.venue.repository.SeatRepository;
import com.treserve.venue.repository.VenueRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final AdminMapper adminMapper;

    public List<EventResponse> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(adminMapper::toEventResponse)
                .collect(Collectors.toList());
    }

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

        // Генерация билетов (tickets) для всех мест площадки
        List<Seat> seats = seatRepository.findByVenueId(venue.getId());
        List<com.treserve.booking.entity.Ticket> tickets = new ArrayList<>();
        
        for (Seat seat : seats) {
            com.treserve.booking.entity.Ticket ticket = com.treserve.booking.entity.Ticket.builder()
                    .eventId(event.getId())
                    .seatId(seat.getId())
                    .status(TicketStatus.AVAILABLE)
                    .price(request.getBasePrice())
                    .build();
            tickets.add(ticket);
        }
        
        ticketRepository.saveAll(tickets);

        // ← ИЗМЕНЕНО: используем маппер
        return adminMapper.toEventResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(Long eventId, EventUpdateRequest request, Long adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        // Проверка: можно редактировать только до начала продаж (startTime > now)
        if (event.getStartTime().isBefore(java.time.Instant.now())) {
            throw new IllegalArgumentException("Cannot edit event after sales started");
        }

        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getStatus() != null) event.setStatus(request.getStatus());
        if (request.getStartTime() != null) event.setStartTime(request.getStartTime());
        if (request.getBasePrice() != null) event.setBasePrice(request.getBasePrice());
        if (request.getImageUrl() != null) event.setImageUrl(request.getImageUrl());
        if (request.getAgeRestriction() != null) event.setAgeRestriction(request.getAgeRestriction());
        if (request.getCategory() != null) event.setCategory(request.getCategory());
        if (request.getDurationMinutes() != null) event.setDurationMinutes(request.getDurationMinutes());

        event = eventRepository.save(event);

        // ← ИЗМЕНЕНО: используем маппер
        return adminMapper.toEventResponse(event);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        
        if (eventRepository.hasBookedTickets(eventId)) {
            throw new BusinessConflictException("Cannot delete event with BOOKED tickets");
        }
        
        // Delete all tickets for this event (findSeatsByEventId returns projections, not entities
        // — use a simple JPA derived method instead)
        ticketRepository.deleteAll(ticketRepository.findByEventId(eventId));
        eventRepository.deleteById(eventId);
    }
}