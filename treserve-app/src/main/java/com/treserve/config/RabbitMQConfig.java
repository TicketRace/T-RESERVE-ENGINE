package com.treserve.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TICKET_BOOKED_QUEUE = "ticket.booked.queue";
    public static final String TICKET_BOOKED_EXCHANGE = "ticket.booked.exchange";
    public static final String TICKET_BOOKED_ROUTING_KEY = "ticket.booked";
    
    public static final String TICKET_BOOKED_DLQ = "ticket.booked.dlq";
    public static final String TICKET_BOOKED_DLX = "ticket.booked.dlx";

    @Bean
    public Queue ticketBookedQueue() {
        return QueueBuilder.durable(TICKET_BOOKED_QUEUE)
                .withArgument("x-dead-letter-exchange", TICKET_BOOKED_DLX)
                .withArgument("x-dead-letter-routing-key", TICKET_BOOKED_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue ticketBookedDeadLetterQueue() {
        return QueueBuilder.durable(TICKET_BOOKED_DLQ).build();
    }

    @Bean
    public TopicExchange ticketBookedExchange() {
        return ExchangeBuilder.topicExchange(TICKET_BOOKED_EXCHANGE).durable(true).build();
    }

    @Bean
    public TopicExchange ticketBookedDeadLetterExchange() {
        return ExchangeBuilder.topicExchange(TICKET_BOOKED_DLX).durable(true).build();
    }

    @Bean
    public Binding ticketBookedBinding() {
        return BindingBuilder.bind(ticketBookedQueue())
                .to(ticketBookedExchange())
                .with(TICKET_BOOKED_ROUTING_KEY);
    }

    @Bean
    public Binding ticketBookedDeadLetterBinding() {
        return BindingBuilder.bind(ticketBookedDeadLetterQueue())
                .to(ticketBookedDeadLetterExchange())
                .with(TICKET_BOOKED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }
}