package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.RecurringSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RecurringSeriesRepository extends JpaRepository<RecurringSeries, UUID> {

    List<RecurringSeries> findByActiveTrueOrderByStartDateDesc();

    List<RecurringSeries> findByPatientIdAndActiveTrue(UUID patientId);

    @Query("SELECT rs FROM RecurringSeries rs JOIN FETCH rs.patient JOIN FETCH rs.sessionType WHERE rs.active = true")
    List<RecurringSeries> findAllActiveWithDetails();

    @Query("SELECT rs FROM RecurringSeries rs JOIN FETCH rs.patient JOIN FETCH rs.sessionType WHERE rs.id = :id")
    RecurringSeries findByIdWithDetails(@Param("id") UUID id);
}
