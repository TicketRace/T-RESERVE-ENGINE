package com.treserve.config;

import com.treserve.booking.port.EventLookup;
import com.treserve.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaEventLookup implements EventLookup {

    private final EventRepository eventRepository;

    @Override
    public boolean existsById(Long eventId) {
        return eventRepository.existsById(eventId);
    }
}
