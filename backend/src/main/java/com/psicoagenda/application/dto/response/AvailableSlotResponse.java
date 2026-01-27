package com.psicoagenda.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record AvailableSlotResponse(
    LocalDate date,
    List<TimeSlot> slots
) {
    public record TimeSlot(
        LocalTime time,
        LocalDateTime dateTime,
        boolean available
    ) {}
}
