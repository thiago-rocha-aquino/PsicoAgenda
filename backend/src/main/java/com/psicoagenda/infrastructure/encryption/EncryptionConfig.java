package com.psicoagenda.infrastructure.encryption;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfig {

    private final EncryptionService encryptionService;

    public EncryptionConfig(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @PostConstruct
    public void init() {
        // Inject encryption service into the JPA converter
        EncryptedStringConverter.setEncryptionService(encryptionService);
    }
}
