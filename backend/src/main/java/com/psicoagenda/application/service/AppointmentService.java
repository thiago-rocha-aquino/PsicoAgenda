package com.psicoagenda.application.service;

import com.psicoagenda.application.dto.request.AdminAppointmentRequest;
import com.psicoagenda.application.dto.request.BookingRequest;
import com.psicoagenda.application.dto.request.RescheduleRequest;
import com.psicoagenda.application.dto.request.UpdateAppointmentStatusRequest;
import com.psicoagenda.application.dto.response.AppointmentResponse;
import com.psicoagenda.application.dto.response.BookingConfirmationResponse;
import com.psicoagenda.application.exception.BusinessException;
import com.psicoagenda.application.exception.ConflictException;
import com.psicoagenda.application.exception.ResourceNotFoundException;
import com.psicoagenda.application.exception.ValidationException;
import com.psicoagenda.domain.entity.Appointment;
import com.psicoagenda.domain.entity.Patient;
import com.psicoagenda.domain.entity.Payment;
import com.psicoagenda.domain.entity.SessionType;
import com.psicoagenda.domain.enums.AppointmentStatus;
import com.psicoagenda.domain.enums.NotificationTrigger;
import com.psicoagenda.domain.enums.PaymentStatus;
import com.psicoagenda.domain.repository.AppointmentRepository;
import com.psicoagenda.domain.repository.PaymentRepository;
import com.psicoagenda.domain.repository.SessionTypeRepository;
import com.psicoagenda.infrastructure.audit.AuditService;
import com.psicoagenda.infrastructure.notification.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final PaymentRepository paymentRepository;
    private final PatientService patientService;
    private final ConsentService consentService;
    private final AvailabilityService availabilityService;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Value("${app.booking.cancellation-hours:24}")
    private int cancellationHours;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              SessionTypeRepository sessionTypeRepository,
                              PaymentRepository paymentRepository,
                              PatientService patientService,
                              ConsentService consentService,
                              AvailabilityService availabilityService,
                              NotificationService notificationService,
                              AuditService auditService) {
        this.appointmentRepository = appointmentRepository;
        this.sessionTypeRepository = sessionTypeRepository;
        this.paymentRepository = paymentRepository;
        this.patientService = patientService;
        this.consentService = consentService;
        this.availabilityService = availabilityService;
        this.notificationService = notificationService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointmentsInRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findAllAppointmentsInRange(start, end)
            .stream()
            .map(AppointmentResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getActiveAppointmentsInRange(LocalDateTime start, LocalDateTime end) {
        return appointmentRepository.findAppointmentsInRange(start, end)
            .stream()
            .map(AppointmentResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getUpcomingAppointments(int limit) {
        return appointmentRepository.findNextAppointments(limit)
            .stream()
            .map(AppointmentResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID id) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento", "id", id));
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentByToken(String token) {
        Appointment appointment = appointmentRepository.findByCancellationToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento", "token", token));
        return AppointmentResponse.from(appointment);
    }

    /**
     * Public booking endpoint - creates appointment from public form
     */
    public BookingConfirmationResponse createPublicBooking(BookingRequest request, String ipAddress, String userAgent) {
        // Get session type
        SessionType sessionType = sessionTypeRepository.findById(request.sessionTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de sessão", "id", request.sessionTypeId()));

        if (!sessionType.isActive()) {
            throw new BusinessException("Este tipo de sessão não está disponível");
        }

        // Validate slot availability
        if (!availabilityService.isSlotAvailable(request.startDateTime(), sessionType.getDurationMinutes())) {
            throw new ConflictException("O horário selecionado não está disponível");
        }

        // Find or create patient
        Patient patient = patientService.findOrCreatePatient(
            request.patientName(),
            request.patientPhone(),
            request.patientEmail()
        );

        // Record consent
        String consentVersion = request.consentVersion() != null ? request.consentVersion() : "1.0";
        consentService.recordConsent(patient, consentVersion, ipAddress, userAgent);

        // Create appointment
        LocalDateTime endDateTime = request.startDateTime().plusMinutes(sessionType.getDurationMinutes());
        String cancellationToken = UUID.randomUUID().toString();

        Appointment appointment = Appointment.builder()
            .patient(patient)
            .sessionType(sessionType)
            .startDateTime(request.startDateTime())
            .endDateTime(endDateTime)
            .status(AppointmentStatus.CONFIRMED)
            .cancellationToken(cancellationToken)
            .build();

        appointment = appointmentRepository.save(appointment);

        // Create payment record
        Payment payment = Payment.builder()
            .appointment(appointment)
            .status(PaymentStatus.UNPAID)
            .amount(sessionType.getPrice())
            .build();
        paymentRepository.save(payment);

        log.info("Created public booking: {} for patient {}", appointment.getId(), patient.getId());
        auditService.logCreate("Appointment", appointment.getId(), appointment);

        // Send confirmation notification
        notificationService.sendNotification(appointment, NotificationTrigger.BOOKING_CONFIRMATION);

        return new BookingConfirmationResponse(
            appointment.getId(),
            patient.getName(),
            sessionType.getName(),
            appointment.getStartDateTime(),
            appointment.getEndDateTime(),
            cancellationToken,
            "/cancelar?token=" + cancellationToken,
            "/reagendar?token=" + cancellationToken,
            "Agendamento confirmado com sucesso!"
        );
    }

    /**
     * Admin creates appointment
     */
    public AppointmentResponse createAdminAppointment(AdminAppointmentRequest request) {
        SessionType sessionType = sessionTypeRepository.findById(request.sessionTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de sessão", "id", request.sessionTypeId()));

        // Determine patient
        Patient patient;
        if (request.patientId() != null) {
            patient = patientService.findOrCreatePatient(
                request.patientName(),
                request.patientPhone(),
                request.patientEmail()
            );
        } else if (request.patientName() != null && request.patientPhone() != null) {
            patient = patientService.findOrCreatePatient(
                request.patientName(),
                request.patientPhone(),
                request.patientEmail()
            );
        } else {
            throw new ValidationException("Informe o ID do paciente ou os dados para cadastro");
        }

        LocalDateTime endDateTime = request.startDateTime().plusMinutes(sessionType.getDurationMinutes());

        // Check for conflicts (admin can bypass advance time restrictions)
        if (appointmentRepository.existsOverlappingAppointment(request.startDateTime(), endDateTime)) {
            throw new ConflictException("Já existe um agendamento neste horário");
        }

        String cancellationToken = UUID.randomUUID().toString();

        Appointment appointment = Appointment.builder()
            .patient(patient)
            .sessionType(sessionType)
            .startDateTime(request.startDateTime())
            .endDateTime(endDateTime)
            .status(request.status() != null ? request.status() : AppointmentStatus.CONFIRMED)
            .sessionLink(request.sessionLink())
            .cancellationToken(cancellationToken)
            .build();

        appointment = appointmentRepository.save(appointment);

        // Create payment record
        Payment payment = Payment.builder()
            .appointment(appointment)
            .status(PaymentStatus.UNPAID)
            .amount(sessionType.getPrice())
            .build();
        paymentRepository.save(payment);

        log.info("Admin created appointment: {} for patient {}", appointment.getId(), patient.getId());
        auditService.logCreate("Appointment", appointment.getId(), appointment);

        return AppointmentResponse.from(appointment);
    }

    /**
     * Update appointment status (admin)
     */
    public AppointmentResponse updateAppointmentStatus(UUID id, UpdateAppointmentStatusRequest request) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento", "id", id));

        AppointmentStatus oldStatus = appointment.getStatus();

        appointment.setStatus(request.status());

        if (request.status() == AppointmentStatus.CANCELLED || request.status() == AppointmentStatus.CANCELLED_LATE) {
            appointment.setCancelledAt(LocalDateTime.now());
            appointment.setCancelledBy("ADMIN");
            appointment.setCancellationReason(request.reason());
        }

        if (request.sessionLink() != null) {
            appointment.setSessionLink(request.sessionLink());
        }

        appointment = appointmentRepository.save(appointment);
        log.info("Updated appointment {} status from {} to {}", id, oldStatus, request.status());

        auditService.logUpdate("Appointment", id, oldStatus.name(), request.status().name());

        return AppointmentResponse.from(appointment);
    }

    /**
     * Public cancellation (by patient via token)
     */
    public void cancelByToken(String token, String reason) {
        Appointment appointment = appointmentRepository.findByCancellationToken(token)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento", "token", token));

        if (!appointment.isCancellable()) {
            throw new BusinessException("Este agendamento não pode mais ser cancelado");
        }

        boolean withinWindow = appointment.isWithinCancellationWindow(cancellationHours);

        AppointmentStatus newStatus = withinWindow ?
            AppointmentStatus.CANCELLED : AppointmentStatus.CANCELLED_LATE;

        appointment.setStatus(newStatus);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancelledBy("PATIENT");
        appointment.setCancellationReason(reason);

        appointmentRepository.save(appointment);
        log.info("Patient cancelled appointment {} ({})", appointment.getId(), newStatus);

        auditService.logUpdate("Appointment", appointment.getId(), "CONFIRMED", newStatus.name());

        // Notify about cancellation
        notificationService.sendNotification(appointment, NotificationTrigger.CANCELLATION);
    }

    /**
     * Public reschedule (by patient via token)
     */
    public BookingConfirmationResponse rescheduleByToken(RescheduleRequest request) {
        Appointment appointment = appointmentRepository.findByCancellationToken(request.cancellationToken())
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento", "token", request.cancellationToken()));

        if (!appointment.isCancellable()) {
            throw new BusinessException("Este agendamento não pode mais ser reagendado");
        }

        if (!appointment.isWithinCancellationWindow(cancellationHours)) {
            throw new BusinessException("O prazo para reagendamento expirou. Entre em contato diretamente.");
        }

        int duration = appointment.getSessionType().getDurationMinutes();

        // Check new slot availability
        if (!availabilityService.isSlotAvailableExcluding(
            request.newStartDateTime(), duration, appointment.getId())) {
            throw new ConflictException("O novo horário não está disponível");
        }

        // Update appointment
        LocalDateTime oldStart = appointment.getStartDateTime();
        appointment.setStartDateTime(request.newStartDateTime());
        appointment.setEndDateTime(request.newStartDateTime().plusMinutes(duration));

        // Generate new cancellation token
        String newToken = UUID.randomUUID().toString();
        appointment.setCancellationToken(newToken);

        appointment = appointmentRepository.save(appointment);
        log.info("Rescheduled appointment {} from {} to {}",
            appointment.getId(), oldStart, request.newStartDateTime());

        auditService.logUpdate("Appointment", appointment.getId(),
            "startDateTime: " + oldStart, "startDateTime: " + request.newStartDateTime());

        // Notify about reschedule
        notificationService.sendNotification(appointment, NotificationTrigger.RESCHEDULE);

        return new BookingConfirmationResponse(
            appointment.getId(),
            appointment.getPatient().getName(),
            appointment.getSessionType().getName(),
            appointment.getStartDateTime(),
            appointment.getEndDateTime(),
            newToken,
            "/cancelar?token=" + newToken,
            "/reagendar?token=" + newToken,
            "Agendamento reagendado com sucesso!"
        );
    }

    /**
     * Get appointments for a patient
     */
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getPatientAppointments(UUID patientId) {
        return appointmentRepository.findByPatientIdOrderByStartDateTimeDesc(patientId)
            .stream()
            .map(AppointmentResponse::from)
            .collect(Collectors.toList());
    }
}
