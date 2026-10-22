package com.example.notificationservice.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * This service's OWN copy of the message that Order Service publishes.
 * Only the JSON shape is shared, not the Java class. Fields we do not care about can simply be left out,
 * and unknown fields in the JSON are ignored. eventType and orderStatus are plain Strings on purpose:
 * if Order Service adds a new value tomorrow, this service will not break.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderEvent {
    private String eventId;
    private String eventType;      // ORDER_CREATED, ORDER_UPDATED or ORDER_DELETED
    private Long orderId;
    private Long userId;
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private String orderStatus;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant occurredAt;
}
