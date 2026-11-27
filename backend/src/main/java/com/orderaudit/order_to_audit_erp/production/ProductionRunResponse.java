package com.orderaudit.order_to_audit_erp.production;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.orderaudit.order_to_audit_erp.costing.RunCostSummary;

public record ProductionRunResponse(
        Long id,
        String runNo,
        Long productId,
        String productCode,
        String productName,
        BigDecimal batches,
        Integer expectedOutput,
        Integer actualOutput,
        Integer wastagePcs,
        BigDecimal yieldVariancePct,
        boolean yieldFlagged,
        RunStatus status,
        LocalDate runDate,
        String batchNo,
        List<Line> lines,
        List<Overhead> overheads,
        RunCostSummary costing) {

    public record Line(
            Long itemId,
            String itemCode,
            String itemName,
            String stockUnit,
            BigDecimal plannedQty,
            BigDecimal actualQty,
            BigDecimal variancePct,
            boolean flagged) {
    }

    public record Overhead(Long id, OverheadType type, BigDecimal amount, String note) {
    }

    public static ProductionRunResponse from(ProductionRun run, RunCostSummary costing) {
        List<Line> lines = run.getLines().stream()
                .map(l -> new Line(
                        l.getItem().getId(),
                        l.getItem().getCode(),
                        l.getItem().getName(),
                        l.getItem().getStockUnit().name(),
                        l.getPlannedQty(),
                        l.getActualQty(),
                        l.getVariancePct(),
                        l.isFlagged()))
                .toList();
        List<Overhead> overheads = run.getOverheads().stream()
                .map(o -> new Overhead(o.getId(), o.getType(), o.getAmount(), o.getNote()))
                .toList();
        return new ProductionRunResponse(
                run.getId(), run.getRunNo(),
                run.getProduct().getId(), run.getProduct().getCode(), run.getProduct().getName(),
                run.getBatches(), run.getExpectedOutput(), run.getActualOutput(), run.getWastagePcs(),
                run.getYieldVariancePct(), run.isYieldFlagged(), run.getStatus(),
                run.getRunDate(), run.getBatchNo(), lines, overheads, costing);
    }
}
