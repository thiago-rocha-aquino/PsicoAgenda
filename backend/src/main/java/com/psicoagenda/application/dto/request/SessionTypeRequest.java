package com.psicoagenda.application.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SessionTypeRequest(
    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
    String name,

    @NotNull(message = "Duração é obrigatória")
    @Min(value = 15, message = "Duração mínima é de 15 minutos")
    @Max(value = 240, message = "Duração máxima é de 240 minutos")
    Integer durationMinutes,

    @NotNull(message = "Preço é obrigatório")
    @DecimalMin(value = "0.00", message = "Preço não pode ser negativo")
    BigDecimal price,

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    String description,

    Boolean active,

    Integer displayOrder
) {}
