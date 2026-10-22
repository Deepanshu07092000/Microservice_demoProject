package com.example.auditservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Audit Service: listens to the Kafka topic "order-events" and keeps a permanent, append-only history
 * of everything that happened to orders ("who/what/when"), stored in its own database.
 */
@SpringBootApplication
public class AuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
