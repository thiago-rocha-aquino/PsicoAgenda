package com.psicoagenda.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.psicoagenda.domain.entity.AuditLog;
import com.psicoagenda.domain.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logCreate(String entityType, UUID entityId, Object newValue) {
        logAction(entityType, entityId, "CREATE", null, newValue);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logUpdate(String entityType, UUID entityId, Object oldValue, Object newValue) {
        logAction(entityType, entityId, "UPDATE", oldValue, newValue);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDelete(String entityType, UUID entityId, Object oldValue) {
        logAction(entityType, entityId, "DELETE", oldValue, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAccess(String entityType, UUID entityId, String action) {
        logAction(entityType, entityId, action, null, null);
    }

    private void logAction(String entityType, UUID entityId, String action, Object oldValue, Object newValue) {
        try {
            String performedBy = getCurrentUser();
            String ipAddress = getCurrentIpAddress();

            AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .oldValue(toJson(oldValue))
                .newValue(toJson(newValue))
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .build();

            auditLogRepository.save(auditLog);

            log.debug("Audit log: {} {} {} by {} from {}",
                action, entityType, entityId, performedBy, ipAddress);
        } catch (Exception e) {
            log.error("Failed to create audit log for {} {} {}", action, entityType, entityId, e);
        }
    }

    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    private String getCurrentIpAddress() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not get IP address", e);
        }
        return null;
    }

    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            // Always serialize to JSON, including strings and enums
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Could not serialize object to JSON", e);
            // Return as JSON string as fallback
            return "\"" + obj.toString().replace("\"", "\\\"") + "\"";
        }
    }
}
