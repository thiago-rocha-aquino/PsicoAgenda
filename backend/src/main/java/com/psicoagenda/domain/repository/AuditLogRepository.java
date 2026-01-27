package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    List<AuditLog> findByEntityTypeAndEntityIdOrderByPerformedAtDesc(String entityType, UUID entityId);

    Page<AuditLog> findByPerformedByOrderByPerformedAtDesc(String performedBy, Pageable pageable);

    Page<AuditLog> findAllByOrderByPerformedAtDesc(Pageable pageable);
}
