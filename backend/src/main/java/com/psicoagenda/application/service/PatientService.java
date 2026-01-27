package com.psicoagenda.application.service;

import com.psicoagenda.application.dto.response.PatientResponse;
import com.psicoagenda.application.exception.ResourceNotFoundException;
import com.psicoagenda.domain.entity.Patient;
import com.psicoagenda.domain.repository.PatientRepository;
import com.psicoagenda.infrastructure.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PatientService {

    private static final Logger log = LoggerFactory.getLogger(PatientService.class);

    private final PatientRepository patientRepository;
    private final AuditService auditService;

    public PatientService(PatientRepository patientRepository, AuditService auditService) {
        this.patientRepository = patientRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> getAllActivePatients() {
        return patientRepository.findByActiveTrue()
            .stream()
            .map(PatientResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> searchPatients(String name) {
        return patientRepository.findByNameContainingIgnoreCaseAndActiveTrue(name)
            .stream()
            .map(PatientResponse::from)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(UUID id) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));
        return PatientResponse.from(patient);
    }

    public Patient findOrCreatePatient(String name, String phone, String email) {
        // Try to find existing patient by phone
        return patientRepository.findByPhoneAndActiveTrue(phone)
            .orElseGet(() -> {
                Patient newPatient = Patient.builder()
                    .name(name)
                    .phone(phone)
                    .email(email)
                    .active(true)
                    .build();

                newPatient = patientRepository.save(newPatient);
                log.info("Created new patient: {}", newPatient.getId());

                auditService.logCreate("Patient", newPatient.getId(), newPatient);

                return newPatient;
            });
    }

    public PatientResponse updatePatient(UUID id, String name, String phone, String email) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));

        Patient oldState = clonePatient(patient);

        patient.setName(name);
        patient.setPhone(phone);
        patient.setEmail(email);

        patient = patientRepository.save(patient);
        log.info("Updated patient: {}", id);

        auditService.logUpdate("Patient", patient.getId(), oldState, patient);

        return PatientResponse.from(patient);
    }

    public void deactivatePatient(UUID id) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));

        patient.setActive(false);
        patientRepository.save(patient);

        log.info("Deactivated patient: {}", id);
        auditService.logUpdate("Patient", id, patient, patient);
    }

    private Patient clonePatient(Patient original) {
        return Patient.builder()
            .name(original.getName())
            .phone(original.getPhone())
            .email(original.getEmail())
            .active(original.isActive())
            .anonymized(original.isAnonymized())
            .build();
    }
}
