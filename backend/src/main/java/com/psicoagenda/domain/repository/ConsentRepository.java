package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.Consent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    List<Consent> findByPatientIdOrderByAcceptedAtDesc(UUID patientId);

    @Query("SELECT c FROM Consent c WHERE c.patient.id = :patientId ORDER BY c.acceptedAt DESC LIMIT 1")
    Optional<Consent> findLatestByPatientId(@Param("patientId") UUID patientId);

    boolean existsByPatientIdAndVersion(UUID patientId, String version);
}
