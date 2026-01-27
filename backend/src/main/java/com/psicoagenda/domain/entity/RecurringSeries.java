package com.psicoagenda.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.psicoagenda.domain.enums.DayOfWeekEnum;
import com.psicoagenda.domain.enums.RecurrenceFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recurring_series")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurringSeries extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_type_id", nullable = false)
    private SessionType sessionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeekEnum dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecurrenceFrequency frequency;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @JsonIgnore
    @OneToMany(mappedBy = "recurringSeries", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();
}
