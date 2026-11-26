-- =====================================================================
-- V1__init_schema.sql   (Flyway, MySQL 8.0.16+ because CHECK constraints are enforced)
-- Order-to-Audit system for a small food manufacturer
--
-- Conventions
--   money      DECIMAL(14,2)   rates DECIMAL(12,4)   quantities DECIMAL(14,3)
--   percentages DECIMAL(7,2)   GST rate DECIMAL(5,2)
--   status / type columns are VARCHAR + CHECK (map with @Enumerated(STRING) in JPA)
--   stock balance is NEVER stored: it is SUM(stock_ledger.qty_delta)
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. Users and configuration
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)   NOT NULL,
    password_hash VARCHAR(100)  NOT NULL,
    role          VARCHAR(20)   NOT NULL,
    active        BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username),
    CONSTRAINT ck_users_role CHECK (role IN ('SALESMAN','ADMIN','PRODUCTION','ACCOUNTANT','OWNER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Key/value config: variance tolerance, factory state code, financial-year start month
CREATE TABLE settings (
    setting_key   VARCHAR(50)   NOT NULL,
    setting_value VARCHAR(100)  NOT NULL,
    PRIMARY KEY (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Gap-free document numbers per type and financial year (lock the row with SELECT ... FOR UPDATE)
CREATE TABLE doc_sequences (
    doc_type VARCHAR(10) NOT NULL,   -- ORD, INV, CN, PUR, RUN
    fy       VARCHAR(7)  NOT NULL,   -- e.g. 2026-27
    last_no  INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (doc_type, fy)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 2. Masters: BOM, items, customers, suppliers
--    boms is created before items because items.bom_id points to it
--    (that is what lets two pack sizes share one recipe)
-- ---------------------------------------------------------------------
CREATE TABLE boms (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100)  NOT NULL,
    yield_packets INT           NOT NULL,           -- expected packets per batch
    ref_pack_g    INT           NOT NULL,           -- pack size the yield refers to
    tolerance_pct DECIMAL(7,2)  NULL,               -- NULL = use settings.variance_tolerance_pct
    PRIMARY KEY (id),
    CONSTRAINT ck_boms_yield CHECK (yield_packets > 0),
    CONSTRAINT ck_boms_ref_pack CHECK (ref_pack_g > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE items (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    code             VARCHAR(30)   NOT NULL,
    name             VARCHAR(100)  NOT NULL,
    item_type        VARCHAR(12)   NOT NULL,        -- RAW, PACKAGING, FINISHED
    stock_unit       VARCHAR(5)    NOT NULL,        -- KG, L, PCS (ledger always holds this unit)
    hsn_code         VARCHAR(8)    NULL,            -- finished goods
    gst_rate         DECIMAL(5,2)  NULL,            -- finished goods, e.g. 5.00
    selling_price    DECIMAL(12,2) NULL,            -- finished goods, before tax
    avg_cost         DECIMAL(12,4) NOT NULL DEFAULT 0,   -- perpetual weighted average per stock unit
    pack_size_g      INT           NULL,            -- finished goods
    packs_per_carton INT           NULL,            -- finished goods
    reorder_level    DECIMAL(14,3) NULL,            -- stretch: low-stock alerts
    bom_id           BIGINT        NULL,            -- finished goods only
    active           BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_items_code (code),
    KEY ix_items_type (item_type),
    CONSTRAINT fk_items_bom FOREIGN KEY (bom_id) REFERENCES boms (id),
    CONSTRAINT ck_items_type CHECK (item_type IN ('RAW','PACKAGING','FINISHED')),
    CONSTRAINT ck_items_unit CHECK (stock_unit IN ('KG','L','PCS')),
    CONSTRAINT ck_items_gst CHECK (gst_rate IS NULL OR gst_rate BETWEEN 0 AND 100),
    CONSTRAINT ck_items_avg_cost CHECK (avg_cost >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE bom_lines (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    bom_id        BIGINT        NOT NULL,
    item_id       BIGINT        NOT NULL,
    qty_per_batch DECIMAL(14,3) NOT NULL,           -- in the item's stock unit
    PRIMARY KEY (id),
    UNIQUE KEY uk_bom_lines (bom_id, item_id),
    CONSTRAINT fk_bom_lines_bom  FOREIGN KEY (bom_id)  REFERENCES boms (id),
    CONSTRAINT fk_bom_lines_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT ck_bom_lines_qty CHECK (qty_per_batch > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE customers (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    name       VARCHAR(120) NOT NULL,
    type       VARCHAR(12)  NOT NULL,               -- RETAILER, DISTRIBUTOR
    gstin      VARCHAR(15)  NULL,
    state_code CHAR(2)      NOT NULL,               -- GST state code, 33 = Tamil Nadu, 29 = Karnataka
    phone      VARCHAR(15)  NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT ck_customers_type CHECK (type IN ('RETAILER','DISTRIBUTOR'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE suppliers (
    id    BIGINT       NOT NULL AUTO_INCREMENT,
    name  VARCHAR(120) NOT NULL,
    gstin VARCHAR(15)  NULL,
    phone VARCHAR(15)  NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3. Purchases
-- ---------------------------------------------------------------------
CREATE TABLE purchases (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    purchase_no      VARCHAR(25)  NOT NULL,
    supplier_id      BIGINT       NOT NULL,
    purchase_date    DATE         NOT NULL,
    supplier_bill_no VARCHAR(40)  NULL,
    created_by       BIGINT       NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_purchases_no (purchase_no),
    KEY ix_purchases_date (purchase_date),
    CONSTRAINT fk_purchases_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers (id),
    CONSTRAINT fk_purchases_user     FOREIGN KEY (created_by)  REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE purchase_lines (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    purchase_id BIGINT        NOT NULL,
    item_id     BIGINT        NOT NULL,
    qty         DECIMAL(14,3) NOT NULL,             -- already converted to the item's stock unit
    rate        DECIMAL(12,4) NOT NULL,             -- per stock unit
    amount      DECIMAL(14,2) NOT NULL,             -- qty * rate, rounded HALF_UP once
    PRIMARY KEY (id),
    KEY ix_purchase_lines_purchase (purchase_id),
    CONSTRAINT fk_purchase_lines_purchase FOREIGN KEY (purchase_id) REFERENCES purchases (id),
    CONSTRAINT fk_purchase_lines_item     FOREIGN KEY (item_id)     REFERENCES items (id),
    CONSTRAINT ck_purchase_lines_qty  CHECK (qty > 0),
    CONSTRAINT ck_purchase_lines_rate CHECK (rate >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 4. Production
-- ---------------------------------------------------------------------
CREATE TABLE production_runs (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    run_no             VARCHAR(25)   NOT NULL,
    product_id         BIGINT        NOT NULL,      -- finished item produced
    batches            DECIMAL(7,2)  NOT NULL,
    expected_output    INT           NOT NULL,      -- bom.yield_packets * batches
    actual_output      INT           NULL,          -- set on completion
    wastage_pcs        INT           NOT NULL DEFAULT 0,
    yield_variance_pct DECIMAL(7,2)  NULL,
    yield_flagged      BOOLEAN       NOT NULL DEFAULT FALSE,
    status             VARCHAR(10)   NOT NULL DEFAULT 'DRAFT',
    run_date           DATE          NOT NULL,
    batch_no           VARCHAR(30)   NULL,          -- cheap hook for batch/expiry stretch goal
    created_by         BIGINT        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_runs_no (run_no),
    KEY ix_runs_date (run_date),
    CONSTRAINT fk_runs_product FOREIGN KEY (product_id) REFERENCES items (id),
    CONSTRAINT fk_runs_user    FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_runs_status  CHECK (status IN ('DRAFT','ISSUED','COMPLETED')),
    CONSTRAINT ck_runs_batches CHECK (batches > 0),
    CONSTRAINT ck_runs_output  CHECK (actual_output IS NULL OR actual_output >= 0),
    CONSTRAINT ck_runs_wastage CHECK (wastage_pcs >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE run_lines (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    run_id       BIGINT        NOT NULL,
    item_id      BIGINT        NOT NULL,
    planned_qty  DECIMAL(14,3) NOT NULL,            -- bom.qty_per_batch * batches (0 for an extra issue)
    actual_qty   DECIMAL(14,3) NOT NULL DEFAULT 0,  -- accumulates as issues are posted
    variance_pct DECIMAL(7,2)  NULL,                -- set on completion
    flagged      BOOLEAN       NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    UNIQUE KEY uk_run_lines (run_id, item_id),
    CONSTRAINT fk_run_lines_run  FOREIGN KEY (run_id)  REFERENCES production_runs (id),
    CONSTRAINT fk_run_lines_item FOREIGN KEY (item_id) REFERENCES items (id),
    CONSTRAINT ck_run_lines_qty CHECK (planned_qty >= 0 AND actual_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE run_overheads (
    id     BIGINT        NOT NULL AUTO_INCREMENT,
    run_id BIGINT        NOT NULL,
    type   VARCHAR(12)   NOT NULL,                  -- LABOUR, GAS_POWER, OTHER
    amount DECIMAL(14,2) NOT NULL,
    note   VARCHAR(200)  NULL,
    PRIMARY KEY (id),
    KEY ix_run_overheads_run (run_id),
    CONSTRAINT fk_run_overheads_run FOREIGN KEY (run_id) REFERENCES production_runs (id),
    CONSTRAINT ck_run_overheads_type   CHECK (type IN ('LABOUR','GAS_POWER','OTHER')),
    CONSTRAINT ck_run_overheads_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 5. Stock ledger (insert-only passbook)
--    source_type + source_id is a polymorphic link, so it has no FK
-- ---------------------------------------------------------------------
CREATE TABLE stock_ledger (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    item_id       BIGINT        NOT NULL,
    movement_date DATE          NOT NULL,
    movement_type VARCHAR(15)   NOT NULL,
    qty_delta     DECIMAL(14,3) NOT NULL,           -- signed, in the item's stock unit
    unit_cost     DECIMAL(12,4) NOT NULL DEFAULT 0,
    source_type   VARCHAR(15)   NOT NULL,
    source_id     BIGINT        NOT NULL,
    created_by    BIGINT        NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_ledger_item_date (item_id, movement_date),
    KEY ix_ledger_source (source_type, source_id),
    CONSTRAINT fk_ledger_item FOREIGN KEY (item_id)    REFERENCES items (id),
    CONSTRAINT fk_ledger_user FOREIGN KEY (created_by) REFERENCES users (id),
    CONSTRAINT ck_ledger_type CHECK (movement_type IN
        ('OPENING','PURCHASE','PROD_ISSUE','PROD_OUTPUT','SALE_DISPATCH','SALE_RETURN','ADJUSTMENT')),
    CONSTRAINT ck_ledger_source CHECK (source_type IN
        ('OPENING','PURCHASE','PRODUCTION_RUN','INVOICE','CREDIT_NOTE','ADJUSTMENT')),
    CONSTRAINT ck_ledger_delta CHECK (qty_delta <> 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Ledger rows can never be changed or removed, even by a bug or a manual SQL slip
CREATE TRIGGER trg_stock_ledger_no_update BEFORE UPDATE ON stock_ledger
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'stock_ledger is insert-only';

CREATE TRIGGER trg_stock_ledger_no_delete BEFORE DELETE ON stock_ledger
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'stock_ledger is insert-only';

-- Current balance per item, always derived from the ledger
CREATE VIEW v_stock_balance AS
SELECT i.id        AS item_id,
       i.code      AS item_code,
       i.name      AS item_name,
       i.item_type AS item_type,
       i.stock_unit AS stock_unit,
       COALESCE(SUM(l.qty_delta), 0) AS balance
FROM items i
LEFT JOIN stock_ledger l ON l.item_id = i.id
GROUP BY i.id, i.code, i.name, i.item_type, i.stock_unit;

-- ---------------------------------------------------------------------
-- 6. Sales: orders, invoices, dispatch, payments, credit notes
-- ---------------------------------------------------------------------
CREATE TABLE orders (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    order_no    VARCHAR(25)  NOT NULL,
    client_uuid CHAR(36)     NOT NULL,              -- generated on the phone; makes retries idempotent
    customer_id BIGINT       NOT NULL,
    salesman_id BIGINT       NOT NULL,
    status      VARCHAR(12)  NOT NULL DEFAULT 'SUBMITTED',
    note        VARCHAR(255) NULL,
    decided_by  BIGINT       NULL,                  -- admin who approved or rejected
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_no (order_no),
    UNIQUE KEY uk_orders_client_uuid (client_uuid),
    KEY ix_orders_status (status),
    KEY ix_orders_salesman (salesman_id, created_at),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_orders_salesman FOREIGN KEY (salesman_id) REFERENCES users (id),
    CONSTRAINT fk_orders_decider  FOREIGN KEY (decided_by)  REFERENCES users (id),
    CONSTRAINT ck_orders_status CHECK (status IN
        ('SUBMITTED','APPROVED','REJECTED','INVOICED','DISPATCHED','CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_lines (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    order_id     BIGINT        NOT NULL,
    item_id      BIGINT        NOT NULL,
    ordered_qty  INT           NOT NULL,
    approved_qty INT           NULL,                -- admin may change it
    unit_price   DECIMAL(12,2) NOT NULL,            -- price snapshot at order time
    PRIMARY KEY (id),
    KEY ix_order_lines_order (order_id),
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_lines_item  FOREIGN KEY (item_id)  REFERENCES items (id),
    CONSTRAINT ck_order_lines_qty CHECK (ordered_qty > 0 AND (approved_qty IS NULL OR approved_qty >= 0)),
    CONSTRAINT ck_order_lines_price CHECK (unit_price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoices (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_no      VARCHAR(25)   NOT NULL,         -- INV/2026-27/0001
    order_id        BIGINT        NOT NULL,
    customer_id     BIGINT        NOT NULL,
    invoice_date    DATE          NOT NULL,
    supply_type     VARCHAR(5)    NOT NULL,         -- INTRA (CGST+SGST) or INTER (IGST)
    place_of_supply CHAR(2)       NOT NULL,
    customer_gstin  VARCHAR(15)   NULL,             -- snapshot
    taxable_value   DECIMAL(14,2) NOT NULL,
    cgst            DECIMAL(14,2) NOT NULL DEFAULT 0,
    sgst            DECIMAL(14,2) NOT NULL DEFAULT 0,
    igst            DECIMAL(14,2) NOT NULL DEFAULT 0,
    total           DECIMAL(14,2) NOT NULL,
    status          VARCHAR(10)   NOT NULL DEFAULT 'POSTED',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoices_no (invoice_no),
    UNIQUE KEY uk_invoices_order (order_id),
    KEY ix_invoices_customer_date (customer_id, invoice_date),
    CONSTRAINT fk_invoices_order    FOREIGN KEY (order_id)    REFERENCES orders (id),
    CONSTRAINT fk_invoices_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_invoices_supply CHECK (supply_type IN ('INTRA','INTER')),
    CONSTRAINT ck_invoices_status CHECK (status IN ('POSTED','CANCELLED')),
    CONSTRAINT ck_invoices_total  CHECK (total = taxable_value + cgst + sgst + igst)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE invoice_lines (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_id    BIGINT        NOT NULL,
    item_id       BIGINT        NOT NULL,
    hsn_code      VARCHAR(8)    NOT NULL,           -- snapshots, so master edits never change a posted invoice
    qty           INT           NOT NULL,
    rate          DECIMAL(12,2) NOT NULL,
    taxable_value DECIMAL(14,2) NOT NULL,
    gst_rate      DECIMAL(5,2)  NOT NULL,
    cgst          DECIMAL(14,2) NOT NULL DEFAULT 0,
    sgst          DECIMAL(14,2) NOT NULL DEFAULT 0,
    igst          DECIMAL(14,2) NOT NULL DEFAULT 0,
    line_total    DECIMAL(14,2) NOT NULL,
    PRIMARY KEY (id),
    KEY ix_invoice_lines_invoice (invoice_id),
    CONSTRAINT fk_invoice_lines_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_invoice_lines_item    FOREIGN KEY (item_id)    REFERENCES items (id),
    CONSTRAINT ck_invoice_lines_qty CHECK (qty > 0),
    CONSTRAINT ck_invoice_lines_total CHECK (line_total = taxable_value + cgst + sgst + igst)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE dispatches (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id    BIGINT NOT NULL,
    dispatch_date DATE   NOT NULL,
    dispatched_by BIGINT NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dispatches_invoice (invoice_id),  -- whole-invoice dispatch only (assumption)
    CONSTRAINT fk_dispatches_invoice FOREIGN KEY (invoice_id)    REFERENCES invoices (id),
    CONSTRAINT fk_dispatches_user    FOREIGN KEY (dispatched_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    invoice_id  BIGINT        NOT NULL,
    amount      DECIMAL(14,2) NOT NULL,
    paid_on     DATE          NOT NULL,
    mode        VARCHAR(10)   NOT NULL,             -- CASH, UPI, BANK, CHEQUE
    reference   VARCHAR(60)   NULL,
    received_by BIGINT        NOT NULL,
    PRIMARY KEY (id),
    KEY ix_payments_invoice (invoice_id),
    KEY ix_payments_date (paid_on),
    CONSTRAINT fk_payments_invoice FOREIGN KEY (invoice_id)  REFERENCES invoices (id),
    CONSTRAINT fk_payments_user    FOREIGN KEY (received_by) REFERENCES users (id),
    CONSTRAINT ck_payments_amount CHECK (amount > 0),
    CONSTRAINT ck_payments_mode   CHECK (mode IN ('CASH','UPI','BANK','CHEQUE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE credit_notes (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    cn_no         VARCHAR(25)   NOT NULL,           -- CN/2026-27/0001
    invoice_id    BIGINT        NOT NULL,
    reason        VARCHAR(255)  NOT NULL,
    taxable_value DECIMAL(14,2) NOT NULL,
    cgst          DECIMAL(14,2) NOT NULL DEFAULT 0,
    sgst          DECIMAL(14,2) NOT NULL DEFAULT 0,
    igst          DECIMAL(14,2) NOT NULL DEFAULT 0,
    total         DECIMAL(14,2) NOT NULL,
    created_by    BIGINT        NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_credit_notes_no (cn_no),
    UNIQUE KEY uk_credit_notes_invoice (invoice_id), -- full cancellation only (assumption)
    CONSTRAINT fk_credit_notes_invoice FOREIGN KEY (invoice_id) REFERENCES invoices (id),
    CONSTRAINT fk_credit_notes_user    FOREIGN KEY (created_by) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 7. Audit trail (insert-only)
-- ---------------------------------------------------------------------
CREATE TABLE audit_log (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    entity_type VARCHAR(30)  NOT NULL,
    entity_id   BIGINT       NOT NULL,
    action      VARCHAR(30)  NOT NULL,
    old_json    JSON         NULL,
    new_json    JSON         NULL,
    user_id     BIGINT       NOT NULL,
    at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY ix_audit_entity (entity_type, entity_id),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TRIGGER trg_audit_log_no_update BEFORE UPDATE ON audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log is insert-only';

CREATE TRIGGER trg_audit_log_no_delete BEFORE DELETE ON audit_log
FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'audit_log is insert-only';

-- ---------------------------------------------------------------------
-- 8. Default configuration (benchmark master data goes in V2__seed_benchmark.sql)
-- ---------------------------------------------------------------------
INSERT INTO settings (setting_key, setting_value) VALUES
    ('variance_tolerance_pct', '3.00'),   -- flag only when |variance| is strictly greater
    ('factory_state_code',     '33'),     -- Tamil Nadu
    ('fy_start_month',         '4');      -- financial year April to March