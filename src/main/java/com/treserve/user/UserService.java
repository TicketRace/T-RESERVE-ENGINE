package com.treserve.user;

import com.treserve.booking.Ticket;
import com.treserve.booking.TicketRepository;
import com.treserve.user.dto.UserBookingResponse;
import com.treserve.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    public List<UserBookingResponse> getUserBookings(Long userId) {
        List<Ticket> tickets = ticketRepository.findByUserIdWithDetails(userId);
        return tickets.stream()
                .map(t -> new UserBookingResponse(
                        t.getId(),
                        t.getEvent().getTitle(),
                        t.getSeat().getSeatLabel(),
                        t.getStatus().name(),
                        t.getPrice(),
                        t.getBookedAt()
                ))
                .collect(Collectors.toList());
    }
}