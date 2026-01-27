package com.psicoagenda.application.service;

import com.psicoagenda.application.dto.request.AvailabilityRequest;
import com.psicoagenda.application.dto.response.AvailabilityResponse;
import com.psicoagenda.application.dto.response.AvailableSlotResponse;
import com.psicoagenda.application.exception.ResourceNotFoundException;
import com.psicoagenda.application.exception.ValidationException;
import com.psicoagenda.domain.entity.Availability;
import com.psicoagenda.domain.entity.Block;
import com.psicoagenda.domain.entity.Appointment;
import com.psicoagenda.domain.enums.DayOfWeekEnum;
import com.psicoagenda.domain.repository.AvailabilityRepository;
import com.psicoagenda.domain.repository.AppointmentRepository;
import com.psicoagenda.domain.repository.BlockRepository;
import com.psicoagenda.infrastructure.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AvailabilityService {

    private static final Logger log = LoggerFactory.getLogger(AvailabilityService.class);

    private final AvailabilityRepository availabilityRepository;
    private final AppointmentRepository appointmentRepository;
    private final BlockRepository blockRepository;
    private final AuditService auditService;

    @Value("${app.booking.slot-minutes:15}")
    private int slotMinutes;

    @Value("${app.booking.min-advance-hours:12}")
    private int minAdvanceHours;

    @Value("${app.booking.max-advance-days:90}")
    private int maxAdvanceDays;

    public AvailabilityService(AvailabilityRepository availabilityRepository,
                               AppointmentRepository appointmentRepository,
                               BlockRepository blockRepository,
                               AuditService auditService) {
        this.availabilityRepository = availabilityRepository;
        this.appointmentRepository = appointmentRepository;
        this.blockRepository = blockRepository;
        this.auditService = auditService;
    }

    public List<AvailabilityResponse> getAllAvailabilities() {
        return availabilityRepository.findAllByOrderByDayOfWeekAscStartTimeAsc()
            .stream()
            .map(AvailabilityResponse::from)
            .collect(Collectors.toList());
    }

    public List<AvailabilityResponse> getActiveAvailabilities() {
        return availabilityRepository.findByActiveTrue()
            .stream()
            .map(AvailabilityResponse::from)
            .collect(Collectors.toList());
    }

    public AvailabilityResponse createAvailability(AvailabilityRequest request) {
        validateTimeRange(request.startTime(), request.endTime());

        Availability availability = Availability.builder()
            .dayOfWeek(request.dayOfWeek())
            .startTime(request.startTime())
            .endTime(request.endTime())
            .active(request.active() != null ? request.active() : true)
            .build();

        availability = availabilityRepository.save(availability);
        log.info("Created availability: {} {} - {}", request.dayOfWeek(), request.startTime(), request.endTime());

        auditService.logCreate("Availability", availability.getId(), availability);

        return AvailabilityResponse.from(availability);
    }

    public AvailabilityResponse updateAvailability(UUID id, AvailabilityRequest request) {
        Availability availability = availabilityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Disponibilidade", "id", id));

        validateTimeRange(request.startTime(), request.endTime());

        Availability oldState = cloneAvailability(availability);

        availability.setDayOfWeek(request.dayOfWeek());
        availability.setStartTime(request.startTime());
        availability.setEndTime(request.endTime());
        if (request.active() != null) {
            availability.setActive(request.active());
        }

        availability = availabilityRepository.save(availability);
        log.info("Updated availability: {}", id);

        auditService.logUpdate("Availability", availability.getId(), oldState, availability);

        return AvailabilityResponse.from(availability);
    }

    public void deleteAvailability(UUID id) {
        Availability availability = availabilityRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Disponibilidade", "id", id));

        availabilityRepository.delete(availability);
        log.info("Deleted availability: {}", id);

        auditService.logDelete("Availability", id, availability);
    }

    /**
     * Get available slots for a specific date and session duration
     */
    public AvailableSlotResponse getAvailableSlotsForDate(LocalDate date, int durationMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate minDate = now.plusHours(minAdvanceHours).toLocalDate();
        LocalDate maxDate = now.plusDays(maxAdvanceDays).toLocalDate();

        if (date.isBefore(minDate) || date.isAfter(maxDate)) {
            return new AvailableSlotResponse(date, Collections.emptyList());
        }

        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.fromJavaDayOfWeek(date.getDayOfWeek());
        List<Availability> availabilities = availabilityRepository.findByDayOfWeekAndActiveTrue(dayOfWeek);

        if (availabilities.isEmpty()) {
            return new AvailableSlotResponse(date, Collections.emptyList());
        }

        // Get all appointments for the day
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Appointment> appointments = appointmentRepository.findAppointmentsInRange(dayStart, dayEnd);

        // Get blocks for the day
        List<Block> blocks = blockRepository.findBlocksInRange(dayStart, dayEnd);

        // Generate slots
        List<AvailableSlotResponse.TimeSlot> slots = new ArrayList<>();

        for (Availability availability : availabilities) {
            LocalTime currentTime = availability.getStartTime();
            LocalTime endTime = availability.getEndTime();

            while (currentTime.plusMinutes(durationMinutes).compareTo(endTime) <= 0) {
                LocalDateTime slotStart = LocalDateTime.of(date, currentTime);
                LocalDateTime slotEnd = slotStart.plusMinutes(durationMinutes);

                // Check if slot is in the future with minimum advance
                boolean isFutureEnough = slotStart.isAfter(now.plusHours(minAdvanceHours));

                // Check if slot conflicts with existing appointments
                boolean conflictsWithAppointment = appointments.stream()
                    .anyMatch(apt -> apt.getStartDateTime().isBefore(slotEnd) && apt.getEndDateTime().isAfter(slotStart));

                // Check if slot conflicts with blocks
                boolean conflictsWithBlock = blocks.stream()
                    .anyMatch(block -> block.getStartDateTime().isBefore(slotEnd) && block.getEndDateTime().isAfter(slotStart));

                boolean available = isFutureEnough && !conflictsWithAppointment && !conflictsWithBlock;

                slots.add(new AvailableSlotResponse.TimeSlot(currentTime, slotStart, available));

                currentTime = currentTime.plusMinutes(slotMinutes);
            }
        }

        return new AvailableSlotResponse(date, slots);
    }

    /**
     * Get available slots for a date range
     */
    public List<AvailableSlotResponse> getAvailableSlotsForRange(LocalDate startDate, LocalDate endDate, int durationMinutes) {
        List<AvailableSlotResponse> result = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            AvailableSlotResponse daySlots = getAvailableSlotsForDate(current, durationMinutes);
            if (!daySlots.slots().isEmpty()) {
                result.add(daySlots);
            }
            current = current.plusDays(1);
        }

        return result;
    }

    /**
     * Check if a specific datetime is available for the given duration
     */
    public boolean isSlotAvailable(LocalDateTime startDateTime, int durationMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);

        // Check minimum advance time
        if (startDateTime.isBefore(now.plusHours(minAdvanceHours))) {
            return false;
        }

        // Check maximum advance time
        if (startDateTime.isAfter(now.plusDays(maxAdvanceDays))) {
            return false;
        }

        // Check if within availability hours
        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.fromJavaDayOfWeek(startDateTime.getDayOfWeek());
        List<Availability> availabilities = availabilityRepository.findByDayOfWeekAndActiveTrue(dayOfWeek);

        boolean withinAvailability = availabilities.stream()
            .anyMatch(av -> !startDateTime.toLocalTime().isBefore(av.getStartTime()) &&
                           !endDateTime.toLocalTime().isAfter(av.getEndTime()));

        if (!withinAvailability) {
            return false;
        }

        // Check for conflicts with appointments
        if (appointmentRepository.existsOverlappingAppointment(startDateTime, endDateTime)) {
            return false;
        }

        // Check for conflicts with blocks
        if (blockRepository.existsBlockOverlapping(startDateTime, endDateTime)) {
            return false;
        }

        return true;
    }

    /**
     * Check if a slot is available excluding a specific appointment (for rescheduling)
     */
    public boolean isSlotAvailableExcluding(LocalDateTime startDateTime, int durationMinutes, UUID excludeAppointmentId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDateTime = startDateTime.plusMinutes(durationMinutes);

        // Check minimum advance time
        if (startDateTime.isBefore(now.plusHours(minAdvanceHours))) {
            return false;
        }

        // Check maximum advance time
        if (startDateTime.isAfter(now.plusDays(maxAdvanceDays))) {
            return false;
        }

        // Check if within availability hours
        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.fromJavaDayOfWeek(startDateTime.getDayOfWeek());
        List<Availability> availabilities = availabilityRepository.findByDayOfWeekAndActiveTrue(dayOfWeek);

        boolean withinAvailability = availabilities.stream()
            .anyMatch(av -> !startDateTime.toLocalTime().isBefore(av.getStartTime()) &&
                           !endDateTime.toLocalTime().isAfter(av.getEndTime()));

        if (!withinAvailability) {
            return false;
        }

        // Check for conflicts with appointments (excluding the current one)
        if (appointmentRepository.existsOverlappingAppointmentExcluding(startDateTime, endDateTime, excludeAppointmentId)) {
            return false;
        }

        // Check for conflicts with blocks
        if (blockRepository.existsBlockOverlapping(startDateTime, endDateTime)) {
            return false;
        }

        return true;
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new ValidationException("Hora de início deve ser anterior à hora de término");
        }
    }

    private Availability cloneAvailability(Availability original) {
        return Availability.builder()
            .dayOfWeek(original.getDayOfWeek())
            .startTime(original.getStartTime())
            .endTime(original.getEndTime())
            .active(original.isActive())
            .build();
    }
}
