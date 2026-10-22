package com.example.notificationservice.service;

import com.example.notificationservice.dto.NotificationResponse;
import com.example.notificationservice.entity.Notification;
import com.example.notificationservice.event.OrderEvent;
import com.example.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Business logic: turn an order event into a customer message and store it. */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void handleOrderEvent(OrderEvent event) {
        if (event == null || event.getEventId() == null) {
            log.warn("Ignoring an order event without an eventId");
            return;
        }

        // Kafka guarantees "at least once": the same message can arrive twice (for example after a restart).
        // Because eventId is unique we can detect that and skip it, so the customer is not notified twice.
        if (notificationRepository.existsByEventId(event.getEventId())) {
            log.warn("Duplicate event {} ignored", event.getEventId());
            return;
        }

        String message = buildMessage(event);

        // A real system would call an email/SMS provider here. We only log it.
        log.info("[SIMULATED EMAIL] to userId={} : {}", event.getUserId(), message);

        notificationRepository.save(Notification.builder()
                .eventId(event.getEventId())
                .userId(event.getUserId())
                .orderId(event.getOrderId())
                .eventType(String.valueOf(event.getEventType()))
                .channel("EMAIL")
                .message(message)
                .status("SENT")
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getAll() {
        return notificationRepository.findAllByOrderByIdDesc().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByIdDesc(userId).stream().map(this::toResponse).toList();
    }

    private String buildMessage(OrderEvent e) {
        String type = e.getEventType() == null ? "" : e.getEventType();
        return switch (type) {
            case "ORDER_CREATED" -> "Your order #%d for %d x %s has been placed. Total: %s."
                    .formatted(e.getOrderId(), e.getQuantity(), e.getProductName(), e.getTotalAmount());
            case "ORDER_UPDATED" -> "Your order #%d was updated. Status: %s. Total: %s."
                    .formatted(e.getOrderId(), e.getOrderStatus(), e.getTotalAmount());
            case "ORDER_DELETED" -> "Your order #%d (%s) was removed."
                    .formatted(e.getOrderId(), e.getProductName());
            default -> "There is an update on your order #%d.".formatted(e.getOrderId());
        };
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .userId(n.getUserId())
                .orderId(n.getOrderId())
                .eventType(n.getEventType())
                .channel(n.getChannel())
                .message(n.getMessage())
                .status(n.getStatus())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
