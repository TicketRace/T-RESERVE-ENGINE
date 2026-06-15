package com.treserve.admin.controller;

import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitoring")
@PreAuthorize("hasRole('ADMIN')")
public class RabbitMQMonitoringController {

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @GetMapping("/queues")
    public ResponseEntity<Map<String, Object>> getQueueStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            var queueInfo = rabbitAdmin.getQueueProperties("ticket.booked.queue");
            if (queueInfo != null) {
                stats.put("queueName", "ticket.booked.queue");
                stats.put("messageCount", queueInfo.get("QUEUE_MESSAGE_COUNT"));
                stats.put("consumerCount", queueInfo.get("QUEUE_CONSUMER_COUNT"));
            }
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(stats);
    }
}