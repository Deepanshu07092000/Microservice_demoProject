package com.example.auditservice.service;

import com.example.auditservice.dto.AuditLogResponse;
import com.example.auditservice.entity.AuditLog;
import com.example.auditservice.event.OrderEvent;
import com.example.auditservice.repository.AuditLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Business logic: store every order event as one audit row. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;   // provided by Spring Boot, used to turn the event back into JSON text

    @Override
    @Transactional
    public void record(OrderEvent event, int partition, long offset) {
        if (event == null || event.getEventId() == null) {
            log.warn("Ignoring an order event without an eventId");
            return;
        }

        // Kafka delivers "at least once", so the same event can arrive twice. Skip it if we already stored it.
        if (auditLogRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate event {} ignored", event.getEventId());
            return;
        }

        AuditLog entry = AuditLog.builder()
                .eventId(event.getEventId())
                .eventType(String.valueOf(event.getEventType()))
                .orderId(event.getOrderId())
                .userId(event.getUserId())
                .payload(toJson(event))
                .occurredAt(event.getOccurredAt())
                .kafkaPartition(partition)
                .kafkaOffset(offset)
                .build();
        auditLogRepository.save(entry);
        log.info("Audit saved: {} orderId={} (partition={}, offset={})",
                event.getEventType(), event.getOrderId(), partition, offset);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAll() {
        return auditLogRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getByOrderId(Long orderId) {
        return auditLogRepository.findByOrderIdOrderByIdAsc(orderId).stream().map(this::toResponse).toList();
    }

    private String toJson(OrderEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            // Cannot happen for this simple class, but if it does the Kafka error handler will retry the message
            throw new IllegalStateException("Could not convert the event to JSON", ex);
        }
    }

    private AuditLogResponse toResponse(AuditLog a) {
        return AuditLogResponse.builder()
                .id(a.getId())
                .eventId(a.getEventId())
                .eventType(a.getEventType())
                .orderId(a.getOrderId())
                .userId(a.getUserId())
                .payload(a.getPayload())
                .occurredAt(a.getOccurredAt())
                .recordedAt(a.getRecordedAt())
                .kafkaPartition(a.getKafkaPartition())
                .kafkaOffset(a.getKafkaOffset())
                .build();
    }
}
