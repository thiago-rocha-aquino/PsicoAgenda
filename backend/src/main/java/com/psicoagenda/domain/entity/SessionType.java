package com.psicoagenda.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "session_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionType extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
}
