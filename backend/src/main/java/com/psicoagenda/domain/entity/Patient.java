package com.psicoagenda.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.psicoagenda.infrastructure.encryption.EncryptedStringConverter;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patient")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String phone;

    @Convert(converter = EncryptedStringConverter.class)
    private String email;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "anonymized")
    @Builder.Default
    private boolean anonymized = false;

    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecurringSeries> recurringSeries = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Consent> consents = new ArrayList<>();
}
