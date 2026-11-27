package com.orderaudit.order_to_audit_erp.master;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BomRepository extends JpaRepository<Bom, Long> {

    // Fetch lines and their items in one query instead of one query per line
    @EntityGraph(attributePaths = {"lines", "lines.item"})
    List<Bom> findAllByOrderByIdAsc();

    @EntityGraph(attributePaths = {"lines", "lines.item"})
    Optional<Bom> findWithLinesById(Long id);
}
