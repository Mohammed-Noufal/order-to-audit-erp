package com.orderaudit.order_to_audit_erp.sales;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    boolean existsByOrderId(Long orderId);

    @EntityGraph(attributePaths = {"customer"})
    List<Invoice> findAllByOrderByInvoiceDateDescIdDesc();

    @EntityGraph(attributePaths = {"customer", "lines", "lines.item"})
    Optional<Invoice> findWithDetailsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Invoice i WHERE i.id = :id")
    Optional<Invoice> lockById(@Param("id") Long id);
}
