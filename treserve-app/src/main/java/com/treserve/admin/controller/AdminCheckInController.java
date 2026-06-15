package com.treserve.admin.controller;

import com.treserve.admin.dto.CheckInRequest;
import com.treserve.admin.dto.CheckInResponse;
import com.treserve.admin.service.AdminCheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
@Tag(name = "Admin Check-in", description = "Управление входом на мероприятие (только ADMIN)")
public class AdminCheckInController {

    private final AdminCheckInService checkInService;

    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Отметить билет как использованный (check-in)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Билет успешно отмечен"),
        @ApiResponse(responseCode = "400", description = "Невалидный токен"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён (требуется роль ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Билет с таким токеном не найден"),
        @ApiResponse(responseCode = "409", description = "Билет уже был использован")
    })
    public CheckInResponse checkIn(@Valid @RequestBody CheckInRequest request) {
        return checkInService.checkIn(request.getToken());
    }

    @PostMapping("/check-in/by-id/{ticketId}")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Отметить билет как использованный по ID билета (альтернатива сканированию)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Билет успешно отмечен"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён (требуется роль ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Билет не найден"),
        @ApiResponse(responseCode = "409", description = "Билет уже был использован")
    })
    public CheckInResponse checkInById(@PathVariable Long ticketId) {
        return checkInService.checkInById(ticketId);
    }
}