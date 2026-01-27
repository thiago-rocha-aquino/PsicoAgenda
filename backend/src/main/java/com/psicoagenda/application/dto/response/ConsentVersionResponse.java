package com.psicoagenda.application.dto.response;

import com.psicoagenda.domain.entity.ConsentVersion;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentVersionResponse(
    UUID id,
    String version,
    String content,
    LocalDateTime effectiveFrom,
    boolean active
) {
    public static ConsentVersionResponse from(ConsentVersion entity) {
        return new ConsentVersionResponse(
            entity.getId(),
            entity.getVersion(),
            entity.getContent(),
            entity.getEffectiveFrom(),
            entity.isActive()
        );
    }
}
