package com.psicoagenda.api.controller;

import com.psicoagenda.application.dto.response.PatientResponse;
import com.psicoagenda.application.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/patients")
@Tag(name = "Admin - Pacientes", description = "Gerenciamento de pacientes")
public class AdminPatientController {

    private final PatientService patientService;

    public AdminPatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(summary = "Listar pacientes", description = "Lista todos os pacientes ativos")
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        return ResponseEntity.ok(patientService.getAllActivePatients());
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar pacientes", description = "Busca pacientes por nome")
    public ResponseEntity<List<PatientResponse>> searchPatients(@RequestParam String name) {
        return ResponseEntity.ok(patientService.searchPatients(name));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do paciente", description = "Retorna detalhes de um paciente")
    public ResponseEntity<PatientResponse> getPatient(@PathVariable UUID id) {
        return ResponseEntity.ok(patientService.getPatientById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar paciente", description = "Atualiza dados de um paciente")
    public ResponseEntity<PatientResponse> updatePatient(
        @PathVariable UUID id,
        @RequestParam String name,
        @RequestParam String phone,
        @RequestParam(required = false) String email
    ) {
        return ResponseEntity.ok(patientService.updatePatient(id, name, phone, email));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar paciente", description = "Desativa um paciente")
    public ResponseEntity<Void> deactivatePatient(@PathVariable UUID id) {
        patientService.deactivatePatient(id);
        return ResponseEntity.noContent().build();
    }
}
