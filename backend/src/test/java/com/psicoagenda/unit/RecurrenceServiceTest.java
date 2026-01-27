package com.psicoagenda.unit;

import com.psicoagenda.application.service.RecurrenceService;
import com.psicoagenda.domain.enums.RecurrenceFrequency;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RecurrenceServiceTest {

    @InjectMocks
    private RecurrenceService recurrenceService;

    @Mock
    private com.psicoagenda.domain.repository.RecurringSeriesRepository recurringSeriesRepository;
    @Mock
    private com.psicoagenda.domain.repository.AppointmentRepository appointmentRepository;
    @Mock
    private com.psicoagenda.domain.repository.SessionTypeRepository sessionTypeRepository;
    @Mock
    private com.psicoagenda.domain.repository.PatientRepository patientRepository;
    @Mock
    private com.psicoagenda.domain.repository.PaymentRepository paymentRepository;
    @Mock
    private com.psicoagenda.domain.repository.BlockRepository blockRepository;
    @Mock
    private com.psicoagenda.domain.repository.AvailabilityRepository availabilityRepository;
    @Mock
    private com.psicoagenda.application.service.PatientService patientService;
    @Mock
    private com.psicoagenda.infrastructure.audit.AuditService auditService;

    @Test
    void generateOccurrences_Weekly_ShouldGenerateCorrectDates() {
        // Arrange
        ReflectionTestUtils.setField(recurrenceService, "maxAdvanceDays", 90);

        LocalDate startDate = LocalDate.of(2024, 1, 8); // Monday
        LocalDate endDate = LocalDate.of(2024, 2, 5); // 4 weeks later
        DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
        LocalTime startTime = LocalTime.of(10, 0);
        RecurrenceFrequency frequency = RecurrenceFrequency.WEEKLY;

        // Act
        List<LocalDateTime> occurrences = recurrenceService.generateOccurrences(
            startDate, endDate, dayOfWeek, startTime, frequency);

        // Assert
        assertEquals(5, occurrences.size());
        assertEquals(LocalDateTime.of(2024, 1, 8, 10, 0), occurrences.get(0));
        assertEquals(LocalDateTime.of(2024, 1, 15, 10, 0), occurrences.get(1));
        assertEquals(LocalDateTime.of(2024, 1, 22, 10, 0), occurrences.get(2));
        assertEquals(LocalDateTime.of(2024, 1, 29, 10, 0), occurrences.get(3));
        assertEquals(LocalDateTime.of(2024, 2, 5, 10, 0), occurrences.get(4));
    }

    @Test
    void generateOccurrences_Biweekly_ShouldGenerateCorrectDates() {
        // Arrange
        ReflectionTestUtils.setField(recurrenceService, "maxAdvanceDays", 90);

        LocalDate startDate = LocalDate.of(2024, 1, 10); // Wednesday
        LocalDate endDate = LocalDate.of(2024, 2, 21);
        DayOfWeek dayOfWeek = DayOfWeek.WEDNESDAY;
        LocalTime startTime = LocalTime.of(14, 30);
        RecurrenceFrequency frequency = RecurrenceFrequency.BIWEEKLY;

        // Act
        List<LocalDateTime> occurrences = recurrenceService.generateOccurrences(
            startDate, endDate, dayOfWeek, startTime, frequency);

        // Assert
        assertEquals(4, occurrences.size());
        assertEquals(LocalDateTime.of(2024, 1, 10, 14, 30), occurrences.get(0));
        assertEquals(LocalDateTime.of(2024, 1, 24, 14, 30), occurrences.get(1));
        assertEquals(LocalDateTime.of(2024, 2, 7, 14, 30), occurrences.get(2));
        assertEquals(LocalDateTime.of(2024, 2, 21, 14, 30), occurrences.get(3));
    }

    @Test
    void generateOccurrences_StartDateNotOnDayOfWeek_ShouldFindFirstOccurrence() {
        // Arrange
        ReflectionTestUtils.setField(recurrenceService, "maxAdvanceDays", 90);

        LocalDate startDate = LocalDate.of(2024, 1, 8); // Monday
        LocalDate endDate = LocalDate.of(2024, 1, 31);
        DayOfWeek dayOfWeek = DayOfWeek.FRIDAY; // Looking for Fridays
        LocalTime startTime = LocalTime.of(9, 0);
        RecurrenceFrequency frequency = RecurrenceFrequency.WEEKLY;

        // Act
        List<LocalDateTime> occurrences = recurrenceService.generateOccurrences(
            startDate, endDate, dayOfWeek, startTime, frequency);

        // Assert
        assertEquals(4, occurrences.size());
        // First Friday after Jan 8 (Monday) is Jan 12
        assertEquals(LocalDateTime.of(2024, 1, 12, 9, 0), occurrences.get(0));
        assertEquals(LocalDateTime.of(2024, 1, 19, 9, 0), occurrences.get(1));
        assertEquals(LocalDateTime.of(2024, 1, 26, 9, 0), occurrences.get(2));
    }

    @Test
    void generateOccurrences_NoEndDate_ShouldRespectMaxAdvanceDays() {
        // Arrange
        ReflectionTestUtils.setField(recurrenceService, "maxAdvanceDays", 30);

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = null; // No end date
        DayOfWeek dayOfWeek = startDate.getDayOfWeek();
        LocalTime startTime = LocalTime.of(10, 0);
        RecurrenceFrequency frequency = RecurrenceFrequency.WEEKLY;

        // Act
        List<LocalDateTime> occurrences = recurrenceService.generateOccurrences(
            startDate, endDate, dayOfWeek, startTime, frequency);

        // Assert
        assertTrue(occurrences.size() >= 4 && occurrences.size() <= 5);
        // All occurrences should be within 30 days
        LocalDate maxDate = LocalDate.now().plusDays(30);
        for (LocalDateTime occurrence : occurrences) {
            assertFalse(occurrence.toLocalDate().isAfter(maxDate));
        }
    }

    @Test
    void generateOccurrences_EmptyRange_ShouldReturnEmptyList() {
        // Arrange
        ReflectionTestUtils.setField(recurrenceService, "maxAdvanceDays", 90);

        LocalDate startDate = LocalDate.of(2024, 1, 8);
        LocalDate endDate = LocalDate.of(2024, 1, 7); // End before start
        DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
        LocalTime startTime = LocalTime.of(10, 0);
        RecurrenceFrequency frequency = RecurrenceFrequency.WEEKLY;

        // Act
        List<LocalDateTime> occurrences = recurrenceService.generateOccurrences(
            startDate, endDate, dayOfWeek, startTime, frequency);

        // Assert
        assertTrue(occurrences.isEmpty());
    }
}
