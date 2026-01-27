package com.psicoagenda.api.controller;

import com.psicoagenda.application.dto.request.AdminAppointmentRequest;
import com.psicoagenda.application.dto.request.UpdateAppointmentStatusRequest;
import com.psicoagenda.application.dto.response.AppointmentResponse;
import com.psicoagenda.application.service.AppointmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/appointments")
@Tag(name = "Admin - Agendamentos", description = "Gerenciamento de agendamentos")
public class AdminAppointmentController {

    private final AppointmentService appointmentService;

    public AdminAppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping
    @Operation(summary = "Listar agendamentos", description = "Lista agendamentos em um período")
    public ResponseEntity<List<AppointmentResponse>> getAppointments(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
        @RequestParam(defaultValue = "false") boolean includeAll
    ) {
        if (includeAll) {
            return ResponseEntity.ok(appointmentService.getAppointmentsInRange(start, end));
        }
        return ResponseEntity.ok(appointmentService.getActiveAppointmentsInRange(start, end));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Próximos agendamentos", description = "Lista os próximos agendamentos")
    public ResponseEntity<List<AppointmentResponse>> getUpcomingAppointments(
        @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(appointmentService.getUpcomingAppointments(limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do agendamento", description = "Retorna detalhes de um agendamento")
    public ResponseEntity<AppointmentResponse> getAppointment(@PathVariable UUID id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @GetMapping("/patient/{patientId}")
    @Operation(summary = "Agendamentos do paciente", description = "Lista agendamentos de um paciente")
    public ResponseEntity<List<AppointmentResponse>> getPatientAppointments(@PathVariable UUID patientId) {
        return ResponseEntity.ok(appointmentService.getPatientAppointments(patientId));
    }

    @PostMapping
    @Operation(summary = "Criar agendamento", description = "Cria um novo agendamento (admin)")
    public ResponseEntity<AppointmentResponse> createAppointment(
        @Valid @RequestBody AdminAppointmentRequest request
    ) {
        return ResponseEntity.ok(appointmentService.createAdminAppointment(request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status", description = "Atualiza o status de um agendamento")
    public ResponseEntity<AppointmentResponse> updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateAppointmentStatusRequest request
    ) {
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, request));
    }
}
