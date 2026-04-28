package com.treserve.admin;

import com.treserve.admin.dto.DashboardResponse;
import com.treserve.event.dto.EventCreateRequest;
import com.treserve.event.dto.EventResponse;
import com.treserve.event.dto.EventUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Управление мероприятиями (только ADMIN)")
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Создать мероприятие (генерация билетов)")
    public EventResponse createEvent(@Valid @RequestBody EventCreateRequest request,
                                     @AuthenticationPrincipal Long adminId) {
        return adminService.createEvent(request, adminId);
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "Редактировать мероприятие (до начала продаж)")
    public EventResponse updateEvent(@PathVariable Long id,
                                     @Valid @RequestBody EventUpdateRequest request,
                                     @AuthenticationPrincipal Long adminId) {
        return adminService.updateEvent(id, request, adminId);
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить мероприятие (каскадно билеты)")
    public void deleteEvent(@PathVariable Long id) {
        adminService.deleteEvent(id);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Дашборд: статистика (mock)")
    public DashboardResponse getDashboard() {
        // Mock данные, позже замените на реальные из БД
        return new DashboardResponse(42, 128, 12500.50);
    }
}