package com.psicoagenda.application.exception;

import java.time.LocalDateTime;
import java.util.List;

public class ConflictException extends RuntimeException {

    private final List<LocalDateTime> conflictingDates;

    public ConflictException(String message) {
        super(message);
        this.conflictingDates = null;
    }

    public ConflictException(String message, List<LocalDateTime> conflictingDates) {
        super(message);
        this.conflictingDates = conflictingDates;
    }

    public List<LocalDateTime> getConflictingDates() {
        return conflictingDates;
    }
}
