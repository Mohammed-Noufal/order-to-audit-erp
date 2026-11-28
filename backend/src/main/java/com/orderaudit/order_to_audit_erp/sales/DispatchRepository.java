package com.orderaudit.order_to_audit_erp.sales;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {

    boolean existsByInvoiceId(Long invoiceId);

    List<Dispatch> findByInvoiceIdIn(Collection<Long> invoiceIds);
}
