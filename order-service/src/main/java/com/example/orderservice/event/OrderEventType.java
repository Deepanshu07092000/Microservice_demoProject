package com.example.orderservice.event;

/** What happened to the order. Consumers use this to decide what to do (send message, write audit row...). */
public enum OrderEventType {
    ORDER_CREATED,
    ORDER_UPDATED,
    ORDER_DELETED
}
