package com.orderaudit.order_to_audit_erp.audit;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.orderaudit.order_to_audit_erp.auth.CurrentUserService;

import jakarta.persistence.EntityManager;

/**
 * Writes one insert-only audit_log row per important change, in the same transaction as the change.
 * Snapshots are small flat maps, so the JSON is built by hand (no dependency on a JSON library).
 */
@Service
public class AuditService {

    private final EntityManager em;
    private final CurrentUserService currentUserService;

    public AuditService(EntityManager em, CurrentUserService currentUserService) {
        this.em = em;
        this.currentUserService = currentUserService;
    }

    /** before / after may be null or empty (stored as SQL NULL). Values: String, Number, Boolean or enum. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void log(String entityType, Long entityId, String action,
                    Map<String, ?> before, Map<String, ?> after) {
        Long userId = currentUserService.requireCurrentUser().getId();
        em.createNativeQuery("""
                INSERT INTO audit_log (entity_type, entity_id, action, old_json, new_json, user_id)
                VALUES (:entityType, :entityId, :action,
                        CAST(NULLIF(:oldJson, '') AS JSON), CAST(NULLIF(:newJson, '') AS JSON), :userId)
                """)
                .setParameter("entityType", entityType)
                .setParameter("entityId", entityId)
                .setParameter("action", action)
                .setParameter("oldJson", toJson(before))
                .setParameter("newJson", toJson(after))
                .setParameter("userId", userId)
                .executeUpdate();
    }

    private static String toJson(Map<String, ?> map) {
        if (map == null || map.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(entry.getKey())).append("\":").append(jsonValue(entry.getValue()));
        }
        return sb.append('}').toString();
    }

    private static String jsonValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.toPlainString(); // never scientific notation
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        return "\"" + escape(value.toString()) + "\"";
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
