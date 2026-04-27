package com.treserve.booking;

import com.treserve.booking.dto.LockRequest;
import com.treserve.booking.dto.LockResponse;
import com.treserve.booking.dto.SeatInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Ядро — бронирование мест")
public class BookingController {

    private final SeatService seatService;
    private final BookingService bookingService;

    @GetMapping("/api/events/{eventId}/seats")
    @Tag(name = "Events")
    @Operation(summary = "Карта мест: все места + их статусы (polling каждые 3 сек, Redis cached)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список мест с текущими статусами",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = SeatInfo.class)))),
        @ApiResponse(responseCode = "404", description = "Мероприятие не найдено")
    })
    public List<SeatInfo> getSeats(@PathVariable Long eventId) {
        return seatService.getSeats(eventId);
    }

    @PostMapping("/api/bookings/lock")
    @Operation(summary = "Заблокировать место (PG SELECT FOR UPDATE NOWAIT) → 200 OK / 409 Conflict")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Место заблокировано, возвращает lockId и время истечения",
            content = @Content(schema = @Schema(implementation = LockResponse.class))),
        @ApiResponse(responseCode = "400", description = "Невалидный запрос"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "409", description = "Место уже занято (race condition — другой пользователь успел первым)")
    })
    public LockResponse lock(
        @Valid @RequestBody LockRequest request,
        @AuthenticationPrincipal Long userId
    ) {
        return bookingService.tryLock(request.getEventId(), request.getSeatId(), userId);
    }

    @PostMapping("/api/bookings/{ticketId}/confirm")
    @Operation(summary = "Подтвердить бронирование (mock оплата)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Бронирование подтверждено (LOCKED → BOOKED)"),
        @ApiResponse(responseCode = "400", description = "Лок истёк или билет не в статусе LOCKED"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "Попытка подтвердить чужое бронирование"),
        @ApiResponse(responseCode = "404", description = "Билет не найден")
    })
    public String confirm(
        @PathVariable Long ticketId,
        @AuthenticationPrincipal Long userId
    ) {
        bookingService.confirm(ticketId, userId);
        return "Booking confirmed";
    }

    @DeleteMapping("/api/bookings/{ticketId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Ручная отмена блокировки")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Блокировка снята (LOCKED → AVAILABLE)"),
        @ApiResponse(responseCode = "400", description = "Билет не в статусе LOCKED"),
        @ApiResponse(responseCode = "401", description = "Неавторизован"),
        @ApiResponse(responseCode = "403", description = "Попытка отменить чужую блокировку"),
        @ApiResponse(responseCode = "404", description = "Билет не найден")
    })
    public void cancel(
        @PathVariable Long ticketId,
        @AuthenticationPrincipal Long userId
    ) {
        bookingService.cancel(ticketId, userId);
    }
}
