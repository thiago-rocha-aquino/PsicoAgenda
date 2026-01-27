package com.psicoagenda.domain.repository;

import com.psicoagenda.domain.entity.Appointment;
import com.psicoagenda.domain.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {

    Optional<Appointment> findByCancellationToken(String cancellationToken);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient JOIN FETCH a.sessionType " +
           "WHERE a.startDateTime >= :start AND a.startDateTime < :end " +
           "AND a.status NOT IN ('CANCELLED', 'CANCELLED_LATE') " +
           "ORDER BY a.startDateTime")
    List<Appointment> findAppointmentsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient JOIN FETCH a.sessionType " +
           "WHERE a.startDateTime >= :start AND a.startDateTime < :end " +
           "ORDER BY a.startDateTime")
    List<Appointment> findAllAppointmentsInRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
           "WHERE a.startDateTime < :end AND a.endDateTime > :start " +
           "AND a.status NOT IN ('CANCELLED', 'CANCELLED_LATE')")
    boolean existsOverlappingAppointment(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Appointment a " +
           "WHERE a.startDateTime < :end AND a.endDateTime > :start " +
           "AND a.status NOT IN ('CANCELLED', 'CANCELLED_LATE') " +
           "AND a.id <> :excludeId")
    boolean existsOverlappingAppointmentExcluding(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end,
                                                   @Param("excludeId") UUID excludeId);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient JOIN FETCH a.sessionType " +
           "WHERE a.patient.id = :patientId ORDER BY a.startDateTime DESC")
    List<Appointment> findByPatientIdOrderByStartDateTimeDesc(@Param("patientId") UUID patientId);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient JOIN FETCH a.sessionType " +
           "WHERE a.recurringSeries.id = :seriesId ORDER BY a.startDateTime")
    List<Appointment> findByRecurringSeriesId(@Param("seriesId") UUID seriesId);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient " +
           "WHERE a.status IN ('CONFIRMED', 'SCHEDULED') " +
           "AND a.startDateTime BETWEEN :start AND :end")
    List<Appointment> findUpcomingForReminders(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Appointment> findByStatusIn(List<AppointmentStatus> statuses);

    @Query("SELECT a FROM Appointment a WHERE a.recurringSeries.id = :seriesId " +
           "AND a.startDateTime >= :fromDate " +
           "AND a.status NOT IN ('CANCELLED', 'CANCELLED_LATE')")
    List<Appointment> findFutureAppointmentsBySeries(@Param("seriesId") UUID seriesId,
                                                      @Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient JOIN FETCH a.sessionType LEFT JOIN FETCH a.payment " +
           "WHERE a.id = :id")
    Optional<Appointment> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT a FROM Appointment a JOIN FETCH a.patient JOIN FETCH a.sessionType " +
           "WHERE a.startDateTime >= CURRENT_TIMESTAMP " +
           "AND a.status IN ('CONFIRMED', 'SCHEDULED') " +
           "ORDER BY a.startDateTime LIMIT :limit")
    List<Appointment> findNextAppointments(@Param("limit") int limit);
}
