package com.orderaudit.order_to_audit_erp.stock;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.orderaudit.order_to_audit_erp.master.ItemType;

@RestController
@RequestMapping("/api/stock")
@PreAuthorize("hasAnyRole('PRODUCTION','ADMIN','OWNER','ACCOUNTANT')") // salesmen never see stock or cost
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    /** Current balance per item, derived from the ledger. Optional ?type=RAW|PACKAGING|FINISHED */
    @GetMapping
    public List<StockBalanceResponse> balances(@RequestParam(required = false) ItemType type) {
        return stockService.getBalances(type);
    }

    /** Every movement with its source document. Optional ?itemId=&from=2026-04-01&to=2026-04-30 */
    @GetMapping("/ledger")
    public List<LedgerEntryResponse> ledger(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return stockService.getLedger(itemId, from, to);
    }
}
