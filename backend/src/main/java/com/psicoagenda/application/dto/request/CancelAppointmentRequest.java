package com.psicoagenda.application.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CancelAppointmentRequest(
    @NotBlank(message = "Token de cancelamento é obrigatório")
    String cancellationToken,

    String reason
) {}
