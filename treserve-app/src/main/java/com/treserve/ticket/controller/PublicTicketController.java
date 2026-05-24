package com.treserve.ticket.controller;

import com.treserve.ticket.dto.PublicTicketResponse;
import com.treserve.ticket.service.PublicTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Публичные эндпоинты для билетов")
public class PublicTicketController {

    private final PublicTicketService publicTicketService;

    @GetMapping("/public")
    @Operation(summary = "Публичный просмотр билета по QR-коду (без авторизации)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Информация о билете получена"),
        @ApiResponse(responseCode = "404", description = "Билет с таким токеном не найден")
    })
    public PublicTicketResponse getTicketByToken(@RequestParam UUID token) {
        return publicTicketService.getTicketByToken(token);
    }
}