package com.psicoagenda.application.dto.response;

import com.psicoagenda.domain.entity.Patient;

import java.time.LocalDateTime;
import java.util.UUID;

public record PatientResponse(
    UUID id,
    String name,
    String phone,
    String email,
    boolean active,
    boolean anonymized,
    LocalDateTime createdAt
) {
    public static PatientResponse from(Patient entity) {
        return new PatientResponse(
            entity.getId(),
            entity.getName(),
            entity.isAnonymized() ? "***" : entity.getPhone(),
            entity.isAnonymized() ? "***" : entity.getEmail(),
            entity.isActive(),
            entity.isAnonymized(),
            entity.getCreatedAt()
        );
    }

    public static PatientResponse fromPublic(Patient entity) {
        // For public responses, mask contact info
        return new PatientResponse(
            entity.getId(),
            entity.getName(),
            maskPhone(entity.getPhone()),
            maskEmail(entity.getEmail()),
            entity.isActive(),
            entity.isAnonymized(),
            entity.getCreatedAt()
        );
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "***";
        return "***" + phone.substring(phone.length() - 4);
    }

    private static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf("@");
        if (atIndex <= 2) return "***" + email.substring(atIndex);
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
