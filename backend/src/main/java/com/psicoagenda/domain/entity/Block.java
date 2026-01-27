package com.psicoagenda.domain.entity;

import com.psicoagenda.domain.enums.BlockType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "block")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Block extends BaseEntity {

    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false)
    private BlockType blockType;

    @Column(length = 500)
    private String reason;
}
