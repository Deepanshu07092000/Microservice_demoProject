package com.example.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** One row per notification we "sent". Saved in notification_service_db, table "notifications". */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The id of the Kafka event. UNIQUE, so the same event can never create two notifications. */
    @Column(nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 30)
    private String eventType;

    /** EMAIL here; could be SMS or PUSH in a real system. */
    @Column(nullable = false, length = 20)
    private String channel;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
