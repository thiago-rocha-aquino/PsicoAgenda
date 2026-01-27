package com.psicoagenda.application.dto.response;

import com.psicoagenda.domain.entity.Payment;
import com.psicoagenda.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID appointmentId,
    PaymentStatus status,
    BigDecimal amount,
    LocalDateTime paidAt,
    String receiptNumber,
    String notes,
    AppointmentResponse appointment
) {
    public static PaymentResponse from(Payment entity) {
        return new PaymentResponse(
            entity.getId(),
            entity.getAppointment().getId(),
            entity.getStatus(),
            entity.getAmount(),
            entity.getPaidAt(),
            entity.getReceiptNumber(),
            entity.getNotes(),
            null
        );
    }

    public static PaymentResponse fromWithAppointment(Payment entity) {
        return new PaymentResponse(
            entity.getId(),
            entity.getAppointment().getId(),
            entity.getStatus(),
            entity.getAmount(),
            entity.getPaidAt(),
            entity.getReceiptNumber(),
            entity.getNotes(),
            AppointmentResponse.fromWithoutToken(entity.getAppointment())
        );
    }
}
