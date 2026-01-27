package com.psicoagenda.api.advice;

import com.psicoagenda.application.dto.response.ErrorResponse;
import com.psicoagenda.application.exception.BusinessException;
import com.psicoagenda.application.exception.ConflictException;
import com.psicoagenda.application.exception.ResourceNotFoundException;
import com.psicoagenda.application.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.NOT_FOUND.value(),
            "NOT_FOUND",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            ex.getErrorCode(),
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        Map<String, Object> details = new HashMap<>();
        if (ex.getConflictingDates() != null) {
            details.put("conflictingDates", ex.getConflictingDates());
        }
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.CONFLICT.value(),
            "CONFLICT",
            ex.getMessage(),
            request.getRequestURI(),
            details.isEmpty() ? null : details
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            ex.getMessage(),
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ErrorResponse.FieldError(
                error.getField(),
                error.getDefaultMessage(),
                error.getRejectedValue()
            ))
            .collect(Collectors.toList());

        log.warn("Validation errors: {}", fieldErrors);

        ErrorResponse error = ErrorResponse.of(
            HttpStatus.BAD_REQUEST.value(),
            "VALIDATION_ERROR",
            "Erro de validação nos dados enviados",
            request.getRequestURI(),
            fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Bad credentials attempt from: {}", request.getRemoteAddr());
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.UNAUTHORIZED.value(),
            "INVALID_CREDENTIALS",
            "Email ou senha inválidos",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.UNAUTHORIZED.value(),
            "AUTHENTICATION_FAILED",
            "Falha na autenticação",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {} to {}", request.getRemoteAddr(), request.getRequestURI());
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.FORBIDDEN.value(),
            "ACCESS_DENIED",
            "Acesso negado",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        ErrorResponse error = ErrorResponse.of(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "INTERNAL_ERROR",
            "Erro interno do servidor",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
