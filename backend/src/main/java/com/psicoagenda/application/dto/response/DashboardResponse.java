package com.psicoagenda.application.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
    List<AppointmentResponse> todayAppointments,
    List<AppointmentResponse> upcomingAppointments,
    Statistics statistics
) {
    public record Statistics(
        long totalAppointmentsToday,
        long totalAppointmentsThisWeek,
        long pendingPayments,
        BigDecimal revenueThisMonth
    ) {}
}
