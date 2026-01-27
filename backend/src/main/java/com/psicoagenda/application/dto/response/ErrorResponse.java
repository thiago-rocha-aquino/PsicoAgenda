package com.psicoagenda.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record ErrorResponse(
    int status,
    String error,
    String message,
    String path,
    LocalDateTime timestamp,
    List<FieldError> fieldErrors,
    Map<String, Object> details
) {
    public record FieldError(
        String field,
        String message,
        Object rejectedValue
    ) {}

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), null, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), fieldErrors, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, Map<String, Object> details) {
        return new ErrorResponse(status, error, message, path, LocalDateTime.now(), null, details);
    }
}
