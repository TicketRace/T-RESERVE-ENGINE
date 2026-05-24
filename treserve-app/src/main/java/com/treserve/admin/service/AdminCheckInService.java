package com.treserve.admin.service;

import com.treserve.admin.dto.CheckInResponse;
import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.exception.ResourceNotFoundException;
import com.treserve.common.exception.SeatAlreadyLockedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminCheckInService {

    private final TicketRepository ticketRepository;

    @Transactional
    public CheckInResponse checkIn(UUID token) {
        Ticket ticket = ticketRepository.findByVerifyToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket with token: " + token, 0L));

        return performCheckIn(ticket);
    }

    @Transactional
    public CheckInResponse checkInById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));

        return performCheckIn(ticket);
    }

    private CheckInResponse performCheckIn(Ticket ticket) {
        // Проверяем, не использован ли уже билет — используем SeatAlreadyLockedException для 409
        if (ticket.getStatus() == TicketStatus.USED) {
            log.warn("Ticket {} already used", ticket.getId());
            throw new SeatAlreadyLockedException("Ticket already used");  // ← будет 409 Conflict
        }

        // Только BOOKED билеты можно отметить
        if (ticket.getStatus() != TicketStatus.BOOKED) {
            throw new IllegalStateException("Ticket is not in BOOKED state (current: " + ticket.getStatus() + ")");
        }

        // Меняем статус на USED
        ticket.setStatus(TicketStatus.USED);
        ticketRepository.save(ticket);

        log.info("Ticket {} checked in successfully", ticket.getId());

        return new CheckInResponse("Checked in successfully", TicketStatus.USED.name());
    }
}