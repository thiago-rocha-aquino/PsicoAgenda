package com.psicoagenda.domain.entity;

import com.psicoagenda.domain.enums.DayOfWeekEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Availability extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeekEnum dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
