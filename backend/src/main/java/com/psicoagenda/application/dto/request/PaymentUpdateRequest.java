package com.psicoagenda.application.dto.request;

import com.psicoagenda.domain.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentUpdateRequest(
    @NotNull(message = "Status é obrigatório")
    PaymentStatus status,

    LocalDateTime paidAt,

    BigDecimal amount,

    @Size(max = 100, message = "Número do recibo deve ter no máximo 100 caracteres")
    String receiptNumber,

    @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres")
    String notes
) {}
