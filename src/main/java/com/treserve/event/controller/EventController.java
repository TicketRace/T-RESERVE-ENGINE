package com.treserve.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import com.treserve.event.entity.Event;
import com.treserve.event.repository.EventRepository;
import com.treserve.common.exception.ResourceNotFoundException;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Мероприятия")
public class EventController {

    private final EventRepository eventRepository;

    @GetMapping
    @Operation(summary = "Список мероприятий (пагинация)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список мероприятий получен"),
        @ApiResponse(responseCode = "400", description = "Неверные параметры пагинации")
    })
    public Page<Event> list(
        @RequestParam(required = false, defaultValue = "") String search,
        @RequestParam(required = false, defaultValue = "") String category,
        @PageableDefault(size = 20) Pageable pageable) {
        return eventRepository.findActiveEvents(search, category, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Детали мероприятия")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Мероприятие найдено", content = @Content(schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "404", description = "Мероприятие не найдено")
    })
    public Event getById(@PathVariable Long id) {
        return eventRepository.findByIdWithVenue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }
}