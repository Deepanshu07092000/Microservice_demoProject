package com.example.notificationservice.exception;

import java.time.LocalDateTime;

/** Uniform JSON shape for errors returned by this service. */
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {
}
