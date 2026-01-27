package com.psicoagenda.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.psicoagenda.domain.enums.AppointmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "appointment", indexes = {
    @Index(name = "idx_appointment_start", columnList = "start_datetime"),
    @Index(name = "idx_appointment_patient", columnList = "patient_id"),
    @Index(name = "idx_appointment_status", columnList = "status"),
    @Index(name = "idx_appointment_cancellation_token", columnList = "cancellation_token")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_series_id")
    private RecurringSeries recurringSeries;

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AppointmentStatus status = AppointmentStatus.CONFIRMED;

    @Column(name = "session_link", length = 500)
    private String sessionLink;

    @Column(name = "cancellation_token", unique = true)
    private String cancellationToken;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancelled_by")
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @JsonIgnore
    @OneToOne(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    @JsonIgnore
    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NotificationLog> notificationLogs = new ArrayList<>();

    public boolean isCancellable() {
        return status == AppointmentStatus.CONFIRMED || status == AppointmentStatus.SCHEDULED;
    }

    public boolean isWithinCancellationWindow(int hoursBeforeLimit) {
        return LocalDateTime.now().plusHours(hoursBeforeLimit).isBefore(startDateTime);
    }
}
