package com.orderaudit.order_to_audit_erp.purchase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderaudit.order_to_audit_erp.auth.CurrentUserService;
import com.orderaudit.order_to_audit_erp.common.BadRequestException;
import com.orderaudit.order_to_audit_erp.common.DocSequenceService;
import com.orderaudit.order_to_audit_erp.common.ResourceNotFoundException;
import com.orderaudit.order_to_audit_erp.master.Item;
import com.orderaudit.order_to_audit_erp.master.ItemRepository;
import com.orderaudit.order_to_audit_erp.master.ItemType;
import com.orderaudit.order_to_audit_erp.master.Supplier;
import com.orderaudit.order_to_audit_erp.master.SupplierRepository;
import com.orderaudit.order_to_audit_erp.stock.MovementType;
import com.orderaudit.order_to_audit_erp.stock.SourceType;
import com.orderaudit.order_to_audit_erp.stock.StockService;

@Service
public class PurchaseService {

    private final PurchaseRepository purchaseRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;
    private final StockService stockService;
    private final DocSequenceService docSequenceService;
    private final CurrentUserService currentUserService;

    public PurchaseService(PurchaseRepository purchaseRepository,
                           SupplierRepository supplierRepository,
                           ItemRepository itemRepository,
                           StockService stockService,
                           DocSequenceService docSequenceService,
                           CurrentUserService currentUserService) {
        this.purchaseRepository = purchaseRepository;
        this.supplierRepository = supplierRepository;
        this.itemRepository = itemRepository;
        this.stockService = stockService;
        this.docSequenceService = docSequenceService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public PurchaseResponse create(CreatePurchaseRequest request) {
        LocalDate date = (request.purchaseDate() != null) ? request.purchaseDate() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Purchase date cannot be in the future");
        }
        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.supplierId()));
        Long userId = currentUserService.requireCurrentUser().getId();

        Purchase purchase = new Purchase();
        purchase.setPurchaseNo(docSequenceService.next("PUR", date));
        purchase.setSupplier(supplier);
        purchase.setPurchaseDate(date);
        purchase.setSupplierBillNo(request.supplierBillNo());
        purchase.setCreatedBy(userId);
        purchase = purchaseRepository.save(purchase); // gets its id now, needed as the ledger source_id

        for (CreatePurchaseRequest.Line line : request.lines()) {
            Item item = itemRepository.lockById(line.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + line.itemId()));
            if (!item.isActive() || item.getItemType() == ItemType.FINISHED) {
                throw new BadRequestException(
                        "Item " + item.getCode() + " cannot be purchased (inactive or a finished good)");
            }

            // 1. Convert what the user typed (e.g. grams) into the item's stock unit
            BigDecimal qty = line.unit().toStockQty(line.qty(), item);
            BigDecimal rate = line.unit().toStockRate(line.rate(), item);
            if (qty.signum() <= 0) {
                throw new BadRequestException("Quantity for " + item.getCode()
                        + " is too small: it rounds to zero " + item.getStockUnit());
            }
            BigDecimal amount = qty.multiply(rate).setScale(2, RoundingMode.HALF_UP);

            // 2. Weighted average cost, computed from the balance BEFORE this receipt
            BigDecimal before = stockService.balance(item.getId());
            BigDecimal newAvgCost = before.multiply(item.getAvgCost())
                    .add(qty.multiply(rate))
                    .divide(before.add(qty), 4, RoundingMode.HALF_UP);

            // 3. Passbook line, then update the cached average on the item
            stockService.post(item, MovementType.PURCHASE, qty, rate,
                    SourceType.PURCHASE, purchase.getId(), date);
            item.setAvgCost(newAvgCost);

            PurchaseLine purchaseLine = new PurchaseLine();
            purchaseLine.setItem(item);
            purchaseLine.setQty(qty);
            purchaseLine.setRate(rate);
            purchaseLine.setAmount(amount);
            purchase.addLine(purchaseLine);
        }

        return PurchaseResponse.from(purchaseRepository.save(purchase));
    }

    @Transactional(readOnly = true)
    public List<PurchaseResponse> list() {
        return purchaseRepository.findAllByOrderByPurchaseDateDescIdDesc().stream()
                .map(PurchaseResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseResponse get(Long id) {
        return purchaseRepository.findWithLinesById(id)
                .map(PurchaseResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found: " + id));
    }
}
