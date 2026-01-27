package com.psicoagenda.application.service;

import com.psicoagenda.application.dto.response.AppointmentResponse;
import com.psicoagenda.application.dto.response.DashboardResponse;
import com.psicoagenda.domain.repository.AppointmentRepository;
import com.psicoagenda.domain.repository.PaymentRepository;
import com.psicoagenda.domain.enums.PaymentStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final PaymentRepository paymentRepository;

    public DashboardService(AppointmentRepository appointmentRepository,
                            PaymentRepository paymentRepository) {
        this.appointmentRepository = appointmentRepository;
        this.paymentRepository = paymentRepository;
    }

    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        // Today's appointments
        List<AppointmentResponse> todayAppointments = appointmentRepository
            .findAppointmentsInRange(todayStart, todayEnd)
            .stream()
            .map(AppointmentResponse::from)
            .collect(Collectors.toList());

        // Upcoming appointments (next 5)
        List<AppointmentResponse> upcomingAppointments = appointmentRepository
            .findNextAppointments(5)
            .stream()
            .map(AppointmentResponse::from)
            .collect(Collectors.toList());

        // Statistics
        long totalToday = todayAppointments.size();

        // This week
        LocalDate weekStart = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);
        long totalThisWeek = appointmentRepository
            .findAppointmentsInRange(weekStart.atStartOfDay(), weekEnd.atStartOfDay())
            .size();

        // Pending payments
        long pendingPayments = paymentRepository.countByStatus(PaymentStatus.UNPAID);

        // Revenue this month
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate monthEnd = today.with(TemporalAdjusters.lastDayOfMonth()).plusDays(1);
        BigDecimal revenueThisMonth = paymentRepository.sumPaidInRange(
            monthStart.atStartOfDay(),
            monthEnd.atStartOfDay()
        );

        DashboardResponse.Statistics stats = new DashboardResponse.Statistics(
            totalToday,
            totalThisWeek,
            pendingPayments,
            revenueThisMonth != null ? revenueThisMonth : BigDecimal.ZERO
        );

        return new DashboardResponse(todayAppointments, upcomingAppointments, stats);
    }
}
