package com.bank.fraud.kafka;

import com.bank.fraud.domain.FraudAlert;
import com.bank.fraud.domain.TransactionEvent;
import com.bank.fraud.policy.PolicyEngine;
import com.bank.fraud.service.ActionService;
import com.bank.fraud.service.CaseService;
import com.bank.fraud.service.MetricsService;
import com.bank.fraud.service.VelocityService;
import com.bank.fraud.service.WhitelistService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TransactionConsumer {

    private final ObjectMapper om = new ObjectMapper();
    private final PolicyEngine policyEngine;
    private final CaseService caseService;
    private final ActionService actionService;
    private final VelocityService velocityService;
    private final WhitelistService whitelistService;
    private final MetricsService metrics;

    public TransactionConsumer(
            PolicyEngine policyEngine,
            CaseService caseService,
            ActionService actionService,
            VelocityService velocityService,
            WhitelistService whitelistService,
            MetricsService metrics
    ) {
        this.policyEngine = policyEngine;
        this.caseService = caseService;
        this.actionService = actionService;
        this.velocityService = velocityService;
        this.whitelistService = whitelistService;
        this.metrics = metrics;
    }

    @KafkaListener(topics = KafkaTopics.TRANSACTIONS_CREATED)
    public void onMessage(ConsumerRecord<String, String> rec, Acknowledgment ack) throws Exception {
        TransactionEvent ev = om.readValue(rec.value(), TransactionEvent.class);

        // metrics: kafka processed
        metrics.incKafkaProcessed();

        // 0) Whitelist FIRST
        WhitelistService.Decision wlDecision = whitelistService.evaluate(ev);
        if (wlDecision.active() && wlDecision.hardBypass()) {
            ack.acknowledge();
            return;
        }
        boolean blockAllowed = !(wlDecision.active() && !wlDecision.hardBypass());

        // 1) Velocity check
        VelocityService.Verdict vel = velocityService.registerAndCheck(ev);
        if (vel.violated()) {
            Map<String, Object> details = baseDetails(ev);
            details.put("velocity", vel.toMap());
            details.put("whitelist", wlDecision.active() ? Map.of(
                    "matchType", wlDecision.matchType(),
                    "matchValue", wlDecision.matchValue(),
                    "reason", wlDecision.reason(),
                    "hardBypass", wlDecision.hardBypass()
            ) : Map.of("active", false));

            String detailsJson = om.writeValueAsString(details);

            FraudAlert alert = caseService.createAlert(
                    ev.eventId(), ev.customerNo(),
                    "VELOCITY", "HIGH",
                    "velocity_rule_v1",
                    detailsJson
            );
            metrics.incAlert("VELOCITY", "HIGH");

            // Ensure case exists/reuse open
            var caze = caseService.openOrReuseOpenCase(
                    ev.customerNo(),
                    "P1",
                    "Velocity rule violated (high frequency / amount burst)",
                    detailsJson,
                    "system"
            );

            // attach alert to case + timeline
            caseService.attachAlertToCase(caze.getId(), alert, "system");

            if (blockAllowed) {
                actionService.blockCredit(ev.customerNo(), "Velocity rule violated", "system");
            }
        }

        // 2) Policy rule evaluation
        List<PolicyEngine.MatchedRule> hits = policyEngine.evaluate(ev);
        if (!hits.isEmpty()) {
            for (PolicyEngine.MatchedRule hit : hits) {

                // Rule-level whitelist
                if (whitelistService.isRuleWhitelisted(hit)) continue;

                Map<String, Object> details = baseDetails(ev);
                details.put("rule", Map.of(
                        "id", hit.id(),
                        "type", hit.type(),
                        "value", hit.value(),
                        "severity", hit.severity(),
                        "action", hit.action()
                ));
                details.put("whitelist", wlDecision.active() ? Map.of(
                        "matchType", wlDecision.matchType(),
                        "matchValue", wlDecision.matchValue(),
                        "reason", wlDecision.reason(),
                        "hardBypass", wlDecision.hardBypass()
                ) : Map.of("active", false));

                String detailsJson = om.writeValueAsString(details);

                String alertType = hit.type().startsWith("BETTING") ? "BETTING_EXPOSURE" : "AML_PATTERN";

                FraudAlert alert = caseService.createAlert(
                        ev.eventId(), ev.customerNo(),
                        alertType, hit.severity(),
                        hit.id(),
                        detailsJson
                );
                metrics.incAlert(alertType, hit.severity());

                // severity -> priority
                String pr = "P3";
                if ("HIGH".equalsIgnoreCase(hit.severity())) pr = "P1";
                else if ("MEDIUM".equalsIgnoreCase(hit.severity())) pr = "P2";

                var caze = caseService.openOrReuseOpenCase(
                        ev.customerNo(),
                        pr,
                        hit.severity() + " severity rule hit: " + hit.id(),
                        detailsJson,
                        "system"
                );

                caseService.attachAlertToCase(caze.getId(), alert, "system");

                if ("BLOCK_CREDIT".equalsIgnoreCase(hit.action()) && blockAllowed) {
                    actionService.blockCredit(ev.customerNo(), "Policy hit: " + hit.id(), "system");
                }
            }
        }

        ack.acknowledge();
    }

    private Map<String, Object> baseDetails(TransactionEvent ev) {
        Map<String, Object> d = new HashMap<>();
        d.put("eventId", ev.eventId());
        d.put("customerNo", ev.customerNo());
        d.put("fromIban", ev.fromIban());
        d.put("toIban", ev.toIban());
        d.put("amountCents", ev.amountCents());
        d.put("currency", ev.currency());
        d.put("channel", ev.channel());
        d.put("mcc", ev.mcc());
        d.put("description", ev.description());
        return d;
    }
}
