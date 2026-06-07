package com.treserve.auth.service;

import com.treserve.auth.dto.LoginRequest;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
