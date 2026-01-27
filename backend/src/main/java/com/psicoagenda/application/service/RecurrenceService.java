package com.psicoagenda.application.service;

import com.psicoagenda.application.dto.request.RecurringSeriesRequest;
import com.psicoagenda.application.dto.response.AppointmentResponse;
import com.psicoagenda.application.dto.response.ConflictCheckResponse;
import com.psicoagenda.application.dto.response.RecurringSeriesResponse;
import com.psicoagenda.application.exception.ConflictException;
import com.psicoagenda.application.exception.ResourceNotFoundException;
import com.psicoagenda.application.exception.ValidationException;
import com.psicoagenda.domain.entity.*;
import com.psicoagenda.domain.enums.AppointmentStatus;
import com.psicoagenda.domain.enums.PaymentStatus;
import com.psicoagenda.domain.enums.RecurrenceFrequency;
import com.psicoagenda.domain.repository.*;
import com.psicoagenda.infrastructure.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class RecurrenceService {

    private static final Logger log = LoggerFactory.getLogger(RecurrenceService.class);

    private final RecurringSeriesRepository recurringSeriesRepository;
    private final AppointmentRepository appointmentRepository;
    private final SessionTypeRepository sessionTypeRepository;
    private final PatientRepository patientRepository;
    private final PaymentRepository paymentRepository;
    private final BlockRepository blockRepository;
    private final AvailabilityRepository availabilityRepository;
    private final PatientService patientService;
    private final AuditService auditService;

    @Value("${app.booking.max-advance-days:90}")
    private int maxAdvanceDays;

    public RecurrenceService(RecurringSeriesRepository recurringSeriesRepository,
                             AppointmentRepository appointmentRepository,
                             SessionTypeRepository sessionTypeRepository,
                             PatientRepository patientRepository,
                             PaymentRepository paymentRepository,
                             BlockRepository blockRepository,
                             AvailabilityRepository availabilityRepository,
                             PatientService patientService,
                             AuditService auditService) {
        this.recurringSeriesRepository = recurringSeriesRepository;
        this.appointmentRepository = appointmentRepository;
        this.sessionTypeRepository = sessionTypeRepository;
        this.patientRepository = patientRepository;
        this.paymentRepository = paymentRepository;
        this.blockRepository = blockRepository;
        this.availabilityRepository = availabilityRepository;
        this.patientService = patientService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<RecurringSeriesResponse> getAllActiveSeries() {
        return recurringSeriesRepository.findAllActiveWithDetails()
            .stream()
            .map(RecurringSeriesResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RecurringSeriesResponse getSeriesById(UUID id) {
        RecurringSeries series = recurringSeriesRepository.findByIdWithDetails(id);
        if (series == null) {
            throw new ResourceNotFoundException("Série recorrente", "id", id);
        }

        List<AppointmentResponse> appointments = appointmentRepository.findByRecurringSeriesId(id)
            .stream()
            .map(AppointmentResponse::from)
            .collect(Collectors.toList());

        return RecurringSeriesResponse.fromWithAppointments(series, appointments);
    }

    /**
     * Check for conflicts before creating a recurring series
     */
    public ConflictCheckResponse checkConflicts(RecurringSeriesRequest request) {
        SessionType sessionType = sessionTypeRepository.findById(request.sessionTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de sessão", "id", request.sessionTypeId()));

        List<LocalDateTime> occurrences = generateOccurrences(
            request.startDate(),
            request.endDate(),
            request.dayOfWeek().toJavaDayOfWeek(),
            request.startTime(),
            request.frequency()
        );

        int duration = sessionType.getDurationMinutes();
        List<ConflictCheckResponse.ConflictDetail> conflicts = new ArrayList<>();

        for (LocalDateTime occurrence : occurrences) {
            LocalDateTime endTime = occurrence.plusMinutes(duration);

            // Check appointment conflicts
            if (appointmentRepository.existsOverlappingAppointment(occurrence, endTime)) {
                conflicts.add(new ConflictCheckResponse.ConflictDetail(
                    occurrence, "Conflito com agendamento existente"));
                continue;
            }

            // Check block conflicts
            if (blockRepository.existsBlockOverlapping(occurrence, endTime)) {
                conflicts.add(new ConflictCheckResponse.ConflictDetail(
                    occurrence, "Conflito com bloqueio (férias/feriado/folga)"));
            }
        }

        return new ConflictCheckResponse(
            !conflicts.isEmpty(),
            conflicts,
            occurrences.size(),
            conflicts.size()
        );
    }

    /**
     * Create a new recurring series with all appointments
     */
    public RecurringSeriesResponse createSeries(RecurringSeriesRequest request) {
        // Validate request
        validateRecurrenceRequest(request);

        // Get session type
        SessionType sessionType = sessionTypeRepository.findById(request.sessionTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de sessão", "id", request.sessionTypeId()));

        // Determine patient
        Patient patient;
        if (request.patientId() != null) {
            patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.patientId()));
        } else if (request.patientName() != null && request.patientPhone() != null) {
            patient = patientService.findOrCreatePatient(
                request.patientName(),
                request.patientPhone(),
                request.patientEmail()
            );
        } else {
            throw new ValidationException("Informe o ID do paciente ou os dados para cadastro");
        }

        // Check for conflicts first
        ConflictCheckResponse conflictCheck = checkConflicts(request);
        if (conflictCheck.hasConflicts()) {
            List<LocalDateTime> conflictDates = conflictCheck.conflicts().stream()
                .map(ConflictCheckResponse.ConflictDetail::dateTime)
                .collect(Collectors.toList());
            throw new ConflictException(
                "Existem " + conflictCheck.conflictCount() + " conflitos de horário. Resolva-os antes de criar a série.",
                conflictDates
            );
        }

        // Create the series
        RecurringSeries series = RecurringSeries.builder()
            .patient(patient)
            .sessionType(sessionType)
            .dayOfWeek(request.dayOfWeek())
            .startTime(request.startTime())
            .frequency(request.frequency())
            .startDate(request.startDate())
            .endDate(request.endDate())
            .active(true)
            .build();

        series = recurringSeriesRepository.save(series);

        // Generate and create all appointments
        List<LocalDateTime> occurrences = generateOccurrences(
            request.startDate(),
            request.endDate(),
            request.dayOfWeek().toJavaDayOfWeek(),
            request.startTime(),
            request.frequency()
        );

        List<Appointment> appointments = new ArrayList<>();
        for (LocalDateTime occurrence : occurrences) {
            Appointment appointment = Appointment.builder()
                .patient(patient)
                .sessionType(sessionType)
                .recurringSeries(series)
                .startDateTime(occurrence)
                .endDateTime(occurrence.plusMinutes(sessionType.getDurationMinutes()))
                .status(AppointmentStatus.CONFIRMED)
                .cancellationToken(UUID.randomUUID().toString())
                .build();

            appointment = appointmentRepository.save(appointment);

            // Create payment for each appointment
            Payment payment = Payment.builder()
                .appointment(appointment)
                .status(PaymentStatus.UNPAID)
                .amount(sessionType.getPrice())
                .build();
            paymentRepository.save(payment);

            appointments.add(appointment);
        }

        log.info("Created recurring series {} with {} appointments", series.getId(), appointments.size());
        auditService.logCreate("RecurringSeries", series.getId(), series);

        List<AppointmentResponse> appointmentResponses = appointments.stream()
            .map(AppointmentResponse::from)
            .collect(Collectors.toList());

        return RecurringSeriesResponse.fromWithAppointments(series, appointmentResponses);
    }

    /**
     * Cancel a single occurrence from a series
     */
    public AppointmentResponse cancelOccurrence(UUID appointmentId, String reason) {
        Appointment appointment = appointmentRepository.findByIdWithDetails(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Agendamento", "id", appointmentId));

        if (appointment.getRecurringSeries() == null) {
            throw new ValidationException("Este agendamento não faz parte de uma série recorrente");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.setCancelledAt(LocalDateTime.now());
        appointment.setCancelledBy("ADMIN");
        appointment.setCancellationReason(reason);

        appointment = appointmentRepository.save(appointment);
        log.info("Cancelled occurrence {} from series {}", appointmentId, appointment.getRecurringSeries().getId());

        auditService.logUpdate("Appointment", appointmentId, "CONFIRMED", "CANCELLED");

        return AppointmentResponse.from(appointment);
    }

    /**
     * Cancel entire series (future appointments only)
     */
    public void cancelSeries(UUID seriesId, String reason) {
        RecurringSeries series = recurringSeriesRepository.findById(seriesId)
            .orElseThrow(() -> new ResourceNotFoundException("Série recorrente", "id", seriesId));

        // Cancel all future appointments
        List<Appointment> futureAppointments = appointmentRepository.findFutureAppointmentsBySeries(
            seriesId, LocalDateTime.now());

        for (Appointment appointment : futureAppointments) {
            appointment.setStatus(AppointmentStatus.CANCELLED);
            appointment.setCancelledAt(LocalDateTime.now());
            appointment.setCancelledBy("ADMIN");
            appointment.setCancellationReason(reason != null ? reason : "Série cancelada");
            appointmentRepository.save(appointment);
        }

        // Deactivate the series
        series.setActive(false);
        recurringSeriesRepository.save(series);

        log.info("Cancelled series {} with {} future appointments", seriesId, futureAppointments.size());
        auditService.logUpdate("RecurringSeries", seriesId, "active: true", "active: false");
    }

    /**
     * Generate occurrence dates for a recurring series
     */
    public List<LocalDateTime> generateOccurrences(LocalDate startDate, LocalDate endDate,
                                                    java.time.DayOfWeek dayOfWeek,
                                                    java.time.LocalTime startTime,
                                                    RecurrenceFrequency frequency) {
        List<LocalDateTime> occurrences = new ArrayList<>();

        // Calculate the end of the window
        LocalDate maxDate = LocalDate.now().plusDays(maxAdvanceDays);
        LocalDate effectiveEndDate = endDate != null && endDate.isBefore(maxDate) ? endDate : maxDate;

        // Find the first occurrence on or after startDate
        LocalDate current = startDate;
        while (current.getDayOfWeek() != dayOfWeek) {
            current = current.plusDays(1);
        }

        // Calculate interval based on frequency
        int daysInterval = frequency == RecurrenceFrequency.WEEKLY ? 7 : 14;

        // Generate occurrences
        while (!current.isAfter(effectiveEndDate)) {
            occurrences.add(LocalDateTime.of(current, startTime));
            current = current.plusDays(daysInterval);
        }

        return occurrences;
    }

    private void validateRecurrenceRequest(RecurringSeriesRequest request) {
        if (request.startDate().isBefore(LocalDate.now())) {
            throw new ValidationException("Data de início não pode ser no passado");
        }

        if (request.endDate() != null && request.endDate().isBefore(request.startDate())) {
            throw new ValidationException("Data de término deve ser posterior à data de início");
        }

        // Check if the time is within availability for the day
        List<Availability> availabilities = availabilityRepository.findByDayOfWeekAndActiveTrue(request.dayOfWeek());
        if (availabilities.isEmpty()) {
            throw new ValidationException("Não há expediente configurado para " + request.dayOfWeek());
        }

        SessionType sessionType = sessionTypeRepository.findById(request.sessionTypeId())
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de sessão", "id", request.sessionTypeId()));

        java.time.LocalTime endTime = request.startTime().plusMinutes(sessionType.getDurationMinutes());

        boolean withinAvailability = availabilities.stream()
            .anyMatch(av -> !request.startTime().isBefore(av.getStartTime()) &&
                           !endTime.isAfter(av.getEndTime()));

        if (!withinAvailability) {
            throw new ValidationException("O horário selecionado está fora do expediente configurado");
        }
    }
}
