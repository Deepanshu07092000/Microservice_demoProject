package com.example.notificationservice.listener;

import com.example.notificationservice.event.OrderEvent;
import com.example.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * The entry point for Kafka messages (like a controller, but for Kafka instead of HTTP).
 *
 * The consumer group is "notification-group" (set in application.yml). Audit Service uses a DIFFERENT group,
 * so every order event is delivered to BOTH services. If two instances of this service run in the same group,
 * they share the work and each event goes to only one of them.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${app.kafka.topic.order-events}")
    public void onMessage(ConsumerRecord<String, OrderEvent> record) {
        log.info("Received from Kafka: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());
        notificationService.handleOrderEvent(record.value());
    }
}
