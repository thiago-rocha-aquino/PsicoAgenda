package com.psicoagenda.application.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingRequest(
    @NotNull(message = "Tipo de sessão é obrigatório")
    UUID sessionTypeId,

    @NotNull(message = "Data e hora são obrigatórios")
    @Future(message = "A data deve ser no futuro")
    LocalDateTime startDateTime,

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String patientName,

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$|^\\(\\d{2}\\)\\s?\\d{4,5}-?\\d{4}$",
             message = "Telefone inválido")
    String patientPhone,

    @Email(message = "Email inválido")
    String patientEmail,

    @NotNull(message = "Aceite do termo é obrigatório")
    @AssertTrue(message = "É necessário aceitar os termos")
    Boolean consentAccepted,

    String consentVersion
) {}
