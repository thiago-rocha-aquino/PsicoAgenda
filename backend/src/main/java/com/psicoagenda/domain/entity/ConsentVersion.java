package com.psicoagenda.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consent_version")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentVersion extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String version;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
