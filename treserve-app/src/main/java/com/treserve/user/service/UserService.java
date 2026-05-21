package com.treserve.user.service;

import com.treserve.booking.repository.TicketDetail;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.user.dto.UserBookingResponse;
import com.treserve.user.dto.UserProfileResponse;
import com.treserve.user.dto.mapper.UserMapper;
import com.treserve.user.entity.User;
import com.treserve.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final UserMapper userMapper;  // ← ДОБАВЛЕНО

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return userMapper.toResponse(user);  // ← ИЗМЕНЕНО
    }

    public List<UserBookingResponse> getUserBookings(Long userId) {
        return ticketRepository.findTicketDetailsByUserId(userId).stream()
                .map(t -> new UserBookingResponse(
                        t.getId(),
                        t.getEventId(),
                        t.getEventStartTime(),
                        t.getEventTitle(),
                        t.getSeatLabel(),
                        t.getStatus(),
                        t.getPrice(),
                        t.getBookedAt()
                ))
                .collect(Collectors.toList());
    }
}