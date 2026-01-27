package com.psicoagenda.infrastructure.scheduler;

import com.psicoagenda.domain.entity.Appointment;
import com.psicoagenda.domain.enums.NotificationTrigger;
import com.psicoagenda.domain.repository.AppointmentRepository;
import com.psicoagenda.domain.repository.NotificationLogRepository;
import com.psicoagenda.infrastructure.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final AppointmentRepository appointmentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final NotificationService notificationService;

    @Value("${app.notifications.enabled}")
    private boolean notificationsEnabled;

    public ReminderScheduler(AppointmentRepository appointmentRepository,
                             NotificationLogRepository notificationLogRepository,
                             NotificationService notificationService) {
        this.appointmentRepository = appointmentRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.notificationService = notificationService;
    }

    /**
     * Send 24-hour reminders - runs every hour
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour at minute 0
    public void send24HourReminders() {
        if (!notificationsEnabled) {
            return;
        }

        log.debug("Running 24-hour reminder job");

        LocalDateTime now = LocalDateTime.now();
        // Find appointments between 23 and 25 hours from now
        LocalDateTime start = now.plusHours(23);
        LocalDateTime end = now.plusHours(25);

        List<Appointment> appointments = appointmentRepository.findUpcomingForReminders(start, end);

        for (Appointment appointment : appointments) {
            // Check if reminder was already sent (idempotent)
            if (!notificationLogRepository.existsByAppointmentIdAndTriggerType(
                appointment.getId(), NotificationTrigger.REMINDER_24H)) {

                notificationService.sendNotification(appointment, NotificationTrigger.REMINDER_24H);
                log.info("Sent 24h reminder for appointment {}", appointment.getId());
            }
        }

        log.debug("24-hour reminder job completed. Processed {} appointments", appointments.size());
    }

    /**
     * Send 2-hour reminders - runs every 15 minutes
     */
    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    public void send2HourReminders() {
        if (!notificationsEnabled) {
            return;
        }

        log.debug("Running 2-hour reminder job");

        LocalDateTime now = LocalDateTime.now();
        // Find appointments between 1h45m and 2h15m from now
        LocalDateTime start = now.plusMinutes(105);
        LocalDateTime end = now.plusMinutes(135);

        List<Appointment> appointments = appointmentRepository.findUpcomingForReminders(start, end);

        for (Appointment appointment : appointments) {
            // Check if reminder was already sent (idempotent)
            if (!notificationLogRepository.existsByAppointmentIdAndTriggerType(
                appointment.getId(), NotificationTrigger.REMINDER_2H)) {

                notificationService.sendNotification(appointment, NotificationTrigger.REMINDER_2H);
                log.info("Sent 2h reminder for appointment {}", appointment.getId());
            }
        }

        log.debug("2-hour reminder job completed. Processed {} appointments", appointments.size());
    }
}
