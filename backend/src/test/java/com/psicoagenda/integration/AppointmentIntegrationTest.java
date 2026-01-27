package com.psicoagenda.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.psicoagenda.application.dto.request.BookingRequest;
import com.psicoagenda.domain.entity.SessionType;
import com.psicoagenda.domain.entity.Availability;
import com.psicoagenda.domain.entity.ConsentVersion;
import com.psicoagenda.domain.enums.DayOfWeekEnum;
import com.psicoagenda.domain.repository.SessionTypeRepository;
import com.psicoagenda.domain.repository.AvailabilityRepository;
import com.psicoagenda.domain.repository.ConsentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AppointmentIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("psicoagenda_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.notifications.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SessionTypeRepository sessionTypeRepository;

    @Autowired
    private AvailabilityRepository availabilityRepository;

    @Autowired
    private ConsentVersionRepository consentVersionRepository;

    private SessionType testSessionType;

    @BeforeEach
    void setUp() {
        // Create test session type
        testSessionType = sessionTypeRepository.findByActiveTrueOrderByDisplayOrderAsc()
            .stream()
            .findFirst()
            .orElseGet(() -> {
                SessionType st = SessionType.builder()
                    .name("Sessao Teste")
                    .durationMinutes(50)
                    .price(new BigDecimal("200.00"))
                    .active(true)
                    .build();
                return sessionTypeRepository.save(st);
            });

        // Ensure availability exists for test dates
        LocalDateTime testDate = LocalDateTime.now().plusDays(7);
        DayOfWeekEnum dayOfWeek = DayOfWeekEnum.fromJavaDayOfWeek(testDate.getDayOfWeek());

        if (availabilityRepository.findByDayOfWeekAndActiveTrue(dayOfWeek).isEmpty()) {
            Availability availability = Availability.builder()
                .dayOfWeek(dayOfWeek)
                .startTime(LocalTime.of(8, 0))
                .endTime(LocalTime.of(18, 0))
                .active(true)
                .build();
            availabilityRepository.save(availability);
        }

        // Ensure consent version exists
        if (consentVersionRepository.findCurrentVersion().isEmpty()) {
            ConsentVersion consent = ConsentVersion.builder()
                .version("1.0")
                .content("Termo de teste")
                .effectiveFrom(LocalDateTime.now().minusDays(1))
                .active(true)
                .build();
            consentVersionRepository.save(consent);
        }
    }

    @Test
    void getSessionTypes_ShouldReturnList() throws Exception {
        mockMvc.perform(get("/api/public/session-types"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getConsent_ShouldReturnCurrentVersion() throws Exception {
        mockMvc.perform(get("/api/public/consent"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.version").exists())
            .andExpect(jsonPath("$.content").exists());
    }

    @Test
    void createBooking_ValidRequest_ShouldSucceed() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now()
            .plusDays(7)
            .withHour(10)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

        BookingRequest request = new BookingRequest(
            testSessionType.getId(),
            appointmentTime,
            "Paciente Teste",
            "(11) 99999-9999",
            "teste@email.com",
            true,
            "1.0"
        );

        mockMvc.perform(post("/api/public/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.appointmentId").exists())
            .andExpect(jsonPath("$.patientName").value("Paciente Teste"))
            .andExpect(jsonPath("$.cancellationToken").exists());
    }

    @Test
    void createBooking_MissingName_ShouldReturnValidationError() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(7);

        BookingRequest request = new BookingRequest(
            testSessionType.getId(),
            appointmentTime,
            "", // Empty name
            "(11) 99999-9999",
            "teste@email.com",
            true,
            "1.0"
        );

        mockMvc.perform(post("/api/public/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void createBooking_NoConsent_ShouldReturnValidationError() throws Exception {
        LocalDateTime appointmentTime = LocalDateTime.now().plusDays(7);

        BookingRequest request = new BookingRequest(
            testSessionType.getId(),
            appointmentTime,
            "Paciente Teste",
            "(11) 99999-9999",
            "teste@email.com",
            false, // No consent
            "1.0"
        );

        mockMvc.perform(post("/api/public/book")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void getAppointmentByToken_InvalidToken_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/api/public/appointment")
                .param("token", "invalid-token"))
            .andExpect(status().isNotFound());
    }
}
