package com.orderaudit.order_to_audit_erp.sales;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** Total paid against one invoice. Null when there are no payments yet. */
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.invoice.id = :invoiceId")
    BigDecimal sumForInvoiceOrNull(@Param("invoiceId") Long invoiceId);

    @EntityGraph(attributePaths = {"invoice", "invoice.customer"})
    List<Payment> findAllByOrderByPaidOnDescIdDesc();

    /** All payments for a set of invoices in one query (used by the outstanding report). */
    List<Payment> findByInvoiceIdIn(Collection<Long> invoiceIds);
}
