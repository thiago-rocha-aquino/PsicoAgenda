package com.psicoagenda.api.controller;

import com.psicoagenda.application.dto.request.RecurringSeriesRequest;
import com.psicoagenda.application.dto.response.AppointmentResponse;
import com.psicoagenda.application.dto.response.ConflictCheckResponse;
import com.psicoagenda.application.dto.response.RecurringSeriesResponse;
import com.psicoagenda.application.service.RecurrenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/recurrence")
@Tag(name = "Admin - Recorrência", description = "Gerenciamento de séries recorrentes")
public class AdminRecurrenceController {

    private final RecurrenceService recurrenceService;

    public AdminRecurrenceController(RecurrenceService recurrenceService) {
        this.recurrenceService = recurrenceService;
    }

    @GetMapping
    @Operation(summary = "Listar séries", description = "Lista todas as séries recorrentes ativas")
    public ResponseEntity<List<RecurringSeriesResponse>> getAllSeries() {
        return ResponseEntity.ok(recurrenceService.getAllActiveSeries());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe da série", description = "Retorna detalhes de uma série com suas ocorrências")
    public ResponseEntity<RecurringSeriesResponse> getSeries(@PathVariable UUID id) {
        return ResponseEntity.ok(recurrenceService.getSeriesById(id));
    }

    @PostMapping("/check-conflicts")
    @Operation(summary = "Verificar conflitos", description = "Verifica conflitos antes de criar uma série")
    public ResponseEntity<ConflictCheckResponse> checkConflicts(
        @Valid @RequestBody RecurringSeriesRequest request
    ) {
        return ResponseEntity.ok(recurrenceService.checkConflicts(request));
    }

    @PostMapping
    @Operation(summary = "Criar série", description = "Cria uma nova série recorrente com todas as ocorrências")
    public ResponseEntity<RecurringSeriesResponse> createSeries(
        @Valid @RequestBody RecurringSeriesRequest request
    ) {
        return ResponseEntity.ok(recurrenceService.createSeries(request));
    }

    @DeleteMapping("/{id}/occurrence/{appointmentId}")
    @Operation(summary = "Cancelar ocorrência", description = "Cancela uma única ocorrência da série")
    public ResponseEntity<AppointmentResponse> cancelOccurrence(
        @PathVariable UUID id,
        @PathVariable UUID appointmentId,
        @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(recurrenceService.cancelOccurrence(appointmentId, reason));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar série", description = "Cancela toda a série (apenas ocorrências futuras)")
    public ResponseEntity<Void> cancelSeries(
        @PathVariable UUID id,
        @RequestParam(required = false) String reason
    ) {
        recurrenceService.cancelSeries(id, reason);
        return ResponseEntity.noContent().build();
    }
}
