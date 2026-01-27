package com.psicoagenda.unit;

import com.psicoagenda.application.service.AvailabilityService;
import com.psicoagenda.domain.entity.Appointment;
import com.psicoagenda.domain.entity.Availability;
import com.psicoagenda.domain.entity.Block;
import com.psicoagenda.domain.enums.DayOfWeekEnum;
import com.psicoagenda.domain.repository.AppointmentRepository;
import com.psicoagenda.domain.repository.AvailabilityRepository;
import com.psicoagenda.domain.repository.BlockRepository;
import com.psicoagenda.infrastructure.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AvailabilityServiceTest {

    @InjectMocks
    private AvailabilityService availabilityService;

    @Mock
    private AvailabilityRepository availabilityRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private BlockRepository blockRepository;

    @Mock
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(availabilityService, "slotMinutes", 15);
        ReflectionTestUtils.setField(availabilityService, "minAdvanceHours", 12);
        ReflectionTestUtils.setField(availabilityService, "maxAdvanceDays", 90);
    }

    @Test
    void isSlotAvailable_ValidSlot_ShouldReturnTrue() {
        // Arrange
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0);
        int duration = 50;

        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.fromJavaDayOfWeek(futureDateTime.getDayOfWeek());

        Availability availability = Availability.builder()
            .dayOfWeek(dayOfWeek)
            .startTime(LocalTime.of(8, 0))
            .endTime(LocalTime.of(18, 0))
            .active(true)
            .build();

        when(availabilityRepository.findByDayOfWeekAndActiveTrue(dayOfWeek))
            .thenReturn(List.of(availability));
        when(appointmentRepository.existsOverlappingAppointment(any(), any()))
            .thenReturn(false);
        when(blockRepository.existsBlockOverlapping(any(), any()))
            .thenReturn(false);

        // Act
        boolean result = availabilityService.isSlotAvailable(futureDateTime, duration);

        // Assert
        assertTrue(result);
    }

    @Test
    void isSlotAvailable_SlotTooSoon_ShouldReturnFalse() {
        // Arrange - slot is only 1 hour from now (less than 12h minimum)
        LocalDateTime soonDateTime = LocalDateTime.now().plusHours(1);
        int duration = 50;

        // Act
        boolean result = availabilityService.isSlotAvailable(soonDateTime, duration);

        // Assert
        assertFalse(result);
    }

    @Test
    void isSlotAvailable_SlotTooFar_ShouldReturnFalse() {
        // Arrange - slot is 100 days from now (more than 90 days max)
        LocalDateTime farDateTime = LocalDateTime.now().plusDays(100);
        int duration = 50;

        // Act
        boolean result = availabilityService.isSlotAvailable(farDateTime, duration);

        // Assert
        assertFalse(result);
    }

    @Test
    void isSlotAvailable_OutsideAvailability_ShouldReturnFalse() {
        // Arrange
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(7).withHour(20).withMinute(0);
        int duration = 50;

        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.fromJavaDayOfWeek(futureDateTime.getDayOfWeek());

        Availability availability = Availability.builder()
            .dayOfWeek(dayOfWeek)
            .startTime(LocalTime.of(8, 0))
            .endTime(LocalTime.of(18, 0))
            .active(true)
            .build();

        when(availabilityRepository.findByDayOfWeekAndActiveTrue(dayOfWeek))
            .thenReturn(List.of(availability));

        // Act
        boolean result = availabilityService.isSlotAvailable(futureDateTime, duration);

        // Assert
        assertFalse(result);
    }

    @Test
    void isSlotAvailable_ConflictWithAppointment_ShouldReturnFalse() {
        // Arrange
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0);
        int duration = 50;

        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.fromJavaDayOfWeek(futureDateTime.getDayOfWeek());

        Availability availability = Availability.builder()
            .dayOfWeek(dayOfWeek)
            .startTime(LocalTime.of(8, 0))
            .endTime(LocalTime.of(18, 0))
            .active(true)
            .build();

        when(availabilityRepository.findByDayOfWeekAndActiveTrue(dayOfWeek))
            .thenReturn(List.of(availability));
        when(appointmentRepository.existsOverlappingAppointment(any(), any()))
            .thenReturn(true); // Conflict exists

        // Act
        boolean result = availabilityService.isSlotAvailable(futureDateTime, duration);

        // Assert
        assertFalse(result);
    }

    @Test
    void isSlotAvailable_ConflictWithBlock_ShouldReturnFalse() {
        // Arrange
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0);
        int duration = 50;

        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.fromJavaDayOfWeek(futureDateTime.getDayOfWeek());

        Availability availability = Availability.builder()
            .dayOfWeek(dayOfWeek)
            .startTime(LocalTime.of(8, 0))
            .endTime(LocalTime.of(18, 0))
            .active(true)
            .build();

        when(availabilityRepository.findByDayOfWeekAndActiveTrue(dayOfWeek))
            .thenReturn(List.of(availability));
        when(appointmentRepository.existsOverlappingAppointment(any(), any()))
            .thenReturn(false);
        when(blockRepository.existsBlockOverlapping(any(), any()))
            .thenReturn(true); // Block exists

        // Act
        boolean result = availabilityService.isSlotAvailable(futureDateTime, duration);

        // Assert
        assertFalse(result);
    }

    @Test
    void isSlotAvailable_NoAvailabilityForDay_ShouldReturnFalse() {
        // Arrange
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(7).withHour(10).withMinute(0);
        int duration = 50;

        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.fromJavaDayOfWeek(futureDateTime.getDayOfWeek());

        when(availabilityRepository.findByDayOfWeekAndActiveTrue(dayOfWeek))
            .thenReturn(Collections.emptyList()); // No availability configured

        // Act
        boolean result = availabilityService.isSlotAvailable(futureDateTime, duration);

        // Assert
        assertFalse(result);
    }
}
