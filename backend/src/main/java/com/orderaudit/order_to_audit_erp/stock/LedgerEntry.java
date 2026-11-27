package com.orderaudit.order_to_audit_erp.stock;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.orderaudit.order_to_audit_erp.master.Item;

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
import jakarta.persistence.Table;

/** One passbook line. @Immutable: Hibernate will never issue an UPDATE for it (the DB trigger forbids it too). */
@Entity
@Immutable
@Table(name = "stock_ledger")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "movement_date", nullable = false)
    private LocalDate movementDate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "movement_type", nullable = false, length = 15)
    private MovementType movementType;

    @Column(name = "qty_delta", nullable = false, precision = 14, scale = 3)
    private BigDecimal qtyDelta;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal unitCost;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "source_type", nullable = false, length = 15)
    private SourceType sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    // Filled by the database default
    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected LedgerEntry() {
        // required by JPA
    }

    public LedgerEntry(Item item, LocalDate movementDate, MovementType movementType,
                       BigDecimal qtyDelta, BigDecimal unitCost,
                       SourceType sourceType, Long sourceId, Long createdBy) {
        this.item = item;
        this.movementDate = movementDate;
        this.movementType = movementType;
        this.qtyDelta = qtyDelta;
        this.unitCost = unitCost;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public Item getItem() { return item; }
    public LocalDate getMovementDate() { return movementDate; }
    public MovementType getMovementType() { return movementType; }
    public BigDecimal getQtyDelta() { return qtyDelta; }
    public BigDecimal getUnitCost() { return unitCost; }
    public SourceType getSourceType() { return sourceType; }
    public Long getSourceId() { return sourceId; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
