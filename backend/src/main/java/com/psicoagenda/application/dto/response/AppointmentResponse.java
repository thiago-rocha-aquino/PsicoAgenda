package com.psicoagenda.application.dto.response;

import com.psicoagenda.domain.entity.Appointment;
import com.psicoagenda.domain.enums.AppointmentStatus;
import com.psicoagenda.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    PatientResponse patient,
    SessionTypeResponse sessionType,
    UUID recurringSeriesId,
    LocalDateTime startDateTime,
    LocalDateTime endDateTime,
    AppointmentStatus status,
    String sessionLink,
    String cancellationToken,
    LocalDateTime cancelledAt,
    String cancelledBy,
    String cancellationReason,
    PaymentInfo payment,
    LocalDateTime createdAt
) {
    public record PaymentInfo(
        UUID id,
        PaymentStatus status,
        BigDecimal amount,
        LocalDateTime paidAt,
        String receiptNumber
    ) {}

    public static AppointmentResponse from(Appointment entity) {
        PaymentInfo paymentInfo = null;
        if (entity.getPayment() != null) {
            paymentInfo = new PaymentInfo(
                entity.getPayment().getId(),
                entity.getPayment().getStatus(),
                entity.getPayment().getAmount(),
                entity.getPayment().getPaidAt(),
                entity.getPayment().getReceiptNumber()
            );
        }

        return new AppointmentResponse(
            entity.getId(),
            PatientResponse.from(entity.getPatient()),
            SessionTypeResponse.from(entity.getSessionType()),
            entity.getRecurringSeries() != null ? entity.getRecurringSeries().getId() : null,
            entity.getStartDateTime(),
            entity.getEndDateTime(),
            entity.getStatus(),
            entity.getSessionLink(),
            entity.getCancellationToken(),
            entity.getCancelledAt(),
            entity.getCancelledBy(),
            entity.getCancellationReason(),
            paymentInfo,
            entity.getCreatedAt()
        );
    }

    public static AppointmentResponse fromWithoutToken(Appointment entity) {
        AppointmentResponse full = from(entity);
        return new AppointmentResponse(
            full.id(),
            full.patient(),
            full.sessionType(),
            full.recurringSeriesId(),
            full.startDateTime(),
            full.endDateTime(),
            full.status(),
            full.sessionLink(),
            null, // Hide token
            full.cancelledAt(),
            full.cancelledBy(),
            full.cancellationReason(),
            full.payment(),
            full.createdAt()
        );
    }
}
