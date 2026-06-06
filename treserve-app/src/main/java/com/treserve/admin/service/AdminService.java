package com.treserve.admin.service;

import com.treserve.admin.dto.DashboardResponse;
import com.treserve.admin.dto.EventStatisticsResponse;
import com.treserve.admin.dto.mapper.AdminMapper;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.service.BookingService;
import com.treserve.booking.service.TicketAdminService;
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
import java.util.ArrayList;
import java.util.List;
import com.treserve.booking.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final SeatRepository seatRepository;
    private final TicketAdminService ticketAdminService;
    private final BookingService bookingService;
    private final UserRepository userRepository;
    private final AdminMapper adminMapper;
    private final TicketRepository ticketRepository;

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

        // Генерация билетов (tickets) для всех мест площадки через TicketAdminService
        List<Seat> seats = seatRepository.findByVenueId(venue.getId());
        List<Long> seatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
        ticketAdminService.generateTicketsForEvent(event.getId(), request.getBasePrice(), seatIds);

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

        return adminMapper.toEventResponse(event);
    }

    @Transactional
    public void deleteEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));
        
        if (bookingService.hasBookedTickets(eventId)) {
            throw new BusinessConflictException("Cannot delete event with BOOKED tickets");
        }
        
        // Удаление всех билетов для мероприятия через TicketAdminService (быстрый Bulk Delete в БД)
        ticketAdminService.deleteTicketsForEvent(eventId);
        eventRepository.deleteById(eventId);
    }

        public DashboardResponse getDashboard(Long adminId) {
        // Получаем все мероприятия, созданные этим админом
        List<Event> events = eventRepository.findByCreatedById(adminId);
        
        List<EventStatisticsResponse> eventStats = new ArrayList<>();
        long totalSold = 0;
        long totalUsed = 0;
        BigDecimal totalRev = BigDecimal.ZERO;
        
        for (Event event : events) {
            // Статистика по мероприятию
            long soldCount = ticketRepository.countByEventIdAndStatus(event.getId(), TicketStatus.BOOKED);
            long usedCount = ticketRepository.countByEventIdAndStatus(event.getId(), TicketStatus.USED);
            long totalSeats = seatRepository.countByVenueId(event.getVenue().getId());
            long availableCount = totalSeats - (soldCount + usedCount);
            
            BigDecimal revenue = ticketRepository.sumPriceByEventIdAndStatus(event.getId(), TicketStatus.BOOKED);
            if (revenue == null) revenue = BigDecimal.ZERO;
            
            double sellThroughRate = totalSeats > 0 ? (double) (soldCount + usedCount) / totalSeats * 100 : 0;
            
            eventStats.add(EventStatisticsResponse.builder()
                    .eventId(event.getId())
                    .title(event.getTitle())
                    .description(event.getDescription())
                    .startTime(event.getStartTime())
                    .venueName(event.getVenue().getName())
                    .totalSeats(totalSeats)
                    .soldCount(soldCount)
                    .usedCount(usedCount)
                    .availableCount(availableCount)
                    .totalRevenue(revenue)
                    .sellThroughRate(Math.round(sellThroughRate * 10) / 10.0)
                    .build());
            
            totalSold += soldCount;
            totalUsed += usedCount;
            totalRev = totalRev.add(revenue);
        }
        
        long totalSeatsAll = eventStats.stream().mapToLong(EventStatisticsResponse::getTotalSeats).sum();
        double overallRate = totalSeatsAll > 0 ? (double) (totalSold + totalUsed) / totalSeatsAll * 100 : 0;
        
        return DashboardResponse.builder()
                .events(eventStats)
                .totalEvents(events.size())
                .totalSoldTickets(totalSold)
                .totalUsedTickets(totalUsed)
                .totalRevenue(totalRev)
                .overallSellThroughRate(Math.round(overallRate * 10) / 10.0)
                .build();
    }
}