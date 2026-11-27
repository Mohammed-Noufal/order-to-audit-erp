#!/usr/bin/env bash
# Run from inside your cloned repo:  bash setup-board.sh
# Needs: GitHub CLI (gh) logged in with the project scope:
#   gh auth login
#   gh auth refresh -s project
set -e

PROJECT_TITLE="Order-to-Audit Board"
REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
OWNER=${REPO%%/*}
echo "Setting up board for $REPO"

# ---- Labels -------------------------------------------------------------
label() { gh label create "$1" --color "$2" --description "$3" --force >/dev/null; }
label "must"      "d73a4a" "Must have"
label "should"    "fbca04" "Should have"
label "stretch"   "0e8a16" "Stretch, only if all else is green"
label "auth"      "1d76db" "Auth and roles"
label "master"    "5319e7" "Master data"
label "stock"     "0052cc" "Ledger and stock"
label "production" "b60205" "Production, BOM, costing"
label "sales"     "006b75" "Orders, invoices, dispatch"
label "reports"   "c5def5" "Reports and exports"
label "testing"   "bfd4f2" "Tests"
label "docs"      "ededed" "README, design note, demo"

# ---- Sprints as milestones (set due dates later in the GitHub UI) -------
sprint() {
  gh api "repos/$REPO/milestones" -f title="$1" -f description="$2" >/dev/null 2>&1 \
    || echo "  (milestone '$1' already exists)"
}
S1="Sprint 1 - Foundation"
S2="Sprint 2 - Stock and Production"
S3="Sprint 3 - Sales and Money"
S4="Sprint 4 - Reports and Hardening"
sprint "$S1" "Skeleton, schema, auth, masters + seed data"
sprint "$S2" "Ledger, purchases, production runs, costing"
sprint "$S3" "Orders, GST invoice, dispatch, payments"
sprint "$S4" "Reports, audit, credit notes, tests, README, demo"

# ---- Project board --------------------------------------------------------
PROJECT_NUMBER=$(gh project create --owner "$OWNER" --title "$PROJECT_TITLE" --format json -q .number)
gh project link "$PROJECT_NUMBER" --owner "$OWNER" --repo "$REPO" >/dev/null || true
echo "Created project #$PROJECT_NUMBER"

# ---- Issues -----------------------------------------------------------------
issue() { # title | milestone | labels | body
  gh issue create --title "$1" --milestone "$2" --label "$3" --body "$4" \
    --project "$PROJECT_TITLE" >/dev/null
  echo "  + $1"
}

# Sprint 1
issue "Project skeleton, docker-compose, Flyway V1 schema" "$S1" "must,master" \
"Spring Boot 3 + MySQL 8 via docker-compose. Flyway V1 with the full 18-table schema.
**Done when:** app starts and all tables exist.
Commit: \`chore: project skeleton and schema\`"
issue "Auth: users table, BCrypt, JWT filter, 5 seeded users" "$S1" "must,auth" \
"Roles: SALESMAN, ADMIN, PRODUCTION, ACCOUNTANT, OWNER. Login, /auth/me, /users.
**Done when:** salesman token gets 403 on /stock."
issue "@PreAuthorize on controllers + salesman query filter in service" "$S1" "must,auth" \
"Role rules per section 5 of the design. Salesman queries filtered by logged-in user id inside the service."
issue "Masters CRUD: items, customers, suppliers, settings" "$S1" "must,master" \
"Items with unit, HSN, GST rate, price, pack_size_g, packs_per_carton, bom_id. Settings: tolerance %, factory state code."
issue "BOM CRUD (header + lines)" "$S1" "must,master" \
"BOMS separate from ITEMS; items.bom_id points to it."
issue "Flyway V2: seed master data + OPENING ledger rows" "$S1" "must,master,stock" \
"**Done when:** GET /stock shows flour 100, oil 30, spice 5, pouches 500, cartons 30, FG 50."

# Sprint 2
issue "Unit enum + conversion (g/ml -> kg/L) with unit tests" "$S2" "must,stock,testing" \
"10500 ML -> 10.500 L; mismatched dimension rejected."
issue "StockService.post() - the only door to the ledger" "$S2" "must,stock" \
"Row lock on item, negative-stock check, propagation MANDATORY.
Tests: negative balance throws and writes no row."
issue "Purchase endpoint with weighted average cost" "$S2" "must,stock" \
"**Done when:** benchmark step 1 gives flour 160 kg, avg cost 43.1250, ledger shows source."
issue "Stock summary + stock ledger endpoints" "$S2" "must,stock" \
"GET /stock?type=, GET /stock/ledger?itemId&from&to. Balance = SUM of ledger."
issue "Production run: create (DRAFT) and issue materials" "$S2" "must,production" \
"Planned lines from BOM x batches. Issue posts PROD_ISSUE rows. Extra materials allowed, flagged."
issue "Production run: complete, variance and flags" "$S2" "must,production,testing" \
"Packaging issue, PROD_OUTPUT, line variance, yield variance. Flag only when strictly beyond tolerance.
Tests: oil +5.00 flagged, exactly 3.00 not flagged, yield -6.00 flagged."
issue "Run overheads + costing computed on read" "$S2" "must,production" \
"**Done when:** benchmark step 8 gives 31.14 per packet, margin 18.86 (37.7%)."
issue "Stock adjustments (reason mandatory, audited)" "$S2" "should,stock" \
"POST /stock/adjustments for PRODUCTION and ADMIN."

# Sprint 3
issue "Submit order, idempotent via clientUuid" "$S3" "must,sales" \
"Replay returns the same order. UNIQUE index on client_uuid. Price snapshot on lines. DocSequence numbering."
issue "Order list/detail, stock-check, edit while SUBMITTED" "$S3" "must,sales" \
"Salesman sees own orders only. Stock check aggregates per item."
issue "Approve / reject with allowBackorder" "$S3" "must,sales" \
"Shortage without allowBackorder -> 409 with shortage list."
issue "GST invoice generation (CGST/SGST vs IGST)" "$S3" "must,sales,testing" \
"FY April-March, gap-free numbering INV/2026-27/0001, per-line rounding HALF_UP.
GstCalculatorTest: 7,500 at 5% intra = 187.50 + 187.50; inter = IGST 375."
issue "Dispatch: ledger out, order DISPATCHED" "$S3" "must,sales,stock" \
"Any shortage throws 409 and nothing is written."
issue "Payments + customer outstanding with ageing" "$S3" "must,sales" \
"Overpayment -> 409. **Done when:** RetailerA outstanding is 3,875."

# Sprint 4
issue "Invoice PDF (OpenPDF)" "$S4" "must,reports" \
"GET /invoices/{id}/pdf."
issue "Exporter (json/csv/xlsx/pdf) + sales register + stock reports" "$S4" "must,reports" \
"One shared Exporter, one query class per report."
issue "GST summary, purchase, production, outstanding reports" "$S4" "should,reports" \
"GST groups by supply_type and gst_rate, B2B vs B2C, minus credit notes."
issue "Cancel invoice with credit note + stock reversal" "$S4" "should,sales" \
"Reason mandatory, 409 if payments exist, SALE_RETURN if dispatched."
issue "Audit trail (AuditService) + GET /audit" "$S4" "should" \
"Explicit calls from services, JSON before/after snapshots."
issue "Owner dashboard" "$S4" "should,reports" \
"Today's orders, stock levels, flagged runs, cost per packet, total outstanding."
issue "BenchmarkIT: replay steps 1-9 and assert results table" "$S4" "must,testing" \
"SpringBootTest + Testcontainers. This is the proof for the 30% benchmark criterion."
issue "README, design note, ER diagram, assumptions, known gaps" "$S4" "must,docs" \
"Stack justification, test logins per role, benchmark table with the two explanations, assumptions list."
issue "Demo video (follow section 9 in order) + deploy" "$S4" "must,docs" \
"Record the benchmark walkthrough steps 1-9."
issue "Stretch: low-stock alerts" "$S4" "stretch,stock" \
"reorder_level already exists as a column."
issue "Stretch: production planning from pending orders" "$S4" "stretch,production" \
"SUM approved qty minus stock, divided by yield, rounded up to whole batches."

echo
echo "Done. Open: https://github.com/users/$OWNER/projects/$PROJECT_NUMBER"
echo "Next: in the project, click '+ New view' -> Board, and group by Status."