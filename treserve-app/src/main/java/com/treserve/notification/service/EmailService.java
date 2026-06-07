package com.treserve.notification.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.subject}")
    private String subject;

    public void sendTicketEmail(String to, String userName, byte[] pdfBytes, Long ticketId, String eventTitle) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject + ": " + eventTitle);

            // === ИЗМЕНЕНИЕ: упрощённый HTML без скрытых блоков ===
            String htmlContent = buildSimpleEmailContent(userName, eventTitle, ticketId);
            helper.setText(htmlContent, true);

            if (pdfBytes != null && pdfBytes.length > 0 && pdfBytes.length > 10) {
                String filename = "ticket-" + ticketId + ".pdf";
                helper.addAttachment(filename, new ByteArrayResource(pdfBytes));
                log.info("PDF attachment added for ticket {}", ticketId);
            }

            mailSender.send(message);
            log.info("Ticket email sent to: {} for ticket: {}", to, ticketId);

        } catch (MessagingException e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send ticket email", e);
        }
    }

    // === ИЗМЕНЕНИЕ: HTML письмо — весь текст всегда виден ===
    private String buildSimpleEmailContent(String userName, String eventTitle, Long ticketId) {
        return "<!DOCTYPE html>" +
            "<html>" +
            "<head><meta charset='UTF-8'><title>Ваш билет</title></head>" +
            "<body style='font-family: Arial, sans-serif; background-color: #f4f4f4; margin: 0; padding: 20px;'>" +
            "<div style='max-width: 600px; margin: 0 auto; background-color: #ffffff; border-radius: 10px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.1);'>" +
            "<h1 style='color: #667eea; text-align: center;'>🎫 Ваш электронный билет</h1>" +
            "<hr style='border: none; border-top: 1px solid #eee;'>" +
            "<p style='font-size: 16px;'>Здравствуйте, <strong>" + userName + "</strong>!</p>" +
            "<p>Спасибо за покупку билета на мероприятие <strong>" + eventTitle + "</strong>.</p>" +
            "<p><strong>Номер билета:</strong> #" + ticketId + "</p>" +
            "<p>Ваш билет находится во вложении к этому письму.</p>" +
            "<p>Пожалуйста, сохраните этот билет и предъявите его на входе в отсканированном или распечатанном виде.</p>" +
            "<hr style='border: none; border-top: 1px solid #eee;'>" +
            "<p style='font-size: 12px; color: #888; text-align: center;'>T-RESERVE — надёжное бронирование билетов</p>" +
            "</div>" +
            "</body>" +
            "</html>";
    }
}