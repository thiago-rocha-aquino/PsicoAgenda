package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.Availability;
import com.psicoagenda.domain.enums.DayOfWeekEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AvailabilityRepository extends JpaRepository<Availability, UUID> {
    List<Availability> findByActiveTrue();
    List<Availability> findByDayOfWeekAndActiveTrue(DayOfWeekEnum dayOfWeek);
    List<Availability> findAllByOrderByDayOfWeekAscStartTimeAsc();
}
