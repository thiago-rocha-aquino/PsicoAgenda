package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.SessionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionTypeRepository extends JpaRepository<SessionType, UUID> {
    List<SessionType> findByActiveTrueOrderByDisplayOrderAsc();
    List<SessionType> findAllByOrderByDisplayOrderAsc();
}
