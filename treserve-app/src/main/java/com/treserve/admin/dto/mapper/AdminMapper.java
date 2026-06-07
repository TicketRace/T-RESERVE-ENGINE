package com.treserve.admin.dto.mapper;

import com.treserve.admin.dto.DashboardResponse;
import com.treserve.event.dto.EventResponse;
import com.treserve.event.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    @Mapping(source = "venue.id", target = "venueId")
    @Mapping(source = "venue.name", target = "venueName")
    EventResponse toEventResponse(Event event);

    List<EventResponse> toEventResponseList(List<Event> events);

    // DashboardResponse — это статические данные, маппер не нужен
}