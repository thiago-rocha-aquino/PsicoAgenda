package com.psicoagenda.infrastructure.notification;

import com.psicoagenda.domain.entity.Appointment;
import com.psicoagenda.domain.enums.NotificationTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class EmailNotificationProvider implements NotificationProvider {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationProvider.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final JavaMailSender mailSender;

    @Value("${app.notifications.enabled}")
    private boolean enabled;

    @Value("${app.notifications.from-email}")
    private String fromEmail;

    @Value("${app.notifications.from-name}")
    private String fromName;

    public EmailNotificationProvider(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public boolean sendNotification(Appointment appointment, NotificationTrigger trigger) {
        if (!enabled) {
            log.debug("Email notifications are disabled");
            return false;
        }

        String recipientEmail = appointment.getPatient().getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.debug("No email for patient {}", appointment.getPatient().getId());
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromName + " <" + fromEmail + ">");
            message.setTo(recipientEmail);
            message.setSubject(getSubject(trigger));
            message.setText(getBody(appointment, trigger));

            mailSender.send(message);
            log.info("Email sent to {} for trigger {}", recipientEmail, trigger);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email to {}", recipientEmail, e);
            return false;
        }
    }

    private String getSubject(NotificationTrigger trigger) {
        return switch (trigger) {
            case BOOKING_CONFIRMATION -> "Confirmacao de Agendamento";
            case REMINDER_24H, REMINDER_2H -> "Lembrete de Compromisso";
            case CANCELLATION -> "Cancelamento de Agendamento";
            case RESCHEDULE -> "Reagendamento Confirmado";
        };
    }

    private String getBody(Appointment appointment, NotificationTrigger trigger) {
        String patientName = appointment.getPatient().getName();
        String date = appointment.getStartDateTime().format(DATE_FORMATTER);
        String time = appointment.getStartDateTime().format(TIME_FORMATTER);

        // Neutral messages - no mention of psychology
        return switch (trigger) {
            case BOOKING_CONFIRMATION -> String.format(
                "Ola %s,\n\n" +
                "Seu agendamento foi confirmado para %s as %s.\n\n" +
                "Caso precise cancelar ou reagendar, utilize os links enviados anteriormente.\n\n" +
                "Atenciosamente,\n%s",
                patientName, date, time, fromName
            );
            case REMINDER_24H -> String.format(
                "Ola %s,\n\n" +
                "Este e um lembrete do seu compromisso amanha, %s, as %s.\n\n" +
                "Atenciosamente,\n%s",
                patientName, date, time, fromName
            );
            case REMINDER_2H -> String.format(
                "Ola %s,\n\n" +
                "Lembrete: voce tem um compromisso hoje as %s.\n\n" +
                "Atenciosamente,\n%s",
                patientName, time, fromName
            );
            case CANCELLATION -> String.format(
                "Ola %s,\n\n" +
                "Seu agendamento para %s as %s foi cancelado.\n\n" +
                "Atenciosamente,\n%s",
                patientName, date, time, fromName
            );
            case RESCHEDULE -> String.format(
                "Ola %s,\n\n" +
                "Seu agendamento foi reagendado para %s as %s.\n\n" +
                "Atenciosamente,\n%s",
                patientName, date, time, fromName
            );
        };
    }

    @Override
    public String getType() {
        return "EMAIL";
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
