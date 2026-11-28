package com.orderaudit.order_to_audit_erp.sales;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {

    @EntityGraph(attributePaths = {"customer", "lines", "lines.item"})
    Optional<SalesOrder> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"customer", "lines", "lines.item"})
    Optional<SalesOrder> findByClientUuid(String clientUuid);

    @EntityGraph(attributePaths = {"customer", "lines", "lines.item"})
    List<SalesOrder> findAllByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = {"customer", "lines", "lines.item"})
    List<SalesOrder> findByStatusOrderByCreatedAtDescIdDesc(OrderStatus status);

    @EntityGraph(attributePaths = {"customer", "lines", "lines.item"})
    List<SalesOrder> findBySalesmanIdOrderByCreatedAtDescIdDesc(Long salesmanId);

    @EntityGraph(attributePaths = {"customer", "lines", "lines.item"})
    List<SalesOrder> findBySalesmanIdAndStatusOrderByCreatedAtDescIdDesc(Long salesmanId, OrderStatus status);

    // Row lock: two admins cannot approve, invoice or dispatch the same order at the same moment
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM SalesOrder o WHERE o.id = :id")
    Optional<SalesOrder> lockById(@Param("id") Long id);
}
