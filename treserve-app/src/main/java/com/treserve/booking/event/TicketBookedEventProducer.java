package com.treserve.booking.producer;

import com.treserve.common.event.TicketBookedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static com.treserve.config.RabbitMQConfig.TICKET_BOOKED_EXCHANGE;
import static com.treserve.config.RabbitMQConfig.TICKET_BOOKED_ROUTING_KEY;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketBookedEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendTicketBookedEvent(TicketBookedEvent event) {
        try {
            rabbitTemplate.convertAndSend(TICKET_BOOKED_EXCHANGE, TICKET_BOOKED_ROUTING_KEY, event);
            log.info("✅ Sent ticket booked event to RabbitMQ for ticket {}", event.getTicketId());
        } catch (Exception e) {
            log.error("❌ Failed to send ticket booked event for ticket {}: {}", event.getTicketId(), e.getMessage());
        }
    }
}