package com.treserve.booking;

import com.treserve.booking.dto.LockRequest;
import com.treserve.booking.dto.LockResponse;
import com.treserve.booking.dto.SeatInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@Tag(name = "Booking", description = "Ядро — бронирование мест")
public class BookingController {

    private final TicketRepository ticketRepository;
    private final BookingService bookingService;

    @GetMapping("/api/events/{eventId}/seats")
    @Tag(name = "Events")
    @Operation(summary = "Карта мест: все места + их статусы (polling каждые 3 сек)")
    public List<SeatInfo> getSeats(@PathVariable Long eventId) {
        return ticketRepository.findByEventIdWithSeat(eventId).stream()
            .map(t -> new SeatInfo(
                t.getSeat().getId(),
                t.getSeat().getSeatLabel(),
                t.getSeat().getRowLabel(),
                t.getSeat().getSeatNumber(),
                t.getStatus().name(),
                t.getPrice()
            ))
            .collect(Collectors.toList());
    }

    @PostMapping("/api/bookings/lock")
    @Operation(summary = "Заблокировать место (PG SELECT FOR UPDATE NOWAIT) → 200 OK / 409 Conflict")
    public LockResponse lock(
        @Valid @RequestBody LockRequest request,
        @AuthenticationPrincipal Long userId
    ) {
        return bookingService.tryLock(request.getEventId(), request.getSeatId(), userId);
    }

    @PostMapping("/api/bookings/{ticketId}/confirm")
    @Operation(summary = "Подтвердить бронирование (mock оплата)")
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
    public void cancel(
        @PathVariable Long ticketId,
        @AuthenticationPrincipal Long userId
    ) {
        bookingService.cancel(ticketId, userId);
    }
}
