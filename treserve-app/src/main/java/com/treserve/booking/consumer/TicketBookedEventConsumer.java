package com.treserve.booking.consumer;

import com.treserve.booking.entity.Ticket;
import com.treserve.booking.entity.TicketStatus;
import com.treserve.booking.repository.TicketRepository;
import com.treserve.common.event.TicketBookedEvent;
import com.treserve.notification.service.EmailService;
import com.treserve.storage.ResilientMinioStorageService;
import com.treserve.ticket.service.TicketPdfGenerator;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.treserve.config.RabbitMQConfig.TICKET_BOOKED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketBookedEventConsumer {

    private final TicketRepository ticketRepository;
    private final TicketPdfGenerator pdfGenerator;
    private final ResilientMinioStorageService minioStorageService;
    private final EmailService emailService;

    @RabbitListener(queues = TICKET_BOOKED_QUEUE)
    @CircuitBreaker(name = "email", fallbackMethod = "processTicketBookedFallback")
    @Transactional
    public void processTicketBooked(TicketBookedEvent event) {
        log.info("📨 Processing ticket booked event for ticket {}", event.getTicketId());

        // 1. Генерация PDF
        Ticket ticket = ticketRepository.findById(event.getTicketId())
                .orElseThrow(() -> new RuntimeException("Ticket not found: " + event.getTicketId()));
        
        byte[] pdfBytes = pdfGenerator.generatePdf(ticket);
        log.info("📄 PDF generated for ticket {}", event.getTicketId());

        // 2. Сохранение в MinIO (с Circuit Breaker)
        String objectKey = "tickets/" + event.getTicketId() + ".pdf";
        String result = minioStorageService.uploadToMinio(objectKey, pdfBytes, "application/pdf");
        
        // 3. Обновление pdf_url в БД
        if (result.startsWith("local://")) {
            log.warn("⚠️ PDF saved locally, not in MinIO for ticket {}", event.getTicketId());
        } else {
            ticketRepository.updatePdfUrlIfNull(event.getTicketId(), objectKey);
            log.info("💾 PDF URL updated in DB for ticket {}", event.getTicketId());
        }

        // 4. Отправка email (с Circuit Breaker)
        sendEmailWithCircuitBreaker(event, pdfBytes);
        
        log.info("✅ Ticket {} processing completed", event.getTicketId());
    }

    @CircuitBreaker(name = "email", fallbackMethod = "sendEmailFallback")
    public void sendEmailWithCircuitBreaker(TicketBookedEvent event, byte[] pdfBytes) {
        emailService.sendTicketEmail(
                event.getUserEmail(),
                event.getUserName(),
                pdfBytes,
                event.getTicketId(),
                event.getEventTitle()
        );
        log.info("📧 Email sent to {} for ticket {}", event.getUserEmail(), event.getTicketId());
    }

    @SuppressWarnings("unused")
    private void sendEmailFallback(TicketBookedEvent event, byte[] pdfBytes, Exception e) {
        log.error("❌ Circuit Breaker OPEN — email not sent for ticket {}: {}", event.getTicketId(), e.getMessage());
        // Можно сохранить в отдельную очередь для повторной отправки
    }

    @SuppressWarnings("unused")
    private void processTicketBookedFallback(TicketBookedEvent event, Exception e) {
        log.error("❌ Circuit Breaker OPEN — processing failed for ticket {}: {}", event.getTicketId(), e.getMessage());
        // Сохраняем в DLQ для повторной обработки позже
    }
}