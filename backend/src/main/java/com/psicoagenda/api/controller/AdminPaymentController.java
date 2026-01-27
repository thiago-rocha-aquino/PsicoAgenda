package com.psicoagenda.api.controller;

import com.psicoagenda.application.dto.request.PaymentUpdateRequest;
import com.psicoagenda.application.dto.response.PaymentResponse;
import com.psicoagenda.application.service.PaymentService;
import com.psicoagenda.domain.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/payments")
@Tag(name = "Admin - Pagamentos", description = "Gerenciamento de pagamentos")
public class AdminPaymentController {

    private final PaymentService paymentService;

    public AdminPaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/pending")
    @Operation(summary = "Pagamentos pendentes", description = "Lista pagamentos não realizados")
    public ResponseEntity<List<PaymentResponse>> getPendingPayments() {
        return ResponseEntity.ok(paymentService.getPendingPayments());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Pagamentos por status", description = "Lista pagamentos por status")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(status));
    }

    @GetMapping("/range")
    @Operation(summary = "Pagamentos em período", description = "Lista pagamentos em um período")
    public ResponseEntity<List<PaymentResponse>> getPaymentsInRange(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return ResponseEntity.ok(paymentService.getPaymentsInRange(start, end));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalhe do pagamento", description = "Retorna detalhes de um pagamento")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable UUID id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }

    @GetMapping("/appointment/{appointmentId}")
    @Operation(summary = "Pagamento do agendamento", description = "Retorna o pagamento de um agendamento")
    public ResponseEntity<PaymentResponse> getPaymentByAppointment(@PathVariable UUID appointmentId) {
        return ResponseEntity.ok(paymentService.getPaymentByAppointmentId(appointmentId));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar pagamento", description = "Atualiza um pagamento")
    public ResponseEntity<PaymentResponse> updatePayment(
        @PathVariable UUID id,
        @Valid @RequestBody PaymentUpdateRequest request
    ) {
        return ResponseEntity.ok(paymentService.updatePayment(id, request));
    }

    @PostMapping("/appointment/{appointmentId}/mark-paid")
    @Operation(summary = "Marcar como pago", description = "Marca um pagamento como pago")
    public ResponseEntity<PaymentResponse> markAsPaid(
        @PathVariable UUID appointmentId,
        @RequestParam(required = false) String receiptNumber
    ) {
        return ResponseEntity.ok(paymentService.markAsPaid(appointmentId, receiptNumber));
    }

    @PostMapping("/appointment/{appointmentId}/waive")
    @Operation(summary = "Isentar pagamento", description = "Isenta um pagamento")
    public ResponseEntity<PaymentResponse> waivePayment(
        @PathVariable UUID appointmentId,
        @RequestParam(required = false) String reason
    ) {
        return ResponseEntity.ok(paymentService.markAsWaived(appointmentId, reason));
    }

    @GetMapping("/generate-receipt-number")
    @Operation(summary = "Gerar número de recibo", description = "Gera um número de recibo sequencial")
    public ResponseEntity<String> generateReceiptNumber() {
        return ResponseEntity.ok(paymentService.generateReceiptNumber());
    }
}
