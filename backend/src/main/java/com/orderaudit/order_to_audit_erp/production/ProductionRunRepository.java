package com.orderaudit.order_to_audit_erp.production;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ProductionRunRepository extends JpaRepository<ProductionRun, Long> {

    @EntityGraph(attributePaths = {"product"})
    List<ProductionRun> findAllByOrderByRunDateDescIdDesc();

    // One collection (lines) is fetched here; overheads load lazily. Two at once would fail.
    @EntityGraph(attributePaths = {"product", "lines", "lines.item"})
    Optional<ProductionRun> findWithDetailsById(Long id);

    // Row lock so two people cannot issue or complete the same run at the same time
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ProductionRun r WHERE r.id = :id")
    Optional<ProductionRun> lockById(@Param("id") Long id);
}
