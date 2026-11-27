package com.orderaudit.order_to_audit_erp.production;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.orderaudit.order_to_audit_erp.audit.AuditService;
import com.orderaudit.order_to_audit_erp.auth.CurrentUserService;
import com.orderaudit.order_to_audit_erp.common.BadRequestException;
import com.orderaudit.order_to_audit_erp.common.ConflictException;
import com.orderaudit.order_to_audit_erp.common.DocSequenceService;
import com.orderaudit.order_to_audit_erp.common.ResourceNotFoundException;
import com.orderaudit.order_to_audit_erp.costing.CostingService;
import com.orderaudit.order_to_audit_erp.costing.RunCostSummary;
import com.orderaudit.order_to_audit_erp.master.Bom;
import com.orderaudit.order_to_audit_erp.master.BomLine;
import com.orderaudit.order_to_audit_erp.master.BomRepository;
import com.orderaudit.order_to_audit_erp.master.Item;
import com.orderaudit.order_to_audit_erp.master.ItemRepository;
import com.orderaudit.order_to_audit_erp.master.ItemType;
import com.orderaudit.order_to_audit_erp.master.SettingRepository;
import com.orderaudit.order_to_audit_erp.stock.MovementType;
import com.orderaudit.order_to_audit_erp.stock.SourceType;
import com.orderaudit.order_to_audit_erp.stock.StockService;

@Service
public class ProductionService {

    private static final String ENTITY = "PRODUCTION_RUN";
    private static final BigDecimal DEFAULT_TOLERANCE = new BigDecimal("3.00");

    private final ProductionRunRepository runRepository;
    private final ItemRepository itemRepository;
    private final BomRepository bomRepository;
    private final SettingRepository settingRepository;
    private final StockService stockService;
    private final CostingService costingService;
    private final DocSequenceService docSequenceService;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;

    public ProductionService(ProductionRunRepository runRepository,
                             ItemRepository itemRepository,
                             BomRepository bomRepository,
                             SettingRepository settingRepository,
                             StockService stockService,
                             CostingService costingService,
                             DocSequenceService docSequenceService,
                             CurrentUserService currentUserService,
                             AuditService auditService) {
        this.runRepository = runRepository;
        this.itemRepository = itemRepository;
        this.bomRepository = bomRepository;
        this.settingRepository = settingRepository;
        this.stockService = stockService;
        this.costingService = costingService;
        this.docSequenceService = docSequenceService;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
    }

    // ------------------------------------------------------------------ 1. CREATE (DRAFT)

