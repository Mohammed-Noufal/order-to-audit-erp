package com.orderaudit.order_to_audit_erp.stock;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.orderaudit.order_to_audit_erp.auth.CurrentUserService;
import com.orderaudit.order_to_audit_erp.common.BadRequestException;
import com.orderaudit.order_to_audit_erp.common.ResourceNotFoundException;
import com.orderaudit.order_to_audit_erp.master.Item;
import com.orderaudit.order_to_audit_erp.master.ItemRepository;
import com.orderaudit.order_to_audit_erp.master.ItemType;

@Service
public class StockService {

    private static final LocalDate EARLIEST = LocalDate.of(1900, 1, 1);
    private static final LocalDate LATEST = LocalDate.of(9999, 12, 31);

    private final ItemRepository itemRepository;
    private final LedgerRepository ledgerRepository;
    private final CurrentUserService currentUserService;

    public StockService(ItemRepository itemRepository,
                        LedgerRepository ledgerRepository,
                        CurrentUserService currentUserService) {
        this.itemRepository = itemRepository;
        this.ledgerRepository = ledgerRepository;
        this.currentUserService = currentUserService;
    }

    /**
     * The only way stock changes. MANDATORY: it must run inside the caller's transaction,
     * so if the business operation fails halfway, every ledger row it wrote rolls back too.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public LedgerEntry post(Item item, MovementType type, BigDecimal delta, BigDecimal unitCost,
                            SourceType sourceType, Long sourceId, LocalDate movementDate) {
        BigDecimal qty = delta.setScale(3, RoundingMode.HALF_UP);
        if (qty.signum() == 0) {
            throw new BadRequestException("Stock movement quantity cannot be zero");
        }
        if (!type.allows(qty)) {
            // A coding mistake, not a user error, so this surfaces as a logged 500
            throw new IllegalArgumentException("Quantity sign " + qty.signum() + " is not valid for " + type);
        }

        // SELECT ... FOR UPDATE: nobody else can change this item's stock until we commit
        Item locked = itemRepository.lockById(item.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + item.getId()));

        BigDecimal balance = balanceOf(locked.getId());
        if (qty.signum() < 0 && balance.add(qty).signum() < 0) {
            throw new InsufficientStockException(locked, balance, qty.negate());
        }

        Long userId = currentUserService.requireCurrentUser().getId();
        return ledgerRepository.save(new LedgerEntry(
                locked, movementDate, type, qty, unitCost.setScale(4, RoundingMode.HALF_UP),
                sourceType, sourceId, userId));
    }

    @Transactional(readOnly = true)
    public BigDecimal balance(Long itemId) {
        return balanceOf(itemId);
    }

    @Transactional(readOnly = true)
    public List<StockBalanceResponse> getBalances(ItemType type) {
        List<StockBalanceView> rows = (type == null)
                ? ledgerRepository.findBalances()
                : ledgerRepository.findBalancesByType(type.name());
        return rows.stream().map(StockBalanceResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryResponse> getLedger(Long itemId, LocalDate from, LocalDate to) {
        LocalDate fromDate = (from != null) ? from : EARLIEST;
        LocalDate toDate = (to != null) ? to : LATEST;
        if (fromDate.isAfter(toDate)) {
            throw new BadRequestException("'from' must not be after 'to'");
        }
        List<LedgerEntry> entries;
        if (itemId != null) {
            if (!itemRepository.existsById(itemId)) {
                throw new ResourceNotFoundException("Item not found: " + itemId);
            }
            entries = ledgerRepository.findByItemAndDateRange(itemId, fromDate, toDate);
        } else {
            entries = ledgerRepository.findByDateRange(fromDate, toDate);
        }
        return entries.stream().map(LedgerEntryResponse::from).toList();
    }

    private BigDecimal balanceOf(Long itemId) {
        BigDecimal sum = ledgerRepository.sumDeltaOrNull(itemId);
        return (sum != null) ? sum : BigDecimal.ZERO;
    }
}
