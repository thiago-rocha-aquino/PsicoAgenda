package com.psicoagenda.infrastructure.notification;

import com.psicoagenda.domain.entity.Appointment;
import com.psicoagenda.domain.entity.NotificationLog;
import com.psicoagenda.domain.enums.NotificationStatus;
import com.psicoagenda.domain.enums.NotificationTrigger;
import com.psicoagenda.domain.enums.NotificationType;
import com.psicoagenda.domain.repository.NotificationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final List<NotificationProvider> providers;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationService(List<NotificationProvider> providers,
                               NotificationLogRepository notificationLogRepository) {
        this.providers = providers;
        this.notificationLogRepository = notificationLogRepository;
    }

    public void sendNotification(Appointment appointment, NotificationTrigger trigger) {
        for (NotificationProvider provider : providers) {
            if (provider.isEnabled()) {
                sendWithProvider(appointment, trigger, provider);
            }
        }
    }

    private void sendWithProvider(Appointment appointment, NotificationTrigger trigger,
                                   NotificationProvider provider) {
        NotificationType type = NotificationType.valueOf(provider.getType());

        // Check if already sent
        if (notificationLogRepository.existsByAppointmentIdAndTriggerType(appointment.getId(), trigger)) {
            log.debug("Notification {} already sent for appointment {}", trigger, appointment.getId());
            return;
        }

        NotificationLog notificationLog = NotificationLog.builder()
            .appointment(appointment)
            .notificationType(type)
            .triggerType(trigger)
            .status(NotificationStatus.PENDING)
            .recipient(getRecipient(appointment, type))
            .build();

        try {
            boolean success = provider.sendNotification(appointment, trigger);

            if (success) {
                notificationLog.setStatus(NotificationStatus.SENT);
                notificationLog.setSentAt(LocalDateTime.now());
            } else {
                notificationLog.setStatus(NotificationStatus.FAILED);
                notificationLog.setErrorMessage("Provider returned false");
            }
        } catch (Exception e) {
            log.error("Failed to send notification", e);
            notificationLog.setStatus(NotificationStatus.FAILED);
            notificationLog.setErrorMessage(e.getMessage());
        }

        notificationLogRepository.save(notificationLog);
    }

    private String getRecipient(Appointment appointment, NotificationType type) {
        return switch (type) {
            case EMAIL -> appointment.getPatient().getEmail();
            case SMS, WHATSAPP -> appointment.getPatient().getPhone();
        };
    }
}
