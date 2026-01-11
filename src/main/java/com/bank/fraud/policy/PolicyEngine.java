package com.bank.fraud.policy;

import com.bank.fraud.domain.TransactionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.bank.fraud.policy.PolicyModels.*;

@Component
public class PolicyEngine {

    private final ObjectMapper om = new ObjectMapper();
    private Policy policy;

    public PolicyEngine(@Value("${app.policyPath}") String policyPath) {
        try {
            this.policy = om.readValue(new File(policyPath), Policy.class);
        } catch (Exception e) {
            // safe fallback
            this.policy = new Policy();
            this.policy.policyId = "fallback";
            this.policy.rules = List.of();
        }
    }

    public List<MatchedRule> evaluate(TransactionEvent ev) {
        List<MatchedRule> matches = new ArrayList<>();
        if (policy.rules == null) return matches;

        for (Rule r : policy.rules) {
            if (r.type == null) continue;
            boolean hit = switch (r.type) {
                case "BETTING_IBAN_PREFIX" -> ev.toIban() != null && ev.toIban().startsWith(r.value);
                case "BETTING_MCC" -> ev.mcc() != null && ev.mcc().equalsIgnoreCase(r.value);
                case "KEYWORD" -> ev.description() != null && ev.description().toLowerCase().contains(r.value.toLowerCase());
                default -> false;
            };
            if (hit) matches.add(new MatchedRule(r.id, r.type, r.severity, r.action, r.value));
        }
        return matches;
    }

    public record MatchedRule(String id, String type, String severity, String action, String value) {}
}
