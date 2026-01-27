package com.psicoagenda.api.controller;

import com.psicoagenda.application.dto.request.BookingRequest;
import com.psicoagenda.application.dto.request.CancelAppointmentRequest;
import com.psicoagenda.application.dto.request.RescheduleRequest;
import com.psicoagenda.application.dto.response.*;
import com.psicoagenda.application.service.AppointmentService;
import com.psicoagenda.application.service.AvailabilityService;
import com.psicoagenda.application.service.ConsentService;
import com.psicoagenda.application.service.SessionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Agendamento Público", description = "Endpoints públicos para agendamento")
public class PublicBookingController {

    private final AppointmentService appointmentService;
    private final SessionTypeService sessionTypeService;
    private final AvailabilityService availabilityService;
    private final ConsentService consentService;

    public PublicBookingController(AppointmentService appointmentService,
                                   SessionTypeService sessionTypeService,
                                   AvailabilityService availabilityService,
                                   ConsentService consentService) {
        this.appointmentService = appointmentService;
        this.sessionTypeService = sessionTypeService;
        this.availabilityService = availabilityService;
        this.consentService = consentService;
    }

    @GetMapping("/session-types")
    @Operation(summary = "Listar tipos de sessão", description = "Retorna os tipos de sessão disponíveis para agendamento")
    public ResponseEntity<List<SessionTypeResponse>> getSessionTypes() {
        return ResponseEntity.ok(sessionTypeService.getActiveSessionTypes());
    }

    @GetMapping("/slots")
    @Operation(summary = "Horários disponíveis", description = "Retorna os horários disponíveis para uma data")
    public ResponseEntity<AvailableSlotResponse> getAvailableSlots(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @RequestParam UUID sessionTypeId
    ) {
        SessionTypeResponse sessionType = sessionTypeService.getSessionTypeById(sessionTypeId);
        return ResponseEntity.ok(availabilityService.getAvailableSlotsForDate(date, sessionType.durationMinutes()));
    }

    @GetMapping("/slots/range")
    @Operation(summary = "Horários disponíveis em período", description = "Retorna os horários disponíveis em um período")
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlotsRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
        @RequestParam UUID sessionTypeId
    ) {
        SessionTypeResponse sessionType = sessionTypeService.getSessionTypeById(sessionTypeId);
        return ResponseEntity.ok(availabilityService.getAvailableSlotsForRange(startDate, endDate, sessionType.durationMinutes()));
    }

    @GetMapping("/consent")
    @Operation(summary = "Termo de consentimento", description = "Retorna a versão atual do termo de consentimento")
    public ResponseEntity<ConsentVersionResponse> getCurrentConsent() {
        return ResponseEntity.ok(consentService.getCurrentConsentVersion());
    }

    @PostMapping("/book")
    @Operation(summary = "Criar agendamento", description = "Cria um novo agendamento")
    public ResponseEntity<BookingConfirmationResponse> createBooking(
        @Valid @RequestBody BookingRequest request,
        HttpServletRequest httpRequest
    ) {
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        return ResponseEntity.ok(appointmentService.createPublicBooking(request, ipAddress, userAgent));
    }

    @GetMapping("/appointment")
    @Operation(summary = "Consultar agendamento", description = "Consulta um agendamento pelo token")
    public ResponseEntity<AppointmentResponse> getAppointmentByToken(@RequestParam String token) {
        return ResponseEntity.ok(appointmentService.getAppointmentByToken(token));
    }

    @PostMapping("/cancel")
    @Operation(summary = "Cancelar agendamento", description = "Cancela um agendamento pelo token")
    public ResponseEntity<Void> cancelAppointment(@Valid @RequestBody CancelAppointmentRequest request) {
        appointmentService.cancelByToken(request.cancellationToken(), request.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reschedule")
    @Operation(summary = "Reagendar", description = "Reagenda um agendamento pelo token")
    public ResponseEntity<BookingConfirmationResponse> rescheduleAppointment(
        @Valid @RequestBody RescheduleRequest request
    ) {
        return ResponseEntity.ok(appointmentService.rescheduleByToken(request));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
