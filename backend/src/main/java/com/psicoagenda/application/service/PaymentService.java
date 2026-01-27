package com.psicoagenda.application.service;

import com.psicoagenda.application.dto.request.PaymentUpdateRequest;
import com.psicoagenda.application.dto.response.PaymentResponse;
import com.psicoagenda.application.exception.ResourceNotFoundException;
import com.psicoagenda.domain.entity.Payment;
import com.psicoagenda.domain.enums.PaymentStatus;
import com.psicoagenda.domain.repository.PaymentRepository;
import com.psicoagenda.infrastructure.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final AuditService auditService;

    public PaymentService(PaymentRepository paymentRepository, AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento", "id", id));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByAppointmentId(UUID appointmentId) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento", "appointmentId", appointmentId));
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByStatus(PaymentStatus status) {
        return paymentRepository.findByStatusWithDetails(status)
            .stream()
            .map(PaymentResponse::fromWithAppointment)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPendingPayments() {
        return getPaymentsByStatus(PaymentStatus.UNPAID);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsInRange(LocalDateTime start, LocalDateTime end) {
        return paymentRepository.findByAppointmentDateRange(start, end)
            .stream()
            .map(PaymentResponse::fromWithAppointment)
            .collect(Collectors.toList());
    }

    public PaymentResponse updatePayment(UUID id, PaymentUpdateRequest request) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento", "id", id));

        PaymentStatus oldStatus = payment.getStatus();

        payment.setStatus(request.status());

        if (request.status() == PaymentStatus.PAID) {
            payment.setPaidAt(request.paidAt() != null ? request.paidAt() : LocalDateTime.now());
        } else if (request.status() == PaymentStatus.UNPAID) {
            payment.setPaidAt(null);
        }

        if (request.amount() != null) {
            payment.setAmount(request.amount());
        }

        if (request.receiptNumber() != null) {
            payment.setReceiptNumber(request.receiptNumber());
        }

        if (request.notes() != null) {
            payment.setNotes(request.notes());
        }

        payment = paymentRepository.save(payment);
        log.info("Updated payment {} status from {} to {}", id, oldStatus, request.status());

        auditService.logUpdate("Payment", id, oldStatus.name(), request.status().name());

        return PaymentResponse.from(payment);
    }

    public PaymentResponse markAsPaid(UUID appointmentId, String receiptNumber) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento", "appointmentId", appointmentId));

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        if (receiptNumber != null) {
            payment.setReceiptNumber(receiptNumber);
        }

        payment = paymentRepository.save(payment);
        log.info("Marked payment for appointment {} as paid", appointmentId);

        auditService.logUpdate("Payment", payment.getId(), "UNPAID", "PAID");

        return PaymentResponse.from(payment);
    }

    public PaymentResponse markAsWaived(UUID appointmentId, String reason) {
        Payment payment = paymentRepository.findByAppointmentId(appointmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Pagamento", "appointmentId", appointmentId));

        payment.setStatus(PaymentStatus.WAIVED);
        payment.setNotes(reason);

        payment = paymentRepository.save(payment);
        log.info("Marked payment for appointment {} as waived", appointmentId);

        auditService.logUpdate("Payment", payment.getId(), "UNPAID", "WAIVED");

        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public long countPendingPayments() {
        return paymentRepository.countByStatus(PaymentStatus.UNPAID);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenueInRange(LocalDateTime start, LocalDateTime end) {
        BigDecimal total = paymentRepository.sumPaidInRange(start, end);
        return total != null ? total : BigDecimal.ZERO;
    }

    /**
     * Generate a simple receipt number (sequential for the year)
     */
    public String generateReceiptNumber() {
        int year = LocalDateTime.now().getYear();
        long count = paymentRepository.count();
        return String.format("REC-%d-%05d", year, count + 1);
    }
}
