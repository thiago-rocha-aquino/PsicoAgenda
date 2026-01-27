package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BlockRepository extends JpaRepository<Block, UUID> {

    @Query("SELECT b FROM Block b WHERE b.endDateTime >= :start AND b.startDateTime <= :end ORDER BY b.startDateTime")
    List<Block> findBlocksInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Block b " +
           "WHERE b.startDateTime < :end AND b.endDateTime > :start")
    boolean existsBlockOverlapping(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Block> findByStartDateTimeAfterOrderByStartDateTimeAsc(LocalDateTime dateTime);
}
