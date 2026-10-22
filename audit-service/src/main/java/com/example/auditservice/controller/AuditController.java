package com.example.auditservice.controller;

import com.example.auditservice.dto.AuditLogResponse;
import com.example.auditservice.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Read-only API. There is deliberately no POST/PUT/DELETE: an audit trail must never be edited. */
@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    /** All audit rows, newest first. */
    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAll() {
        return ResponseEntity.ok(auditService.getAll());
    }

    /** The history of one order, oldest event first. */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<AuditLogResponse>> getByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(auditService.getByOrderId(orderId));
    }
}
