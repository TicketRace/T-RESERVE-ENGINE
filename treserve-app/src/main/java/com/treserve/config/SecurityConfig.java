package com.treserve.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final CookieOAuth2AuthorizationRequestRepository cookieAuthRepo;

    // Разрешенные CORS origins. Для продакшена (например, на Railway) переопределяется через переменную окружения CORS_ALLOWED_ORIGINS
    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:5173,http://localhost:3000,http://localhost,http://localhost:8081}")
    private String corsAllowedOrigins;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // STATELESS: OAuth2 flow использует CookieOAuth2AuthorizationRequestRepository для хранения state параметра в куках.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                    writeError(response, HttpStatus.UNAUTHORIZED, "Unauthorized"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    writeError(response, HttpStatus.FORBIDDEN, "Forbidden"))
            )
            .authorizeHttpRequests(auth -> auth
                // Public
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/**").permitAll()

                // Публичный просмотр билета по QR-коду (без авторизации)
                .requestMatchers(HttpMethod.GET, "/api/tickets/public").permitAll()

                // OAuth2 — Spring обрабатывает эти пути сам
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                // WebSocket endpoint — доступен без авторизации (STOMP handshake)
                .requestMatchers("/ws/**").permitAll()

                // Swagger / OpenAPI
                .requestMatchers(
                    "/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs", "/v3/api-docs/**",
                    "/api-docs", "/api-docs/**"
                ).permitAll()

                // Actuator — только health (healthcheck) и prometheus (scraping) публичны
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/prometheus", "/error", "/api/instance").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Admin
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Authenticated
                .anyRequest().authenticated()
            )
            // OAuth2 Login — Google redirect flow
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth
                    .authorizationRequestRepository(cookieAuthRepo)
                )
                .successHandler(oAuth2SuccessHandler)
                .failureHandler((req, res, ex) -> {
                    log.warn("OAuth2 login failed: {}", ex.getMessage());
                    res.sendRedirect(frontendUrl + "/login?error=oauth2");
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Читаем из env var — Railway задаёт CORS_ALLOWED_ORIGINS для прода
        List<String> origins = List.of(corsAllowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
            "timestamp", Instant.now().toString(),
            "status", status.value(),
            "error", message
        ));
    }
}