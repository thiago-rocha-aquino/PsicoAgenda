package com.psicoagenda.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_login")
    private java.time.LocalDateTime lastLogin;
}
