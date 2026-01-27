package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.Payment;
import com.psicoagenda.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByAppointmentId(UUID appointmentId);

    List<Payment> findByStatus(PaymentStatus status);

    @Query("SELECT p FROM Payment p JOIN FETCH p.appointment a JOIN FETCH a.patient " +
           "WHERE p.status = :status ORDER BY a.startDateTime DESC")
    List<Payment> findByStatusWithDetails(@Param("status") PaymentStatus status);

    @Query("SELECT p FROM Payment p JOIN FETCH p.appointment a JOIN FETCH a.patient " +
           "WHERE p.paidAt BETWEEN :start AND :end ORDER BY p.paidAt DESC")
    List<Payment> findPaidInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT p FROM Payment p JOIN FETCH p.appointment a " +
           "WHERE a.startDateTime BETWEEN :start AND :end " +
           "ORDER BY a.startDateTime")
    List<Payment> findByAppointmentDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = :status")
    long countByStatus(@Param("status") PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'PAID' AND p.paidAt BETWEEN :start AND :end")
    java.math.BigDecimal sumPaidInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
