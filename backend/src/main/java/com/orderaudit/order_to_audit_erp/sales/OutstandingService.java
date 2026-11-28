package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderaudit.order_to_audit_erp.common.ResourceNotFoundException;
import com.orderaudit.order_to_audit_erp.master.Customer;
import com.orderaudit.order_to_audit_erp.master.CustomerRepository;

@Service
public class OutstandingService {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    private final CustomerRepository customerRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public OutstandingService(CustomerRepository customerRepository,
                              InvoiceRepository invoiceRepository,
                              PaymentRepository paymentRepository) {
        this.customerRepository = customerRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public OutstandingResponse forCustomer(Long customerId, LocalDate asOfDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + customerId));
        LocalDate asOf = (asOfDate != null) ? asOfDate : LocalDate.now();

        List<Invoice> invoices = invoiceRepository
                .findByCustomerIdAndInvoiceDateLessThanEqualOrderByInvoiceDateAscIdAsc(customerId, asOf);

        // One query for every payment of these invoices, then summed per invoice
        Map<Long, BigDecimal> paidByInvoice = invoices.isEmpty()
                ? Map.of()
                : paymentRepository.findByInvoiceIdIn(invoices.stream().map(Invoice::getId).toList()).stream()
                        .filter(p -> !p.getPaidOn().isAfter(asOf))
                        .collect(Collectors.groupingBy(
                                p -> p.getInvoice().getId(),
                                Collectors.mapping(Payment::getAmount,
                                        Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        Map<AgeingBucket, BigDecimal> buckets = new EnumMap<>(AgeingBucket.class);
        for (AgeingBucket bucket : AgeingBucket.values()) {
            buckets.put(bucket, ZERO);
        }

        BigDecimal totalInvoiced = ZERO;
        BigDecimal totalPaid = ZERO;
        BigDecimal totalOutstanding = ZERO;
        List<OutstandingResponse.InvoiceRow> rows = new ArrayList<>();

        for (Invoice invoice : invoices) {
            BigDecimal paid = paidByInvoice.getOrDefault(invoice.getId(), ZERO).setScale(2);
            boolean posted = invoice.getStatus() == InvoiceStatus.POSTED;
            // A cancelled invoice is not owed any more
            BigDecimal balance = posted ? invoice.getTotal().subtract(paid).max(ZERO) : ZERO;

            long ageDays = Math.max(0, ChronoUnit.DAYS.between(invoice.getInvoiceDate(), asOf));
            AgeingBucket bucket = AgeingBucket.forDays(ageDays);

            if (posted) {
                totalInvoiced = totalInvoiced.add(invoice.getTotal());
                totalPaid = totalPaid.add(paid);
                totalOutstanding = totalOutstanding.add(balance);
                if (balance.signum() > 0) {
                    buckets.merge(bucket, balance, BigDecimal::add);
                }
            }

            rows.add(new OutstandingResponse.InvoiceRow(
                    invoice.getId(), invoice.getInvoiceNo(), invoice.getInvoiceDate(), invoice.getStatus(),
                    invoice.getTotal(), paid, balance, ageDays, bucket.label()));
        }

        OutstandingResponse.Ageing ageing = new OutstandingResponse.Ageing(
                buckets.get(AgeingBucket.DAYS_0_30),
                buckets.get(AgeingBucket.DAYS_31_60),
                buckets.get(AgeingBucket.DAYS_61_90),
                buckets.get(AgeingBucket.DAYS_90_PLUS));

        return new OutstandingResponse(
                customer.getId(), customer.getName(), customer.getGstin(), asOf,
                totalInvoiced, totalPaid, totalOutstanding, ageing, rows);
    }
}
