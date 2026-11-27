package com.orderaudit.order_to_audit_erp.master;

import java.math.BigDecimal;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "items")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 30)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "item_type", nullable = false, length = 12)
    private ItemType itemType;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "stock_unit", nullable = false, length = 5)
    private StockUnit stockUnit;

    @Column(name = "hsn_code", length = 8)
    private String hsnCode;

    @Column(name = "gst_rate", precision = 5, scale = 2)
    private BigDecimal gstRate;

    @Column(name = "selling_price", precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    // Perpetual weighted-average cost per stock unit. Maintained by purchases (Checkpoint 4).
    @Column(name = "avg_cost", nullable = false, precision = 12, scale = 4)
    private BigDecimal avgCost = BigDecimal.ZERO;

    @Column(name = "pack_size_g")
    private Integer packSizeG;

    @Column(name = "packs_per_carton")
    private Integer packsPerCarton;

    @Column(name = "reorder_level", precision = 14, scale = 3)
    private BigDecimal reorderLevel;

    @Column(name = "bom_id")
    private Long bomId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ItemType getItemType() { return itemType; }
    public void setItemType(ItemType itemType) { this.itemType = itemType; }
    public StockUnit getStockUnit() { return stockUnit; }
    public void setStockUnit(StockUnit stockUnit) { this.stockUnit = stockUnit; }
    public String getHsnCode() { return hsnCode; }
    public void setHsnCode(String hsnCode) { this.hsnCode = hsnCode; }
    public BigDecimal getGstRate() { return gstRate; }
    public void setGstRate(BigDecimal gstRate) { this.gstRate = gstRate; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public BigDecimal getAvgCost() { return avgCost; }
    public void setAvgCost(BigDecimal avgCost) { this.avgCost = avgCost; }
    public Integer getPackSizeG() { return packSizeG; }
    public void setPackSizeG(Integer packSizeG) { this.packSizeG = packSizeG; }
    public Integer getPacksPerCarton() { return packsPerCarton; }
    public void setPacksPerCarton(Integer packsPerCarton) { this.packsPerCarton = packsPerCarton; }
    public BigDecimal getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(BigDecimal reorderLevel) { this.reorderLevel = reorderLevel; }
    public Long getBomId() { return bomId; }
    public void setBomId(Long bomId) { this.bomId = bomId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
