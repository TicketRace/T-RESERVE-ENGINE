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
    public UserInfo findById(Long id) {
        return userRepository.findById(id)
                .map(user -> new UserInfo(user.getId(), user.getEmail(), user.getName()))
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
    }

    @Override
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }
}
