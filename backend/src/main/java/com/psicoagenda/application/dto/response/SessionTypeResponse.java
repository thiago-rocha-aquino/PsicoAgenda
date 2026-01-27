package com.psicoagenda.application.dto.response;

import com.psicoagenda.domain.entity.SessionType;

import java.math.BigDecimal;
import java.util.UUID;

public record SessionTypeResponse(
    UUID id,
    String name,
    Integer durationMinutes,
    BigDecimal price,
    String description,
    boolean active,
    Integer displayOrder
) {
    public static SessionTypeResponse from(SessionType entity) {
        return new SessionTypeResponse(
            entity.getId(),
            entity.getName(),
            entity.getDurationMinutes(),
            entity.getPrice(),
            entity.getDescription(),
            entity.isActive(),
            entity.getDisplayOrder()
        );
    }
}
