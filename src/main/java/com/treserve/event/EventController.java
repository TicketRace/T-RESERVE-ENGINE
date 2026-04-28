package com.treserve.event;

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
    public Page<Event> list(@PageableDefault(size = 20) Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Детали мероприятия")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Мероприятие найдено", content = @Content(schema = @Schema(implementation = Event.class))),
        @ApiResponse(responseCode = "404", description = "Мероприятие не найдено")
    })
    public Event getById(@PathVariable Long id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new com.treserve.common.exception.ResourceNotFoundException("Event", id));
    }
}