package com.example.auditservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** One immutable row per event, saved in audit_service_db, table "audit_logs". Rows are never updated or deleted. */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The id of the Kafka event. UNIQUE, so a duplicate delivery can never create a second audit row. */
    @Column(nullable = false, unique = true, length = 64)
    private String eventId;

    @Column(nullable = false, length = 30)
    private String eventType;

    private Long orderId;

    private Long userId;

    /** The complete event as JSON text, exactly as it was received. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /** When the order change happened (taken from the event). */
    private Instant occurredAt;

    /** When Audit Service stored it. */
    @Column(nullable = false, updatable = false)
    private Instant recordedAt;

    /** Where the message sat in Kafka - handy for tracing and for learning how partitions/offsets work. */
    private Integer kafkaPartition;

    private Long kafkaOffset;

    @PrePersist
    void onCreate() {
        this.recordedAt = Instant.now();
    }
}
