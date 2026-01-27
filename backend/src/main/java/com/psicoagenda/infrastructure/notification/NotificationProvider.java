package com.psicoagenda.infrastructure.notification;

import com.psicoagenda.domain.entity.Appointment;
import com.psicoagenda.domain.enums.NotificationTrigger;

/**
 * Interface for notification providers.
 * Implement this to add new notification channels (email, SMS, WhatsApp, etc.)
 */
public interface NotificationProvider {

    /**
     * Send a notification for an appointment
     * @param appointment The appointment
     * @param trigger The trigger that caused this notification
     * @return true if sent successfully, false otherwise
     */
    boolean sendNotification(Appointment appointment, NotificationTrigger trigger);

    /**
     * Get the type of this provider
     */
    String getType();

    /**
     * Check if this provider is enabled
     */
    boolean isEnabled();
}
