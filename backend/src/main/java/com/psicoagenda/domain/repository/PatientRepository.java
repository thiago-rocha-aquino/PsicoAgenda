package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    List<Patient> findByActiveTrue();

    List<Patient> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    Optional<Patient> findByPhoneAndActiveTrue(String phone);

    @Query("SELECT p FROM Patient p WHERE p.anonymized = false AND p.createdAt < :cutoffDate AND NOT EXISTS " +
           "(SELECT 1 FROM Appointment a WHERE a.patient = p AND a.startDateTime >= :cutoffDate)")
    List<Patient> findPatientsForAnonymization(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Modifying
    @Query("UPDATE Patient p SET p.name = 'Anonimizado', p.phone = 'XXX', p.email = 'XXX', p.anonymized = true WHERE p.id = :id")
    void anonymizePatient(@Param("id") UUID id);
}
