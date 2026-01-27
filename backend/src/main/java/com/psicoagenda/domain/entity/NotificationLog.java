package com.psicoagenda.domain.entity;

import com.psicoagenda.domain.enums.NotificationStatus;
import com.psicoagenda.domain.enums.NotificationTrigger;
import com.psicoagenda.domain.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_log", indexes = {
    @Index(name = "idx_notification_appointment", columnList = "appointment_id"),
    @Index(name = "idx_notification_trigger", columnList = "trigger_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private NotificationTrigger triggerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "recipient")
    private String recipient;
}
