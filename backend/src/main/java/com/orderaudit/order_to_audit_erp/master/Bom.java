package com.orderaudit.order_to_audit_erp.master;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "boms")
public class Bom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    // Expected packets per batch, for packs of ref_pack_g grams
    @Column(name = "yield_packets", nullable = false)
    private Integer yieldPackets;

    @Column(name = "ref_pack_g", nullable = false)
    private Integer refPackG;

    // NULL means "use settings.variance_tolerance_pct"
    @Column(name = "tolerance_pct", precision = 7, scale = 2)
    private BigDecimal tolerancePct;

    @OneToMany(mappedBy = "bom", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<BomLine> lines = new ArrayList<>();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getYieldPackets() { return yieldPackets; }
    public void setYieldPackets(Integer yieldPackets) { this.yieldPackets = yieldPackets; }
    public Integer getRefPackG() { return refPackG; }
    public void setRefPackG(Integer refPackG) { this.refPackG = refPackG; }
    public BigDecimal getTolerancePct() { return tolerancePct; }
    public void setTolerancePct(BigDecimal tolerancePct) { this.tolerancePct = tolerancePct; }
    public List<BomLine> getLines() { return lines; }

    public void addLine(BomLine line) {
        lines.add(line);
        line.setBom(this);
    }

    public void removeLine(BomLine line) {
        lines.remove(line); // orphanRemoval deletes the row
    }
}
