package com.treserve.config;

import com.treserve.auth.service.JwtService;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Вызывается Spring после успешной OAuth2 авторизации через Google.
 * Создаёт или обновляет пользователя в БД, генерирует JWT и
 * редиректит на фронтенд с токенами в query params.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email      = oAuth2User.getAttribute("email");
        String name       = oAuth2User.getAttribute("name");
        String picture    = oAuth2User.getAttribute("picture");
        String providerId = oAuth2User.getAttribute("sub"); // Google user ID

        if (email == null) {
            log.error("Google OAuth2: email is null — user denied email scope");
            getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?error=oauth2_no_email");
            return;
        }

        // Ищем существующего пользователя или создаём нового
        User user = userRepository.findByEmail(email)
            .map(existing -> {
                // Слияние аккаунтов — обновляем Google-поля
                existing.setAuthProvider("GOOGLE");
                existing.setProviderId(providerId);
                if (picture != null && existing.getAvatarUrl() == null) {
                    existing.setAvatarUrl(picture);
                }
                return userRepository.save(existing);
            })
            .orElseGet(() -> {
                log.info("Creating new user from Google OAuth2: {}", email);
                return userRepository.save(User.builder()
                    .email(email)
                    .name(name != null ? name : email.split("@")[0])
                    .role("USER")
                    .authProvider("GOOGLE")
                    .providerId(providerId)
                    .avatarUrl(picture)
                    .build());
            });

        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        String redirectUrl = frontendUrl + "/oauth2/callback"
            + "?token=" + accessToken
            + "&refreshToken=" + refreshToken;

        log.info("OAuth2 success for user: {} ({})", user.getEmail(), user.getAuthProvider());
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