    @Transactional
    public ProductionRunResponse create(ProductionRequests.CreateRun request) {
        Item product = itemRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + request.productId()));
        if (!product.isActive() || product.getItemType() != ItemType.FINISHED || product.getBomId() == null) {
            throw new BadRequestException(
                    "Item " + product.getCode() + " is not an active finished product with a BOM");
        }
        Bom bom = bomRepository.findWithLinesById(product.getBomId())
                .orElseThrow(() -> new ResourceNotFoundException("BOM not found: " + product.getBomId()));
        if (bom.getLines().isEmpty()) {
            throw new BadRequestException("The BOM for " + product.getCode() + " has no lines");
        }

        LocalDate date = (request.runDate() != null) ? request.runDate() : LocalDate.now();
        if (date.isAfter(LocalDate.now())) {
            throw new BadRequestException("Run date cannot be in the future");
        }

        // A product sharing this BOM with a different pack size scales output and packaging
        // (200 g reference, 500 g product: ratio 0.4, so 200 packets per batch becomes 80)
        BigDecimal packRatio = packRatio(bom, product);
        int expectedOutput = BigDecimal.valueOf(bom.getYieldPackets())
                .multiply(request.batches())
                .multiply(packRatio)
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
        if (expectedOutput < 1) {
            throw new BadRequestException("Batch size is too small: expected output rounds to zero packets");
        }

        ProductionRun run = new ProductionRun();
        run.setRunNo(docSequenceService.next("RUN", date));
        run.setProduct(product);
        run.setBatches(request.batches());
        run.setExpectedOutput(expectedOutput);
        run.setStatus(RunStatus.DRAFT);
        run.setRunDate(date);
        run.setBatchNo(request.batchNo());
        run.setCreatedBy(currentUserService.requireCurrentUser().getId());

        for (BomLine bomLine : bom.getLines()) {
            BigDecimal planned = bomLine.getQtyPerBatch().multiply(request.batches());
            if (bomLine.getItem().getItemType() == ItemType.PACKAGING) {
                planned = planned.multiply(packRatio);
            }
            RunLine line = new RunLine();
            line.setItem(bomLine.getItem());
            line.setPlannedQty(planned.setScale(3, RoundingMode.HALF_UP));
            run.addLine(line);
        }

        ProductionRun saved = runRepository.save(run);
        auditService.log(ENTITY, saved.getId(), "CREATE", null,
                Map.of("runNo", saved.getRunNo(), "productId", product.getId(),
                        "batches", saved.getBatches(), "expectedOutput", expectedOutput));
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ 2. ISSUE (ISSUED)

    @Transactional
    public ProductionRunResponse issue(Long runId, ProductionRequests.Issue request) {
        ProductionRun run = requireLockedRun(runId);
        if (run.getStatus() == RunStatus.COMPLETED) {
            throw new ConflictException("Cannot issue materials to a COMPLETED run");
        }
        RunStatus before = run.getStatus();

        consume(run, request.lines(), EnumSet.of(ItemType.RAW, ItemType.PACKAGING));

        run.setStatus(RunStatus.ISSUED);
        ProductionRun saved = runRepository.save(run);
        auditService.log(ENTITY, saved.getId(), "ISSUE",
                Map.of("status", before),
                Map.of("status", RunStatus.ISSUED, "linesIssued", request.lines().size()));
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ 3. COMPLETE (COMPLETED)

    @Transactional
    public ProductionRunResponse complete(Long runId, ProductionRequests.Complete request) {
        ProductionRun run = requireLockedRun(runId);
        if (run.getStatus() != RunStatus.ISSUED) {
            throw new ConflictException(
                    "A run must be ISSUED before it can be completed (current status: " + run.getStatus() + ")");
        }
        int actualOutput = request.actualOutput();

        // Packaging used (pouches, cartons) leaves stock now
        if (request.packagingUsed() != null && !request.packagingUsed().isEmpty()) {
            consume(run, request.packagingUsed(), EnumSet.of(ItemType.PACKAGING));
        }

        // Good packets enter finished-goods stock, valued at this run's material cost per packet
        if (actualOutput > 0) {
            BigDecimal output = BigDecimal.valueOf(actualOutput);
            BigDecimal runUnitCost = costingService.materialCost(run.getId())
                    .divide(output, 4, RoundingMode.HALF_UP);

            Item product = itemRepository.lockById(run.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            BigDecimal stockBefore = stockService.balance(product.getId());
            BigDecimal newAvgCost = stockBefore.multiply(product.getAvgCost())
                    .add(output.multiply(runUnitCost))
                    .divide(stockBefore.add(output), 4, RoundingMode.HALF_UP);

            stockService.post(product, MovementType.PROD_OUTPUT, output, runUnitCost,
                    SourceType.PRODUCTION_RUN, run.getId(), run.getRunDate());
            product.setAvgCost(newAvgCost);
        }

        // Variance: planned vs actual per material, and expected vs actual yield
        BigDecimal tolerance = toleranceFor(run);
        for (RunLine line : run.getLines()) {
            if (line.getPlannedQty().signum() > 0) {
                BigDecimal exact = Variance.exactPct(line.getActualQty(), line.getPlannedQty());
                line.setVariancePct(Variance.display(exact));
                line.setFlagged(Variance.exceeds(exact, tolerance));
            } else {
                // Not in the BOM at all: anything issued is unplanned consumption
                line.setVariancePct(null);
                line.setFlagged(line.getActualQty().signum() > 0);
            }
        }
        BigDecimal yieldExact = Variance.exactPct(
                BigDecimal.valueOf(actualOutput), BigDecimal.valueOf(run.getExpectedOutput()));

        run.setActualOutput(actualOutput);
        run.setWastagePcs(request.wastagePcs());
        run.setYieldVariancePct(Variance.display(yieldExact));
        run.setYieldFlagged(Variance.exceeds(yieldExact, tolerance));
        run.setStatus(RunStatus.COMPLETED);

        ProductionRun saved = runRepository.save(run);
        auditService.log(ENTITY, saved.getId(), "COMPLETE",
                Map.of("status", RunStatus.ISSUED),
                Map.of("status", RunStatus.COMPLETED,
                        "actualOutput", actualOutput,
                        "wastagePcs", request.wastagePcs(),
                        "yieldVariancePct", saved.getYieldVariancePct(),
                        "yieldFlagged", saved.isYieldFlagged()));
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ OVERHEADS (any status)

    @Transactional
    public ProductionRunResponse addOverhead(Long runId, ProductionRequests.Overhead request) {
        ProductionRun run = requireLockedRun(runId);

        RunOverhead overhead = new RunOverhead();
        overhead.setType(request.type());
        overhead.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        overhead.setNote(request.note());
        run.addOverhead(overhead);

        ProductionRun saved = runRepository.save(run);
        auditService.log(ENTITY, saved.getId(), "ADD_OVERHEAD", null,
                Map.of("type", request.type(), "amount", overhead.getAmount()));
        return toResponse(saved);
    }

    // ------------------------------------------------------------------ READS

    @Transactional(readOnly = true)
    public List<ProductionRunResponse> list() {
        return runRepository.findAllByOrderByRunDateDescIdDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductionRunResponse get(Long runId) {
        return runRepository.findWithDetailsById(runId)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Production run not found: " + runId));
    }

    // ------------------------------------------------------------------ helpers

    private ProductionRun requireLockedRun(Long runId) {
        return runRepository.lockById(runId)
                .orElseThrow(() -> new ResourceNotFoundException("Production run not found: " + runId));
    }

    /** Takes materials out of stock for this run and adds them to the matching run line. */
    private void consume(ProductionRun run, List<ProductionRequests.QtyLine> lines, Set<ItemType> allowedTypes) {
        // Always lock items in the same order (by id) so two runs can never deadlock each other
        List<ProductionRequests.QtyLine> ordered = lines.stream()
                .sorted(Comparator.comparing(ProductionRequests.QtyLine::itemId))
                .toList();

        for (ProductionRequests.QtyLine input : ordered) {
            Item item = itemRepository.lockById(input.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found: " + input.itemId()));
            if (!item.isActive() || !allowedTypes.contains(item.getItemType())) {
                throw new BadRequestException("Item " + item.getCode() + " is not allowed here (it must be an active "
                        + allowedTypes + " item)");
            }
            BigDecimal qty = input.unit().toStockQty(input.qty(), item);
            if (qty.signum() <= 0) {
                throw new BadRequestException("Quantity for " + item.getCode()
                        + " is too small: it rounds to zero " + item.getStockUnit());
            }

            // Issued at the item's current average cost; that cost is frozen in the ledger row
            stockService.post(item, MovementType.PROD_ISSUE, qty.negate(), item.getAvgCost(),
                    SourceType.PRODUCTION_RUN, run.getId(), run.getRunDate());

            RunLine line = findOrCreateLine(run, item);
            line.setActualQty(line.getActualQty().add(qty));
        }
    }

    private RunLine findOrCreateLine(ProductionRun run, Item item) {
        return run.getLines().stream()
                .filter(l -> l.getItem().getId().equals(item.getId()))
                .findFirst()
                .orElseGet(() -> {
                    RunLine extra = new RunLine();
                    extra.setItem(item);
                    extra.setPlannedQty(BigDecimal.ZERO.setScale(3));
                    run.addLine(extra); // planned 0: any use of it will be flagged
                    return extra;
                });
    }

    private BigDecimal packRatio(Bom bom, Item product) {
        if (product.getPackSizeG() == null || product.getPackSizeG() <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(bom.getRefPackG())
                .divide(BigDecimal.valueOf(product.getPackSizeG()), 6, RoundingMode.HALF_UP);
    }

    private BigDecimal toleranceFor(ProductionRun run) {
        Bom bom = bomRepository.findById(run.getProduct().getBomId())
                .orElseThrow(() -> new ResourceNotFoundException("BOM not found for run " + run.getRunNo()));
        if (bom.getTolerancePct() != null) {
            return bom.getTolerancePct();
        }
        return settingRepository.findById("variance_tolerance_pct")
                .map(s -> new BigDecimal(s.getSettingValue()))
                .orElse(DEFAULT_TOLERANCE);
    }

    private ProductionRunResponse toResponse(ProductionRun run) {
        BigDecimal overheadTotal = run.getOverheads().stream()
                .map(RunOverhead::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        RunCostSummary costing = costingService.summarize(
                run.getId(), run.getActualOutput(), run.getProduct().getSellingPrice(), overheadTotal);
        return ProductionRunResponse.from(run, costing);
    }
}
