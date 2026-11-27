package com.orderaudit.order_to_audit_erp.purchase;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {

    @EntityGraph(attributePaths = {"supplier", "lines", "lines.item"})
    List<Purchase> findAllByOrderByPurchaseDateDescIdDesc();

    @EntityGraph(attributePaths = {"supplier", "lines", "lines.item"})
    Optional<Purchase> findWithLinesById(Long id);
}
