package com.treserve.config;

import com.treserve.booking.port.UserLookup;
import com.treserve.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Реализует порт UserLookup (определён в treserve-booking).
 * Живёт в treserve-app, потому что зависит от UserRepository (JPA).
 *
 * Когда booking будет вынесен в полноценный микросервис,
 * этот адаптер станет HTTP-клиентом к user-service.
 */
@Component
@RequiredArgsConstructor
public class JpaUserLookup implements UserLookup {

    private final UserRepository userRepository;

    @Override
    public boolean existsById(Long userId) {
        return userRepository.existsById(userId);
    }
}
