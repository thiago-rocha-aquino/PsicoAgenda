package com.psicoagenda.application.service;

import com.psicoagenda.application.dto.request.SessionTypeRequest;
import com.psicoagenda.application.dto.response.SessionTypeResponse;
import com.psicoagenda.application.exception.ResourceNotFoundException;
import com.psicoagenda.domain.entity.SessionType;
import com.psicoagenda.domain.repository.SessionTypeRepository;
import com.psicoagenda.infrastructure.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SessionTypeService {

    private static final Logger log = LoggerFactory.getLogger(SessionTypeService.class);

    private final SessionTypeRepository sessionTypeRepository;
    private final AuditService auditService;

    public SessionTypeService(SessionTypeRepository sessionTypeRepository, AuditService auditService) {
        this.sessionTypeRepository = sessionTypeRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SessionTypeResponse> getAllSessionTypes() {
        return sessionTypeRepository.findAllByOrderByDisplayOrderAsc()
            .stream()
            .map(SessionTypeResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SessionTypeResponse> getActiveSessionTypes() {
        return sessionTypeRepository.findByActiveTrueOrderByDisplayOrderAsc()
            .stream()
            .map(SessionTypeResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SessionTypeResponse getSessionTypeById(UUID id) {
        SessionType sessionType = sessionTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de sessão", "id", id));
        return SessionTypeResponse.from(sessionType);
    }

    public SessionTypeResponse createSessionType(SessionTypeRequest request) {
        SessionType sessionType = SessionType.builder()
            .name(request.name())
            .durationMinutes(request.durationMinutes())
            .price(request.price())
            .description(request.description())
            .active(request.active() != null ? request.active() : true)
            .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
            .build();

        sessionType = sessionTypeRepository.save(sessionType);
        log.info("Created session type: {}", sessionType.getName());

        auditService.logCreate("SessionType", sessionType.getId(), sessionType);

        return SessionTypeResponse.from(sessionType);
    }

    public SessionTypeResponse updateSessionType(UUID id, SessionTypeRequest request) {
        SessionType sessionType = sessionTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de sessão", "id", id));

        SessionType oldState = cloneSessionType(sessionType);

        sessionType.setName(request.name());
        sessionType.setDurationMinutes(request.durationMinutes());
        sessionType.setPrice(request.price());
        sessionType.setDescription(request.description());
        if (request.active() != null) {
            sessionType.setActive(request.active());
        }
        if (request.displayOrder() != null) {
            sessionType.setDisplayOrder(request.displayOrder());
        }

        sessionType = sessionTypeRepository.save(sessionType);
        log.info("Updated session type: {}", id);

        auditService.logUpdate("SessionType", sessionType.getId(), oldState, sessionType);

        return SessionTypeResponse.from(sessionType);
    }

    public void deleteSessionType(UUID id) {
        SessionType sessionType = sessionTypeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de sessão", "id", id));

        // Soft delete - just deactivate
        sessionType.setActive(false);
        sessionTypeRepository.save(sessionType);

        log.info("Deactivated session type: {}", id);
        auditService.logUpdate("SessionType", id, sessionType, sessionType);
    }

    private SessionType cloneSessionType(SessionType original) {
        return SessionType.builder()
            .name(original.getName())
            .durationMinutes(original.getDurationMinutes())
            .price(original.getPrice())
            .description(original.getDescription())
            .active(original.isActive())
            .displayOrder(original.getDisplayOrder())
            .build();
    }
}
