package com.psicoagenda.application.dto.request;

import com.psicoagenda.domain.enums.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAppointmentStatusRequest(
    @NotNull(message = "Status é obrigatório")
    AppointmentStatus status,

    @Size(max = 500, message = "Motivo deve ter no máximo 500 caracteres")
    String reason,

    @Size(max = 500, message = "Link da sessão deve ter no máximo 500 caracteres")
    String sessionLink
) {}
