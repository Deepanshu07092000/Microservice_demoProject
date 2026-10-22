package com.example.auditservice.service;

import com.example.auditservice.dto.AuditLogResponse;
import com.example.auditservice.event.OrderEvent;

import java.util.List;

public interface AuditService {

    /** Called by the Kafka listener for every order event. */
    void record(OrderEvent event, int partition, long offset);

    List<AuditLogResponse> getAll();

    /** The full history of one order, oldest event first. */
    List<AuditLogResponse> getByOrderId(Long orderId);
}
