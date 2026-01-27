package com.psicoagenda.application.dto.response;

import com.psicoagenda.domain.entity.Block;
import com.psicoagenda.domain.enums.BlockType;

import java.time.LocalDateTime;
import java.util.UUID;

public record BlockResponse(
    UUID id,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    BlockType blockType,
    String reason
) {
    public static BlockResponse from(Block entity) {
        return new BlockResponse(
            entity.getId(),
            entity.getStartDateTime(),
            entity.getEndDateTime(),
            entity.getBlockType(),
            entity.getReason()
        );
    }
}
