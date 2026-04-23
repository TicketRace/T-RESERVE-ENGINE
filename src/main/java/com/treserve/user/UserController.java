package com.treserve.user;

import com.treserve.user.dto.UserBookingResponse;
import com.treserve.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
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
    public UserProfileResponse getProfile(@AuthenticationPrincipal Long userId) {
        return userService.getProfile(userId);
    }

    @GetMapping("/bookings")
    @Operation(summary = "История бронирований пользователя (LOCKED + BOOKED)")
    public List<UserBookingResponse> getMyBookings(@AuthenticationPrincipal Long userId) {
        return userService.getUserBookings(userId);
    }
}