package com.treserve.ticket.controller;

import com.treserve.ticket.service.TicketDownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Управление билетами")
public class TicketDownloadController {

    private final TicketDownloadService downloadService;

    @GetMapping("/{id}/download")
    @Operation(summary = "Скачать PDF билет")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF билет успешно сгенерирован"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "Билет не принадлежит пользователю"),
        @ApiResponse(responseCode = "404", description = "Билет не найден")
    })
    public ResponseEntity<byte[]> downloadTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        
        byte[] pdfBytes = downloadService.getOrCreateTicketPdf(id, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition
                .attachment()
                .filename("ticket-" + id + ".pdf")
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    @GetMapping("/{id}/qr")
    @Operation(summary = "Получить QR-код билета")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "QR-код успешно сгенерирован"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "Билет не принадлежит пользователю"),
        @ApiResponse(responseCode = "404", description = "Билет не найден")
    })
    public ResponseEntity<byte[]> downloadTicketQr(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {

        byte[] qrBytes = downloadService.getTicketQrCode(id, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return ResponseEntity.ok()
                .headers(headers)
                .body(qrBytes);
    }
}