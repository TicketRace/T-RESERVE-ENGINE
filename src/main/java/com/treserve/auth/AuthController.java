package com.treserve.auth;

import com.treserve.auth.dto.AuthResponse;
import com.treserve.auth.dto.LoginRequest;
import com.treserve.auth.dto.RefreshRequest;
import com.treserve.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Регистрация, авторизация, refresh")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация → access + refresh токены")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Пользователь зарегистрирован", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Невалидные данные (email, пароль)"),
        @ApiResponse(responseCode = "409", description = "Пользователь с таким email уже существует")
    })
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Логин → access + refresh токены")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Успешный вход", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Неверный email или пароль")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Обновить access токен (передать refresh token)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Новая пара токенов", content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "400", description = "Невалидный refresh token"),
        @ApiResponse(responseCode = "401", description = "Refresh token истёк или неверен")
    })
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.getRefreshToken());
    }
}