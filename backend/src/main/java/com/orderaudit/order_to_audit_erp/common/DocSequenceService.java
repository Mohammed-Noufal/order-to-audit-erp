package com.orderaudit.order_to_audit_erp.common;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

@Service
public class DocSequenceService {

    private static final int DEFAULT_FY_START_MONTH = 4; // April, as in the Indian financial year

    private final EntityManager em;

    public DocSequenceService(EntityManager em) {
        this.em = em;
    }

    /** 2026-04-02 -> "2026-27", 2027-01-15 -> "2026-27" */
    public String financialYear(LocalDate date) {
        int startMonth = readFyStartMonth();
        int startYear = date.getMonthValue() >= startMonth ? date.getYear() : date.getYear() - 1;
        return "%d-%02d".formatted(startYear, (startYear + 1) % 100);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String next(String docType, LocalDate date) {
        String fy = financialYear(date);

        // 1. Make sure the counter row exists (no-op if it already does)
        em.createNativeQuery("""
                INSERT INTO doc_sequences (doc_type, fy, last_no) VALUES (:docType, :fy, 0)
                ON DUPLICATE KEY UPDATE last_no = last_no
                """)
                .setParameter("docType", docType)
                .setParameter("fy", fy)
                .executeUpdate();

        // 2. Lock the row so two requests can never read the same number
        Number current = (Number) em.createNativeQuery(
                        "SELECT last_no FROM doc_sequences WHERE doc_type = :docType AND fy = :fy FOR UPDATE")
                .setParameter("docType", docType)
                .setParameter("fy", fy)
                .getSingleResult();

        // 3. Increment
        int nextNo = current.intValue() + 1;
        em.createNativeQuery(
                        "UPDATE doc_sequences SET last_no = :nextNo WHERE doc_type = :docType AND fy = :fy")
                .setParameter("nextNo", nextNo)
                .setParameter("docType", docType)
                .setParameter("fy", fy)
                .executeUpdate();

        return "%s/%s/%04d".formatted(docType, fy, nextNo);
    }

    private int readFyStartMonth() {
        List<?> rows = em.createNativeQuery(
                "SELECT setting_value FROM settings WHERE setting_key = 'fy_start_month'").getResultList();
        return rows.isEmpty() ? DEFAULT_FY_START_MONTH : Integer.parseInt(rows.get(0).toString());
    }
}
