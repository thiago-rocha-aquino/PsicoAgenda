package com.psicoagenda.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingConfirmationResponse(
    UUID appointmentId,
    String patientName,
    String sessionTypeName,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    String cancellationToken,
    String cancellationUrl,
    String rescheduleUrl,
    String message
) {}
