package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderaudit.order_to_audit_erp.audit.AuditService;
import com.orderaudit.order_to_audit_erp.auth.CurrentUserService;
import com.orderaudit.order_to_audit_erp.common.BadRequestException;
import com.orderaudit.order_to_audit_erp.common.ConflictException;
import com.orderaudit.order_to_audit_erp.common.ResourceNotFoundException;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public PaymentService(PaymentRepository paymentRepository,
                          InvoiceRepository invoiceRepository,
                          CurrentUserService currentUserService,
                          AuditService auditService) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    @Transactional
    public PaymentResponse record(PaymentRequests.CreatePayment request) {
        // Lock the invoice row: two payments on the same invoice cannot both pass the check below
        Invoice invoice = invoiceRepository.lockById(request.invoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + request.invoiceId()));
        if (invoice.getStatus() != InvoiceStatus.POSTED) {
            throw new ConflictException("Cannot record a payment against a " + invoice.getStatus() + " invoice");
        }

        LocalDate paidOn = (request.paidOn() != null) ? request.paidOn() : LocalDate.now();
        if (paidOn.isAfter(LocalDate.now())) {
            throw new BadRequestException("Payment date cannot be in the future");
        }
        if (paidOn.isBefore(invoice.getInvoiceDate())) {
            throw new BadRequestException(
                    "Payment date cannot be before the invoice date (advance payments are not supported)");
        }

        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal alreadyPaid = paidSoFar(invoice.getId());
        BigDecimal outstanding = invoice.getTotal().subtract(alreadyPaid);
        if (amount.compareTo(outstanding) > 0) {
            throw new ConflictException("Payment of Rs %s exceeds the invoice's outstanding balance of Rs %s"
                    .formatted(amount.toPlainString(), outstanding.toPlainString()));
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setPaidOn(paidOn);
        payment.setMode(request.mode());
        payment.setReference(request.reference());
        payment.setReceivedBy(currentUserService.requireCurrentUser().getId());
        Payment saved = paymentRepository.save(payment);

        auditService.log("PAYMENT", saved.getId(), "CREATE", null,
                Map.of("invoiceNo", invoice.getInvoiceNo(), "amount", amount, "mode", request.mode()));
        return PaymentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> list() {
        return paymentRepository.findAllByOrderByPaidOnDescIdDesc().stream()
                .map(PaymentResponse::from)
                .toList();
    }

    private BigDecimal paidSoFar(Long invoiceId) {
        BigDecimal sum = paymentRepository.sumForInvoiceOrNull(invoiceId);
        return (sum != null) ? sum.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2);
    }
}
