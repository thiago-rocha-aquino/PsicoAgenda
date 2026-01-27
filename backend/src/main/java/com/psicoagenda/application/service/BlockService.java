package com.psicoagenda.application.service;

import com.psicoagenda.application.dto.request.BlockRequest;
import com.psicoagenda.application.dto.response.BlockResponse;
import com.psicoagenda.application.exception.ConflictException;
import com.psicoagenda.application.exception.ResourceNotFoundException;
import com.psicoagenda.application.exception.ValidationException;
import com.psicoagenda.domain.entity.Block;
import com.psicoagenda.domain.repository.AppointmentRepository;
import com.psicoagenda.domain.repository.BlockRepository;
import com.psicoagenda.infrastructure.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class BlockService {

    private static final Logger log = LoggerFactory.getLogger(BlockService.class);

    private final BlockRepository blockRepository;
    private final AppointmentRepository appointmentRepository;
    private final AuditService auditService;

    public BlockService(BlockRepository blockRepository,
                        AppointmentRepository appointmentRepository,
                        AuditService auditService) {
        this.blockRepository = blockRepository;
        this.appointmentRepository = appointmentRepository;
        this.auditService = auditService;
    }

    public List<BlockResponse> getAllBlocks() {
        return blockRepository.findAll()
            .stream()
            .map(BlockResponse::from)
            .collect(Collectors.toList());
    }

    public List<BlockResponse> getFutureBlocks() {
        return blockRepository.findByStartDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime.now())
            .stream()
            .map(BlockResponse::from)
            .collect(Collectors.toList());
    }

    public List<BlockResponse> getBlocksInRange(LocalDateTime start, LocalDateTime end) {
        return blockRepository.findBlocksInRange(start, end)
            .stream()
            .map(BlockResponse::from)
            .collect(Collectors.toList());
    }

    public BlockResponse getBlockById(UUID id) {
        Block block = blockRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bloqueio", "id", id));
        return BlockResponse.from(block);
    }

    public BlockResponse createBlock(BlockRequest request) {
        validateBlockRequest(request);

        // Check for existing appointments in this period
        boolean hasAppointments = appointmentRepository.existsOverlappingAppointment(
            request.startDateTime(), request.endDateTime());

        if (hasAppointments) {
            throw new ConflictException("Existem agendamentos no período selecionado. Cancele-os antes de criar o bloqueio.");
        }

        Block block = Block.builder()
            .startDateTime(request.startDateTime())
            .endDateTime(request.endDateTime())
            .blockType(request.blockType())
            .reason(request.reason())
            .build();

        block = blockRepository.save(block);
        log.info("Created block: {} from {} to {}", request.blockType(), request.startDateTime(), request.endDateTime());

        auditService.logCreate("Block", block.getId(), block);

        return BlockResponse.from(block);
    }

    public BlockResponse updateBlock(UUID id, BlockRequest request) {
        Block block = blockRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bloqueio", "id", id));

        validateBlockRequest(request);

        Block oldState = cloneBlock(block);

        block.setStartDateTime(request.startDateTime());
        block.setEndDateTime(request.endDateTime());
        block.setBlockType(request.blockType());
        block.setReason(request.reason());

        block = blockRepository.save(block);
        log.info("Updated block: {}", id);

        auditService.logUpdate("Block", block.getId(), oldState, block);

        return BlockResponse.from(block);
    }

    public void deleteBlock(UUID id) {
        Block block = blockRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bloqueio", "id", id));

        blockRepository.delete(block);
        log.info("Deleted block: {}", id);

        auditService.logDelete("Block", id, block);
    }

    private void validateBlockRequest(BlockRequest request) {
        if (request.endDateTime().isBefore(request.startDateTime()) ||
            request.endDateTime().equals(request.startDateTime())) {
            throw new ValidationException("Data/hora de término deve ser posterior à data/hora de início");
        }
    }

    private Block cloneBlock(Block original) {
        return Block.builder()
            .startDateTime(original.getStartDateTime())
            .endDateTime(original.getEndDateTime())
            .blockType(original.getBlockType())
            .reason(original.getReason())
            .build();
    }
}
