package com.treserve.booking.event;

import com.treserve.booking.event.EventTitleProvider;
import com.treserve.event.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EventTitleProviderImpl implements EventTitleProvider {

    private final EventRepository eventRepository;

    @Override
    public String getEventTitle(Long eventId) {
        return eventRepository.findById(eventId)
                .map(event -> event.getTitle())
                .orElse("Мероприятие #" + eventId);
    }
}