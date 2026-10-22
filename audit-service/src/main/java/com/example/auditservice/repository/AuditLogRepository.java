package com.example.auditservice.repository;

import com.example.auditservice.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    boolean existsByEventId(String eventId);

    List<AuditLog> findAllByOrderByIdDesc();

    List<AuditLog> findByOrderIdOrderByIdAsc(Long orderId);
}
