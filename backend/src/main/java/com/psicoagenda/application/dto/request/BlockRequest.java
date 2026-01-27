package com.psicoagenda.application.dto.request;

import com.psicoagenda.domain.enums.BlockType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record BlockRequest(
    @NotNull(message = "Data/hora de início é obrigatória")
    LocalDateTime startDateTime,

    @NotNull(message = "Data/hora de término é obrigatória")
    LocalDateTime endDateTime,

    @NotNull(message = "Tipo de bloqueio é obrigatório")
    BlockType blockType,

    @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    String reason
) {}
