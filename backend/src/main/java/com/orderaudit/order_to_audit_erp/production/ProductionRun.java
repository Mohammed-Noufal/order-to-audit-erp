package com.orderaudit.order_to_audit_erp.production;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.orderaudit.order_to_audit_erp.master.Item;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "production_runs")
public class ProductionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_no", nullable = false, length = 25)
    private String runNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Item product;

    @Column(name = "batches", nullable = false, precision = 7, scale = 2)
    private BigDecimal batches;

    @Column(name = "expected_output", nullable = false)
    private Integer expectedOutput;

    @Column(name = "actual_output")
    private Integer actualOutput;

    @Column(name = "wastage_pcs", nullable = false)
    private Integer wastagePcs = 0;

    @Column(name = "yield_variance_pct", precision = 7, scale = 2)
    private BigDecimal yieldVariancePct;

    @Column(name = "yield_flagged", nullable = false)
    private boolean yieldFlagged = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 10)
    private RunStatus status = RunStatus.DRAFT;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(name = "batch_no", length = 30)
    private String batchNo;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    // Both lists are LAZY on purpose: two EAGER lists make Hibernate fail at startup
    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RunLine> lines = new ArrayList<>();

    @OneToMany(mappedBy = "run", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RunOverhead> overheads = new ArrayList<>();

    public Long getId() { return id; }
    public String getRunNo() { return runNo; }
    public void setRunNo(String runNo) { this.runNo = runNo; }
    public Item getProduct() { return product; }
    public void setProduct(Item product) { this.product = product; }
    public BigDecimal getBatches() { return batches; }
    public void setBatches(BigDecimal batches) { this.batches = batches; }
    public Integer getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(Integer expectedOutput) { this.expectedOutput = expectedOutput; }
    public Integer getActualOutput() { return actualOutput; }
    public void setActualOutput(Integer actualOutput) { this.actualOutput = actualOutput; }
    public Integer getWastagePcs() { return wastagePcs; }
    public void setWastagePcs(Integer wastagePcs) { this.wastagePcs = wastagePcs; }
    public BigDecimal getYieldVariancePct() { return yieldVariancePct; }
    public void setYieldVariancePct(BigDecimal yieldVariancePct) { this.yieldVariancePct = yieldVariancePct; }
    public boolean isYieldFlagged() { return yieldFlagged; }
    public void setYieldFlagged(boolean yieldFlagged) { this.yieldFlagged = yieldFlagged; }
    public RunStatus getStatus() { return status; }
    public void setStatus(RunStatus status) { this.status = status; }
    public LocalDate getRunDate() { return runDate; }
    public void setRunDate(LocalDate runDate) { this.runDate = runDate; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public List<RunLine> getLines() { return lines; }
    public List<RunOverhead> getOverheads() { return overheads; }

    public void addLine(RunLine line) {
        lines.add(line);
        line.setRun(this);
    }

    public void addOverhead(RunOverhead overhead) {
        overheads.add(overhead);
        overhead.setRun(this);
    }
}
