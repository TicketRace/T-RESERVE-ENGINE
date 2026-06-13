package com.treserve.config;

import com.treserve.booking.port.EventLookup;
import com.treserve.event.entity.Event;
import com.treserve.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JpaEventLookup implements EventLookup {

    private final EventRepository eventRepository;

    @Override
    public String getEventTitle(Long eventId) {
        log.debug("Looking up event title for eventId: {}", eventId);
        
        return eventRepository.findById(eventId)
                .map(Event::getTitle)
                .orElseGet(() -> {
                    log.warn("Event not found for id: {}", eventId);
                    return "Unknown Event";
                });
    }

    @Override
    public boolean existsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }
}