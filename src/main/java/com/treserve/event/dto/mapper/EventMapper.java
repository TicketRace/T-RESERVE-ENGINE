package com.treserve.event.dto.mapper;

import com.treserve.event.dto.EventResponse;
import com.treserve.event.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(source = "venue.id", target = "venueId")
    @Mapping(source = "venue.name", target = "venueName")
    EventResponse toResponse(Event entity);

    List<EventResponse> toResponseList(List<Event> entities);
}