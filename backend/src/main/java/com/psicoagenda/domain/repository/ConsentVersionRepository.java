package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.ConsentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsentVersionRepository extends JpaRepository<ConsentVersion, UUID> {

    Optional<ConsentVersion> findByVersion(String version);

    @Query("SELECT cv FROM ConsentVersion cv WHERE cv.active = true ORDER BY cv.effectiveFrom DESC LIMIT 1")
    Optional<ConsentVersion> findCurrentVersion();

    boolean existsByVersion(String version);
}
