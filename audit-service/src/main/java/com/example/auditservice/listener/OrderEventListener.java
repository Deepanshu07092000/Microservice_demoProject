package com.example.auditservice.listener;

import com.example.auditservice.event.OrderEvent;
import com.example.auditservice.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The entry point for Kafka messages (like a controller, but for Kafka instead of HTTP).
 *
 * The consumer group is "audit-group" (set in application.yml). Notification Service uses a DIFFERENT group,
 * so every order event is delivered to BOTH services, and each keeps its own progress (offset) in the topic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final AuditService auditService;

    @KafkaListener(topics = "${app.kafka.topic.order-events}")
    public void onMessage(ConsumerRecord<String, OrderEvent> record) {
        log.info("Received from Kafka: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        auditService.record(record.value(), record.partition(), record.offset());
    }
}
