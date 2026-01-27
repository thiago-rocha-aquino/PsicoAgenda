package com.psicoagenda.api.controller;

import com.psicoagenda.application.dto.request.AvailabilityRequest;
import com.psicoagenda.application.dto.request.BlockRequest;
import com.psicoagenda.application.dto.response.AvailabilityResponse;
import com.psicoagenda.application.dto.response.BlockResponse;
import com.psicoagenda.application.service.AvailabilityService;
import com.psicoagenda.application.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/availability")
@Tag(name = "Admin - Disponibilidade", description = "Gerenciamento de disponibilidade e bloqueios")
public class AdminAvailabilityController {

    private final AvailabilityService availabilityService;
    private final BlockService blockService;

    public AdminAvailabilityController(AvailabilityService availabilityService, BlockService blockService) {
        this.availabilityService = availabilityService;
        this.blockService = blockService;
    }

    // ==================== Expediente ====================

    @GetMapping("/schedule")
    @Operation(summary = "Listar expediente", description = "Lista o expediente configurado")
    public ResponseEntity<List<AvailabilityResponse>> getSchedule() {
        return ResponseEntity.ok(availabilityService.getAllAvailabilities());
    }

    @PostMapping("/schedule")
    @Operation(summary = "Criar expediente", description = "Adiciona um período de expediente")
    public ResponseEntity<AvailabilityResponse> createAvailability(
        @Valid @RequestBody AvailabilityRequest request
    ) {
        return ResponseEntity.ok(availabilityService.createAvailability(request));
    }

    @PutMapping("/schedule/{id}")
    @Operation(summary = "Atualizar expediente", description = "Atualiza um período de expediente")
    public ResponseEntity<AvailabilityResponse> updateAvailability(
        @PathVariable UUID id,
        @Valid @RequestBody AvailabilityRequest request
    ) {
        return ResponseEntity.ok(availabilityService.updateAvailability(id, request));
    }

    @DeleteMapping("/schedule/{id}")
    @Operation(summary = "Remover expediente", description = "Remove um período de expediente")
    public ResponseEntity<Void> deleteAvailability(@PathVariable UUID id) {
        availabilityService.deleteAvailability(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Bloqueios ====================

    @GetMapping("/blocks")
    @Operation(summary = "Listar bloqueios", description = "Lista bloqueios futuros")
    public ResponseEntity<List<BlockResponse>> getBlocks() {
        return ResponseEntity.ok(blockService.getFutureBlocks());
    }

    @GetMapping("/blocks/{id}")
    @Operation(summary = "Detalhe do bloqueio", description = "Retorna detalhes de um bloqueio")
    public ResponseEntity<BlockResponse> getBlock(@PathVariable UUID id) {
        return ResponseEntity.ok(blockService.getBlockById(id));
    }

    @PostMapping("/blocks")
    @Operation(summary = "Criar bloqueio", description = "Cria um novo bloqueio (férias, feriado, folga)")
    public ResponseEntity<BlockResponse> createBlock(@Valid @RequestBody BlockRequest request) {
        return ResponseEntity.ok(blockService.createBlock(request));
    }

    @PutMapping("/blocks/{id}")
    @Operation(summary = "Atualizar bloqueio", description = "Atualiza um bloqueio")
    public ResponseEntity<BlockResponse> updateBlock(
        @PathVariable UUID id,
        @Valid @RequestBody BlockRequest request
    ) {
        return ResponseEntity.ok(blockService.updateBlock(id, request));
    }

    @DeleteMapping("/blocks/{id}")
    @Operation(summary = "Remover bloqueio", description = "Remove um bloqueio")
    public ResponseEntity<Void> deleteBlock(@PathVariable UUID id) {
        blockService.deleteBlock(id);
        return ResponseEntity.noContent().build();
    }
}
