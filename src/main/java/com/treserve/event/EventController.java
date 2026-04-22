package com.treserve.event;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Мероприятия")
public class EventController {

    private final EventRepository eventRepository;

    @GetMapping
    @Operation(summary = "Список мероприятий (пагинация)")
    public Page<Event> list(@PageableDefault(size = 20) Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Детали мероприятия")
    public Event getById(@PathVariable Long id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new com.treserve.common.exception.ResourceNotFoundException("Event", id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Создать мероприятие (ADMIN)")
    public Event create(@RequestBody Event event) {
        return eventRepository.save(event);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить мероприятие (ADMIN)")
    public void delete(@PathVariable Long id) {
        eventRepository.deleteById(id);
    }
}
