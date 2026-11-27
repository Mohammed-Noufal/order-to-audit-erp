package com.orderaudit.order_to_audit_erp.production;

import java.math.BigDecimal;

import com.orderaudit.order_to_audit_erp.master.Item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "run_lines")
public class RunLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private ProductionRun run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    // What the BOM says should be used (0 for a material that is not in the BOM)
    @Column(name = "planned_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal plannedQty;

    // What was really issued so far, accumulated as issues are posted
    @Column(name = "actual_qty", nullable = false, precision = 14, scale = 3)
    private BigDecimal actualQty = BigDecimal.ZERO.setScale(3);

    @Column(name = "variance_pct", precision = 7, scale = 2)
    private BigDecimal variancePct;

    @Column(name = "flagged", nullable = false)
    private boolean flagged = false;

    public Long getId() { return id; }
    public ProductionRun getRun() { return run; }
    public void setRun(ProductionRun run) { this.run = run; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public BigDecimal getPlannedQty() { return plannedQty; }
    public void setPlannedQty(BigDecimal plannedQty) { this.plannedQty = plannedQty; }
    public BigDecimal getActualQty() { return actualQty; }
    public void setActualQty(BigDecimal actualQty) { this.actualQty = actualQty; }
    public BigDecimal getVariancePct() { return variancePct; }
    public void setVariancePct(BigDecimal variancePct) { this.variancePct = variancePct; }
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
}
