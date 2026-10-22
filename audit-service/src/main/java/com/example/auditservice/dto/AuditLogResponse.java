package com.example.auditservice.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AuditLogResponse {
    private Long id;
    private String eventId;
    private String eventType;
    private Long orderId;
    private Long userId;
    private String payload;
    private Instant occurredAt;
    private Instant recordedAt;
    private Integer kafkaPartition;
    private Long kafkaOffset;
}
