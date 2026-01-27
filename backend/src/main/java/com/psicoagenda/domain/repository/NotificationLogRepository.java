package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.NotificationLog;
import com.psicoagenda.domain.enums.NotificationTrigger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {

    List<NotificationLog> findByAppointmentIdOrderByCreatedAtDesc(UUID appointmentId);

    boolean existsByAppointmentIdAndTriggerType(UUID appointmentId, NotificationTrigger triggerType);
}
