package com.psicoagenda.application.service;

import com.psicoagenda.application.dto.response.ConsentVersionResponse;
import com.psicoagenda.application.exception.ResourceNotFoundException;
import com.psicoagenda.domain.entity.Consent;
import com.psicoagenda.domain.entity.ConsentVersion;
import com.psicoagenda.domain.entity.Patient;
import com.psicoagenda.domain.repository.ConsentRepository;
import com.psicoagenda.domain.repository.ConsentVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class ConsentService {

    private static final Logger log = LoggerFactory.getLogger(ConsentService.class);

    private final ConsentRepository consentRepository;
    private final ConsentVersionRepository consentVersionRepository;

    public ConsentService(ConsentRepository consentRepository,
                          ConsentVersionRepository consentVersionRepository) {
        this.consentRepository = consentRepository;
        this.consentVersionRepository = consentVersionRepository;
    }

    @Transactional(readOnly = true)
    public ConsentVersionResponse getCurrentConsentVersion() {
        ConsentVersion version = consentVersionRepository.findCurrentVersion()
            .orElseThrow(() -> new ResourceNotFoundException("Versão do termo", "status", "ativa"));
        return ConsentVersionResponse.from(version);
    }

    @Transactional(readOnly = true)
    public ConsentVersionResponse getConsentVersionByVersion(String version) {
        ConsentVersion consentVersion = consentVersionRepository.findByVersion(version)
            .orElseThrow(() -> new ResourceNotFoundException("Versão do termo", "version", version));
        return ConsentVersionResponse.from(consentVersion);
    }

    public void recordConsent(Patient patient, String version, String ipAddress, String userAgent) {
        // Check if this version was already accepted
        if (consentRepository.existsByPatientIdAndVersion(patient.getId(), version)) {
            log.debug("Patient {} already accepted consent version {}", patient.getId(), version);
            return;
        }

        Consent consent = Consent.builder()
            .patient(patient)
            .version(version)
            .acceptedAt(LocalDateTime.now())
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build();

        consentRepository.save(consent);
        log.info("Recorded consent for patient {} version {}", patient.getId(), version);
    }

    public boolean hasAcceptedCurrentVersion(Patient patient) {
        return consentVersionRepository.findCurrentVersion()
            .map(current -> consentRepository.existsByPatientIdAndVersion(patient.getId(), current.getVersion()))
            .orElse(false);
    }
}
