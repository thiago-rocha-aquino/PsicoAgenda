package com.psicoagenda.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record ConflictCheckResponse(
    boolean hasConflicts,
    List<ConflictDetail> conflicts,
    int totalOccurrences,
    int conflictCount
) {
    public record ConflictDetail(
        LocalDateTime dateTime,
        String reason
    ) {}
}
