package com.psicoagenda.infrastructure.scheduler;

import com.psicoagenda.domain.entity.Patient;
import com.psicoagenda.domain.repository.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RetentionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RetentionScheduler.class);

    private final PatientRepository patientRepository;

    @Value("${app.retention.enabled}")
    private boolean retentionEnabled;

    @Value("${app.retention.months}")
    private int retentionMonths;

    public RetentionScheduler(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    /**
     * Anonymize patient data after retention period.
     * Runs daily at 3 AM.
     * This job only ANONYMIZES data, it does not DELETE.
     */
    @Scheduled(cron = "0 0 3 * * *") // Daily at 3 AM
    @Transactional
    public void anonymizeOldPatientData() {
        if (!retentionEnabled) {
            log.debug("Retention job is disabled");
            return;
        }

        log.info("Running retention/anonymization job");

        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(retentionMonths);

        // Find patients with no appointments after cutoff date and not already anonymized
        List<Patient> patientsToAnonymize = patientRepository.findPatientsForAnonymization(cutoffDate);

        int anonymizedCount = 0;
        for (Patient patient : patientsToAnonymize) {
            try {
                patientRepository.anonymizePatient(patient.getId());
                anonymizedCount++;
                log.info("Anonymized patient: {}", patient.getId());
            } catch (Exception e) {
                log.error("Failed to anonymize patient {}", patient.getId(), e);
            }
        }

        log.info("Retention job completed. Anonymized {} patients", anonymizedCount);
    }
}
