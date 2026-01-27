package com.psicoagenda.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "consent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private String version;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;
}
