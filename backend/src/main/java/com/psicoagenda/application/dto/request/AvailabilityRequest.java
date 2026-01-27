package com.psicoagenda.application.dto.request;

import com.psicoagenda.domain.enums.DayOfWeekEnum;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record AvailabilityRequest(
    @NotNull(message = "Dia da semana é obrigatório")
    DayOfWeekEnum dayOfWeek,

    @NotNull(message = "Hora de início é obrigatória")
    LocalTime startTime,

    @NotNull(message = "Hora de término é obrigatória")
    LocalTime endTime,

    Boolean active
) {}
