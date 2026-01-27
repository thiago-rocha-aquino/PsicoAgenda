package com.psicoagenda.domain.enums;

public enum AppointmentStatus {
    SCHEDULED,      // Agendado, aguardando confirmação
    CONFIRMED,      // Confirmado
    CANCELLED,      // Cancelado dentro do prazo
    CANCELLED_LATE, // Cancelado fora do prazo (menos de 24h)
    ATTENDED,       // Compareceu
    NO_SHOW         // Não compareceu
}
