package com.psicoagenda.infrastructure.config;

import com.psicoagenda.domain.entity.AdminUser;
import com.psicoagenda.domain.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-email}")
    private String adminEmail;

    @Value("${app.admin.default-password}")
    private String adminPassword;

    @Value("${app.admin.default-name}")
    private String adminName;

    public DataInitializer(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createDefaultAdminIfNotExists();
    }

    private void createDefaultAdminIfNotExists() {
        if (!adminUserRepository.existsByEmail(adminEmail)) {
            AdminUser admin = AdminUser.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .name(adminName)
                .active(true)
                .build();

            adminUserRepository.save(admin);
            log.info("Default admin user created: {}", adminEmail);
        } else {
            log.debug("Admin user already exists: {}", adminEmail);
        }
    }
}
