package com.treserve.booking.event;

import com.treserve.common.event.TicketBookedEvent;

/**
 * Продюсер событий о бронировании билета.
 * Реализацию (RabbitMQ) предоставляет модуль treserve-app.
 */
public interface TicketBookedEventProducer {
    
    /**
     * Отправить событие о подтверждении бронирования.
     * 
     * @param event событие бронирования
     */
    void sendTicketBookedEvent(TicketBookedEvent event);
}