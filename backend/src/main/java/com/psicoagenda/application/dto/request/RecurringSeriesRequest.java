package com.psicoagenda.application.dto.request;

import com.psicoagenda.domain.enums.DayOfWeekEnum;
import com.psicoagenda.domain.enums.RecurrenceFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RecurringSeriesRequest(
    // Patient info - can be existing patient ID or new patient data
    UUID patientId,

    @Size(min = 2, max = 255, message = "Nome deve ter entre 2 e 255 caracteres")
    String patientName,

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$|^\\(\\d{2}\\)\\s?\\d{4,5}-?\\d{4}$",
             message = "Telefone inválido")
    String patientPhone,

    String patientEmail,

    @NotNull(message = "Tipo de sessão é obrigatório")
    UUID sessionTypeId,

    @NotNull(message = "Dia da semana é obrigatório")
    DayOfWeekEnum dayOfWeek,

    @NotNull(message = "Hora de início é obrigatória")
    LocalTime startTime,

    @NotNull(message = "Frequência é obrigatória")
    RecurrenceFrequency frequency,

    @NotNull(message = "Data de início é obrigatória")
    LocalDate startDate,

    LocalDate endDate
) {}
