-- =====================================================================
-- V2__seed_benchmark.sql
-- Section 5 benchmark master data and opening stock.
-- Assumptions (write these in the README):
--   * Opening stock is dated 2026-04-01, the start of financial year 2026-27.
--   * Opening cost of finished Murukku is the standard material cost per packet:
--     (40*42 + 10*140 + 1.2*200 + 200*2.50 + 10*15) / 200 = 19.85
--     because the brief gives no opening cost for finished goods.
--   * Murukku HSN is set to 21069099 and GST to 5% as instructed by the brief.
--   * Customer, supplier and GSTIN values are invented dummy data.
-- No login accounts are created here. Dev accounts come from DevUserSeeder.
-- =====================================================================

-- 1. Technical owner of the OPENING ledger rows. Inactive and with no valid password hash,
--    so nobody can ever log in as this user.
INSERT INTO users (username, password_hash, role, active)
SELECT 'system', '!', 'ADMIN', FALSE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'system');

SET @system_user_id = (SELECT id FROM users WHERE username = 'system');

-- 2. BOM header: Murukku 200 g, one batch = 200 packets.
--    tolerance_pct is NULL so settings.variance_tolerance_pct (3.00) governs.
INSERT INTO boms (id, name, yield_packets, ref_pack_g, tolerance_pct) VALUES
    (1, 'Murukku 200 g Recipe', 200, 200, NULL);

-- 3. Items: raw, packaging, finished (finished goods point to the BOM)
INSERT INTO items (id, code, name, item_type, stock_unit, hsn_code, gst_rate, selling_price,
                   avg_cost, pack_size_g, packs_per_carton, reorder_level, bom_id, active) VALUES
    (1, 'RM-FLOUR',       'Flour',                       'RAW',       'KG',  NULL,       NULL, NULL,   42.0000, NULL, NULL,  50.000, NULL, TRUE),
    (2, 'RM-OIL',         'Cooking oil',                 'RAW',       'L',   NULL,       NULL, NULL,  140.0000, NULL, NULL,  15.000, NULL, TRUE),
    (3, 'RM-SPICE',       'Salt and spice mix',          'RAW',       'KG',  NULL,       NULL, NULL,  200.0000, NULL, NULL,   2.000, NULL, TRUE),
    (4, 'PKG-POUCH-200',  '200 g pouch',                 'PACKAGING', 'PCS', NULL,       NULL, NULL,    2.5000, NULL, NULL, 200.000, NULL, TRUE),
    (5, 'PKG-CARTON-20',  'Carton (holds 20 packets)',   'PACKAGING', 'PCS', NULL,       NULL, NULL,   15.0000, NULL, NULL,  15.000, NULL, TRUE),
    (6, 'FG-MURUKKU-200', 'Murukku 200 g',               'FINISHED',  'PCS', '21069099', 5.00, 50.00,  19.8500, 200,  20,    50.000, 1,    TRUE);

-- 4. BOM lines: quantity per batch, in each item's stock unit
INSERT INTO bom_lines (bom_id, item_id, qty_per_batch) VALUES
    (1, 1,  40.000),   -- flour 40 kg
    (1, 2,  10.000),   -- cooking oil 10 L
    (1, 3,   1.200),   -- salt and spice mix 1.2 kg
    (1, 4, 200.000),   -- pouches 200 pcs
    (1, 5,  10.000);   -- cartons 10 pcs

-- 5. Customers (state code 33 = Tamil Nadu, 29 = Karnataka)
INSERT INTO customers (id, name, type, gstin, state_code, phone, active) VALUES
    (1, 'Retailer A',    'RETAILER',    '33AABCR1234A1Z5', '33', '9840011111', TRUE),
    (2, 'Distributor B', 'DISTRIBUTOR', '29AABCD5678B1Z9', '29', '9840022222', TRUE);

-- 6. Supplier
INSERT INTO suppliers (id, name, gstin, phone) VALUES
    (1, 'Cauvery Agro Mills', '33AABCC9999K1Z1', '9443012345');

-- 7. Opening stock as OPENING rows in the ledger (the passbook starts here)
INSERT INTO stock_ledger (item_id, movement_date, movement_type, qty_delta, unit_cost,
                          source_type, source_id, created_by) VALUES
    (1, '2026-04-01', 'OPENING', 100.000,  42.0000, 'OPENING', 0, @system_user_id),
    (2, '2026-04-01', 'OPENING',  30.000, 140.0000, 'OPENING', 0, @system_user_id),
    (3, '2026-04-01', 'OPENING',   5.000, 200.0000, 'OPENING', 0, @system_user_id),
    (4, '2026-04-01', 'OPENING', 500.000,   2.5000, 'OPENING', 0, @system_user_id),
    (5, '2026-04-01', 'OPENING',  30.000,  15.0000, 'OPENING', 0, @system_user_id),
    (6, '2026-04-01', 'OPENING',  50.000,  19.8500, 'OPENING', 0, @system_user_id);
