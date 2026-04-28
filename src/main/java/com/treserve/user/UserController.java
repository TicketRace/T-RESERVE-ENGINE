package com.treserve.user;

import com.treserve.user.dto.UserBookingResponse;
import com.treserve.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Профиль и бронирования пользователя")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Получить профиль текущего пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Профиль получен", content = @Content(schema = @Schema(implementation = UserProfileResponse.class))),
        @ApiResponse(responseCode = "401", description = "Неавторизован (требуется JWT токен)"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    public UserProfileResponse getProfile(@AuthenticationPrincipal Long userId) {
        return userService.getProfile(userId);
    }

    @GetMapping("/bookings")
    @Operation(summary = "История бронирований пользователя (LOCKED + BOOKED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список бронирований получен"),
        @ApiResponse(responseCode = "401", description = "Неавторизован (требуется JWT токен)")
    })
    public List<UserBookingResponse> getMyBookings(@AuthenticationPrincipal Long userId) {
        return userService.getUserBookings(userId);
    }
}