package com.treserve.admin;

import com.treserve.admin.dto.DashboardResponse;
import com.treserve.event.dto.EventCreateRequest;
import com.treserve.event.dto.EventResponse;
import com.treserve.event.dto.EventUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Мероприятие создано", content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "400", description = "Невалидные данные запроса"),
        @ApiResponse(responseCode = "401", description = "Неавторизован (требуется JWT токен)"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён (требуется роль ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Площадка не найдена")
    })
    public EventResponse createEvent(@Valid @RequestBody EventCreateRequest request,
                                     @AuthenticationPrincipal Long adminId) {
        return adminService.createEvent(request, adminId);
    }

    @PutMapping("/events/{id}")
    @Operation(summary = "Редактировать мероприятие (до начала продаж)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Мероприятие обновлено", content = @Content(schema = @Schema(implementation = EventResponse.class))),
        @ApiResponse(responseCode = "400", description = "Невалидные данные или мероприятие уже началось"),
        @ApiResponse(responseCode = "401", description = "Неавторизован (требуется JWT токен)"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён (требуется роль ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Мероприятие не найдено")
    })
    public EventResponse updateEvent(@PathVariable Long id,
                                     @Valid @RequestBody EventUpdateRequest request,
                                     @AuthenticationPrincipal Long adminId) {
        return adminService.updateEvent(id, request, adminId);
    }

    @DeleteMapping("/events/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Удалить мероприятие (каскадно билеты)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Мероприятие удалено"),
        @ApiResponse(responseCode = "401", description = "Неавторизован (требуется JWT токен)"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён (требуется роль ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Мероприятие не найдено"),
        @ApiResponse(responseCode = "409", description = "Нельзя удалить мероприятие с оплаченными билетами")
    })
    public void deleteEvent(@PathVariable Long id) {
        adminService.deleteEvent(id);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Дашборд: статистика (mock)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Статистика получена", content = @Content(schema = @Schema(implementation = DashboardResponse.class))),
        @ApiResponse(responseCode = "401", description = "Неавторизован (требуется JWT токен)"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён (требуется роль ADMIN)")
    })
    public DashboardResponse getDashboard() {
        // Mock данные, позже замените на реальные из БД
        return new DashboardResponse(42, 128, 12500.50);
    }
}