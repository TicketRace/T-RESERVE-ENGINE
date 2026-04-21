package com.treserve.auth;

import com.treserve.auth.dto.AuthResponse;
import com.treserve.auth.dto.LoginRequest;
import com.treserve.auth.dto.RegisterRequest;
import com.treserve.user.User;
import com.treserve.user.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .name(request.getName())
            .role("USER")
            .build();

        user = userRepository.save(user);
        return buildResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return buildResponse(user);
    }

    /**
     * Refresh: принимает refresh token → выдаёт новую пару access + refresh.
     * Старый refresh token после этого всё ещё валиден до истечения.
     * Для полной ротации нужен blacklist (v2).
     */
    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwtService.parseToken(refreshToken);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        // Проверяем что это именно refresh, а не access
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new IllegalArgumentException("Invalid token type: expected refresh token");
        }

        Long userId = Long.parseLong(claims.getSubject());
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId());
        return new AuthResponse(
            accessToken,
            refreshToken,
            new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getName(), user.getRole())
        );
    }
}
