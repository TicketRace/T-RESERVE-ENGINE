package com.treserve.booking.notification;

public interface EmailSender {
    void sendTicketEmail(String to, String userName, byte[] pdfBytes, Long ticketId, String eventTitle);
}