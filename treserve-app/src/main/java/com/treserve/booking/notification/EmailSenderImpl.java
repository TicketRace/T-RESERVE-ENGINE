package com.treserve.booking.notification;

import com.treserve.booking.notification.EmailSender;
import com.treserve.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailSenderImpl implements EmailSender {

    private final EmailService emailService;

    @Override
    public void sendTicketEmail(String to, String userName, byte[] pdfBytes, Long ticketId, String eventTitle) {
        emailService.sendTicketEmail(to, userName, pdfBytes, ticketId, eventTitle);
    }

}