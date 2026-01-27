package com.psicoagenda.application.dto.request;

import com.psicoagenda.domain.enums.AppointmentStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminAppointmentRequest(
    UUID patientId,

    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String patientName,

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$|^\\(\\d{2}\\)\\s?\\d{4,5}-?\\d{4}$",
             message = "Telefone inválido")
    String patientPhone,

    String patientEmail,

    @NotNull(message = "Tipo de sessão é obrigatório")
    UUID sessionTypeId,

    @NotNull(message = "Data e hora são obrigatórios")
    LocalDateTime startDateTime,

    AppointmentStatus status,

    @Size(max = 500, message = "Link da sessão deve ter no máximo 500 caracteres")
    String sessionLink
) {}
