package com.psicoagenda.application.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record RescheduleRequest(
    @NotBlank(message = "Token de cancelamento é obrigatório")
    String cancellationToken,

    @NotNull(message = "Nova data e hora são obrigatórios")
    @Future(message = "A data deve ser no futuro")
    LocalDateTime newStartDateTime
) {}
