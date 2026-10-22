package com.example.auditservice.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * This service's OWN copy of the message that Order Service publishes (only the JSON shape is shared).
 * eventType and orderStatus are Strings on purpose, so a new value added by Order Service later cannot break us.
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
