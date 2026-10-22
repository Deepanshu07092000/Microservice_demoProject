package com.example.orderservice.messaging;

import com.example.orderservice.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * The ONLY class that talks to Kafka on the producing side.
 *
 * OrderServiceImpl does not call Kafka directly. It announces an OrderEvent inside Spring, and this class
 * sends it to Kafka only AFTER the database transaction has committed. That way we never publish an
 * event for an order that was rolled back and does not exist.
 *
 * Kafka problems must never break order creation: the order is already saved, so we only log the error.
 * (Production systems solve "saved but not published" with the Outbox pattern - out of scope here.)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Value("${app.kafka.topic.order-events}")
    private String topic;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publish(OrderEvent event) {
        // Key = orderId -> all events of the same order land in the same partition, in order.
        String key = String.valueOf(event.getOrderId());
        try {
            // send() is asynchronous: the result arrives later in whenComplete()
            kafkaTemplate.send(topic, key, event).whenComplete((result, error) -> {
                if (error != null) {
                    log.error("Could not publish {} for orderId={} to Kafka: {}",
                            event.getEventType(), event.getOrderId(), error.getMessage());
                } else {
                    log.info("Published {} for orderId={} -> topic={}, partition={}, offset={}",
                            event.getEventType(), event.getOrderId(), topic,
                            result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                }
            });
        } catch (Exception ex) {
            log.error("Could not publish {} for orderId={} to Kafka: {}",
                    event.getEventType(), event.getOrderId(), ex.getMessage());
        }
    }
}
