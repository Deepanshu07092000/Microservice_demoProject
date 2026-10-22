package com.example.orderservice.event;

import com.example.orderservice.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The MESSAGE we put on the Kafka topic "order-events" (it is converted to JSON automatically).
 *
 * An event says "something already happened" (an order WAS created), so it is a fact, not a command.
 * It carries everything the consumers need, so they never have to call Order Service back.
 *
 * Notification Service and Audit Service each have their own copy of this class.
 * They only share the JSON shape, never the Java class - that keeps the services independent.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    /** Unique id of this event. Consumers use it to ignore duplicates (Kafka can deliver a message twice). */
    private String eventId;

    private OrderEventType eventType;

    private Long orderId;
    private Long userId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private OrderStatus orderStatus;

    /** When it happened, written as ISO text like 2026-10-22T10:15:30Z. */
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}
