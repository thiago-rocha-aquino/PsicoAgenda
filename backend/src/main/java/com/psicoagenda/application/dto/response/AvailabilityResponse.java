package com.psicoagenda.application.dto.response;

import com.psicoagenda.domain.entity.Availability;
import com.psicoagenda.domain.enums.DayOfWeekEnum;

import java.time.LocalTime;
import java.util.UUID;

public record AvailabilityResponse(
    UUID id,
    DayOfWeekEnum dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    boolean active
) {
    public static AvailabilityResponse from(Availability entity) {
        return new AvailabilityResponse(
            entity.getId(),
            entity.getDayOfWeek(),
            entity.getStartTime(),
            entity.getEndTime(),
            entity.isActive()
        );
    }
}
