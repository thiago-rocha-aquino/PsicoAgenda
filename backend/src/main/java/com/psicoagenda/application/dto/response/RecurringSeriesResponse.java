package com.psicoagenda.application.dto.response;

import com.psicoagenda.domain.entity.RecurringSeries;
import com.psicoagenda.domain.enums.DayOfWeekEnum;
import com.psicoagenda.domain.enums.RecurrenceFrequency;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record RecurringSeriesResponse(
    UUID id,
    PatientResponse patient,
    SessionTypeResponse sessionType,
    DayOfWeekEnum dayOfWeek,
    LocalTime startTime,
    RecurrenceFrequency frequency,
    LocalDate startDate,
    LocalDate endDate,
    boolean active,
    List<AppointmentResponse> appointments
) {
    public static RecurringSeriesResponse from(RecurringSeries entity) {
        return new RecurringSeriesResponse(
            entity.getId(),
            PatientResponse.from(entity.getPatient()),
            SessionTypeResponse.from(entity.getSessionType()),
            entity.getDayOfWeek(),
            entity.getStartTime(),
            entity.getFrequency(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.isActive(),
            null
        );
    }

    public static RecurringSeriesResponse fromWithAppointments(RecurringSeries entity, List<AppointmentResponse> appointments) {
        return new RecurringSeriesResponse(
            entity.getId(),
            PatientResponse.from(entity.getPatient()),
            SessionTypeResponse.from(entity.getSessionType()),
            entity.getDayOfWeek(),
            entity.getStartTime(),
            entity.getFrequency(),
            entity.getStartDate(),
            entity.getEndDate(),
            entity.isActive(),
            appointments
        );
    }
}
