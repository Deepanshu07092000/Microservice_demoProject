package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.event.OrderEvent;

import java.util.List;

public interface NotificationService {

    /** Called by the Kafka listener for every order event. */
    void handleOrderEvent(OrderEvent event);

    List<NotificationResponse> getAll();

    List<NotificationResponse> getByUserId(Long userId);
}
