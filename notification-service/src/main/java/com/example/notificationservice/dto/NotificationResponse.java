package com.example.notificationservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private Long userId;
    private Long orderId;
    private String eventType;
    private String channel;
    private String message;
    private String status;
    private Instant createdAt;
}
