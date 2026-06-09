package com.treserve.auth.service;

import com.treserve.auth.dto.AuthResponse;
import com.treserve.auth.dto.LoginRequest;
import com.treserve.auth.dto.RegisterRequest;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_whenEmailIsFree_savesLocalUserAndReturnsTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new.user@example.com");
        request.setPassword("secret123");
        request.setName("New User");

        when(userRepository.existsByEmail("new.user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return user;
        });
        when(jwtService.generateAccessToken(42L, "new.user@example.com", "USER")).thenReturn("access-token");
        when(jwtService.generateRefreshToken(42L)).thenReturn("refresh-token");

        AuthResponse response = authService.register(request);

        assertEquals("access-token", response.getToken());
        assertEquals("refresh-token", response.getRefreshToken());
        assertEquals(42L, response.getUser().getId());
        assertEquals("new.user@example.com", response.getUser().getEmail());
        assertEquals("New User", response.getUser().getName());
        assertEquals("USER", response.getUser().getRole());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedUser = captor.getValue();
        assertEquals("new.user@example.com", savedUser.getEmail());
        assertEquals("encoded-password", savedUser.getPasswordHash());
        assertEquals("New User", savedUser.getName());
        assertEquals("USER", savedUser.getRole());
    }

    @Test
    void register_whenEmailAlreadyExists_throwsExceptionAndDoesNotSave() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("taken@example.com");
        request.setPassword("secret123");
        request.setName("Taken");
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.register(request));

        assertEquals("Email already registered: taken@example.com", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_whenUserIsGoogleOnly_throwsException() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setEmail("google.user@example.com");
        request.setPassword("anyPassword");

        User googleUser = User.builder()
                .id(1L)
                .email("google.user@example.com")
                .passwordHash(null) // No password
                .authProvider("GOOGLE")
                .build();

        when(userRepository.findByEmail("google.user@example.com")).thenReturn(Optional.of(googleUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(request);
        });

        assertEquals("This account uses Google Sign-In. Please use the Google login button.", exception.getMessage());
    }

    @Test
    void login_whenPasswordDoesNotMatch_throwsGenericInvalidCredentialsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("local.user@example.com");
        request.setPassword("wrong-password");
        User localUser = User.builder()
                .id(7L)
                .email("local.user@example.com")
                .passwordHash("encoded-password")
                .authProvider("LOCAL")
                .build();
        when(userRepository.findByEmail("local.user@example.com")).thenReturn(Optional.of(localUser));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(jwtService, never()).generateAccessToken(anyLong(), any(), any());
    }

    @Test
    void refresh_whenTokenCannotBeParsed_throwsInvalidOrExpiredRefreshToken() {
        when(jwtService.parseToken("bad-token")).thenThrow(new RuntimeException("expired"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.refresh("bad-token"));

        assertEquals("Invalid or expired refresh token", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void refresh_whenAccessTokenProvided_throwsInvalidTokenType() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        when(jwtService.parseToken("access-token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("access");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> authService.refresh("access-token"));

        assertEquals("Invalid token type: expected refresh token", exception.getMessage());
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void refresh_whenRefreshTokenIsValid_returnsNewTokenPairForSubjectUser() {
        Claims claims = org.mockito.Mockito.mock(Claims.class);
        User user = User.builder()
                .id(7L)
                .email("local.user@example.com")
                .name("Local User")
                .role("USER")
                .build();
        when(jwtService.parseToken("refresh-token")).thenReturn(claims);
        when(claims.get("type", String.class)).thenReturn("refresh");
        when(claims.getSubject()).thenReturn("7");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(7L, "local.user@example.com", "USER")).thenReturn("new-access");
        when(jwtService.generateRefreshToken(7L)).thenReturn("new-refresh");

        AuthResponse response = authService.refresh("refresh-token");

        assertEquals("new-access", response.getToken());
        assertEquals("new-refresh", response.getRefreshToken());
        assertEquals(7L, response.getUser().getId());
        assertEquals("local.user@example.com", response.getUser().getEmail());
        assertEquals("Local User", response.getUser().getName());
        assertEquals("USER", response.getUser().getRole());
    }
}
