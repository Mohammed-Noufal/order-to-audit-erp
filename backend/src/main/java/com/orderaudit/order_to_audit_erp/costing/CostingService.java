package com.orderaudit.order_to_audit_erp.costing;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderaudit.order_to_audit_erp.stock.LedgerRepository;
import com.orderaudit.order_to_audit_erp.stock.MovementType;
import com.orderaudit.order_to_audit_erp.stock.SourceType;

@Service
public class CostingService {

    private final LedgerRepository ledgerRepository;

    public CostingService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    /** Sum of (quantity issued x unit cost at that moment) for this run, 4 decimals. */
    @Transactional(readOnly = true)
    public BigDecimal materialCost(Long runId) {
        BigDecimal signedValue = ledgerRepository.sumValueBySource(
                SourceType.PRODUCTION_RUN, runId, MovementType.PROD_ISSUE);
        // Issues are stored as negative quantities, so the sum is negative
        return (signedValue == null)
                ? BigDecimal.ZERO.setScale(4)
                : signedValue.negate().setScale(4, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public RunCostSummary summarize(Long runId, Integer actualOutput,
                                    BigDecimal sellingPrice, BigDecimal overheadCost) {
        return RunCostSummary.calculate(materialCost(runId), overheadCost, actualOutput, sellingPrice);
    }
}
