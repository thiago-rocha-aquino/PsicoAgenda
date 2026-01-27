package com.psicoagenda.api.controller;

import com.psicoagenda.application.dto.request.SessionTypeRequest;
import com.psicoagenda.application.dto.response.SessionTypeResponse;
import com.psicoagenda.application.service.SessionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/session-types")
@Tag(name = "Admin - Tipos de Sessão", description = "Gerenciamento de tipos de sessão")
public class AdminSessionTypeController {

    private final SessionTypeService sessionTypeService;

    public AdminSessionTypeController(SessionTypeService sessionTypeService) {
        this.sessionTypeService = sessionTypeService;
    }

    @GetMapping
    @Operation(summary = "Listar tipos de sessão", description = "Lista todos os tipos de sessão")
    public ResponseEntity<List<SessionTypeResponse>> getAllSessionTypes() {
        return ResponseEntity.ok(sessionTypeService.getAllSessionTypes());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do tipo de sessão", description = "Retorna detalhes de um tipo de sessão")
    public ResponseEntity<SessionTypeResponse> getSessionType(@PathVariable UUID id) {
        return ResponseEntity.ok(sessionTypeService.getSessionTypeById(id));
    }

    @PostMapping
    @Operation(summary = "Criar tipo de sessão", description = "Cria um novo tipo de sessão")
    public ResponseEntity<SessionTypeResponse> createSessionType(
        @Valid @RequestBody SessionTypeRequest request
    ) {
        return ResponseEntity.ok(sessionTypeService.createSessionType(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar tipo de sessão", description = "Atualiza um tipo de sessão")
    public ResponseEntity<SessionTypeResponse> updateSessionType(
        @PathVariable UUID id,
        @Valid @RequestBody SessionTypeRequest request
    ) {
        return ResponseEntity.ok(sessionTypeService.updateSessionType(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desativar tipo de sessão", description = "Desativa um tipo de sessão")
    public ResponseEntity<Void> deleteSessionType(@PathVariable UUID id) {
        sessionTypeService.deleteSessionType(id);
        return ResponseEntity.noContent().build();
    }
}
