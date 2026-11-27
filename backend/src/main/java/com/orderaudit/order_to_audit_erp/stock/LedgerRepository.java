package com.orderaudit.order_to_audit_erp.stock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    String BALANCE_SELECT = """
            SELECT v.item_id AS itemId, v.item_code AS code, v.item_name AS name,
                   v.item_type AS itemType, v.stock_unit AS stockUnit, v.balance AS balance,
                   i.avg_cost AS avgCost, i.reorder_level AS reorderLevel
            FROM v_stock_balance v
            JOIN items i ON i.id = v.item_id
            WHERE i.active = TRUE
            """;

    /** Balance = sum of the ledger. Returns null when the item has no rows. */
    @Query("SELECT SUM(l.qtyDelta) FROM LedgerEntry l WHERE l.item.id = :itemId")
    BigDecimal sumDeltaOrNull(@Param("itemId") Long itemId);

    /** Sum of quantity x unit cost for one source document and movement type. Null when no rows. */
    @Query("""
            SELECT SUM(l.qtyDelta * l.unitCost) FROM LedgerEntry l
            WHERE l.sourceType = :sourceType AND l.sourceId = :sourceId
              AND l.movementType = :movementType
            """)
    BigDecimal sumValueBySource(@Param("sourceType") SourceType sourceType,
                                @Param("sourceId") Long sourceId,
                                @Param("movementType") MovementType movementType);

    @Query("""
            SELECT l FROM LedgerEntry l JOIN FETCH l.item
            WHERE l.movementDate >= :fromDate AND l.movementDate <= :toDate
            ORDER BY l.movementDate ASC, l.id ASC
            """)
    List<LedgerEntry> findByDateRange(@Param("fromDate") LocalDate fromDate,
                                      @Param("toDate") LocalDate toDate);

    @Query("""
            SELECT l FROM LedgerEntry l JOIN FETCH l.item
            WHERE l.item.id = :itemId
              AND l.movementDate >= :fromDate AND l.movementDate <= :toDate
            ORDER BY l.movementDate ASC, l.id ASC
            """)
    List<LedgerEntry> findByItemAndDateRange(@Param("itemId") Long itemId,
                                             @Param("fromDate") LocalDate fromDate,
                                             @Param("toDate") LocalDate toDate);

    @Query(value = BALANCE_SELECT + " ORDER BY v.item_id", nativeQuery = true)
    List<StockBalanceView> findBalances();

    @Query(value = BALANCE_SELECT + " AND v.item_type = :type ORDER BY v.item_id", nativeQuery = true)
    List<StockBalanceView> findBalancesByType(@Param("type") String type);
}
