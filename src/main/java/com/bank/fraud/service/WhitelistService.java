package com.bank.fraud.service;

import com.bank.fraud.domain.TransactionEvent;
import com.bank.fraud.domain.WhitelistEntry;
import com.bank.fraud.policy.PolicyEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static com.bank.fraud.service.Repositories.*;

@Service
public class WhitelistService {

    private final WhitelistRepo repo;
    private final AuditService audit;

    public WhitelistService(WhitelistRepo repo, AuditService audit) {
        this.repo = repo;
        this.audit = audit;
    }

    public Decision evaluate(TransactionEvent ev) {
        // CUSTOMER exception
        var customer = repo.findByTypeAndValue("CUSTOMER", ev.customerNo()).orElse(null);
        if (customer != null && !customer.isExpired()) {
            return Decision.hard("CUSTOMER", customer.getValue(), customer.getReason(), customer.isHardBypass());
        }

        // IBAN exception (destination)
        if (ev.toIban() != null) {
            var iban = repo.findByTypeAndValue("IBAN", ev.toIban()).orElse(null);
            if (iban != null && !iban.isExpired()) {
                return Decision.hard("IBAN", iban.getValue(), iban.getReason(), iban.isHardBypass());
            }
        }

        // MCC exception
        if (ev.mcc() != null) {
            var mcc = repo.findByTypeAndValue("MCC", ev.mcc()).orElse(null);
            if (mcc != null && !mcc.isExpired()) {
                return Decision.hard("MCC", mcc.getValue(), mcc.getReason(), mcc.isHardBypass());
            }
        }

        return Decision.none();
    }

    public boolean isRuleWhitelisted(PolicyEngine.MatchedRule hit) {
        var r = repo.findByTypeAndValue("RULE_ID", hit.id()).orElse(null);
        return r != null && !r.isExpired();
    }

    @Transactional
    public WhitelistEntry upsert(String type, String value, boolean hardBypass, String reason, String createdBy, String ticketRef, Instant expiresAt) {
        var existing = repo.findByTypeAndValue(type, value).orElse(null);
        if (existing != null) {
            // simplest approach: delete and recreate (keeps unique constraint logic simple)
            repo.delete(existing);
        }
        var e = new WhitelistEntry(type, value, hardBypass, reason, createdBy, ticketRef, expiresAt);
        repo.save(e);

        audit.write(createdBy, "UPSERT_WHITELIST", "WHITELIST", type + ":" + value,
                "{\"hardBypass\":" + hardBypass + ",\"ticketRef\":\"" + safe(ticketRef) + "\"}");
        return e;
    }

    @Transactional
    public void delete(Long id, String actor) {
        repo.deleteById(id);
        audit.write(actor, "DELETE_WHITELIST", "WHITELIST", String.valueOf(id), "{}");
    }

    public List<WhitelistEntry> list(String type) {
        if (type == null || type.isBlank()) return repo.findTop200ByOrderByCreatedAtDesc();
        return repo.findTop200ByTypeOrderByCreatedAtDesc(type);
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }

    public record Decision(boolean active, String matchType, String matchValue, String reason, boolean hardBypass) {
        static Decision none() { return new Decision(false, "", "", "", true); }
        static Decision hard(String t, String v, String r, boolean hardBypass) { return new Decision(true, t, v, r, hardBypass); }
    }
}
