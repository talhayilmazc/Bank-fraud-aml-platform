package com.bank.fraud.service;

import com.bank.fraud.domain.CaseEvent;
import com.bank.fraud.domain.CustomerRiskState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.bank.fraud.service.Repositories.*;

@Service
public class ActionService {

    private final CustomerRiskRepo riskRepo;
    private final FraudCaseRepo caseRepo;
    private final CaseEventRepo eventRepo;
    private final MetricsService metrics;
    private final AuditService audit;

    public ActionService(CustomerRiskRepo riskRepo,
                         FraudCaseRepo caseRepo,
                         CaseEventRepo eventRepo,
                         MetricsService metrics,
                         AuditService audit) {
        this.riskRepo = riskRepo;
        this.caseRepo = caseRepo;
        this.eventRepo = eventRepo;
        this.metrics = metrics;
        this.audit = audit;
    }

    @Transactional
    public CustomerRiskState blockCredit(String customerNo, String reason, String actor) {
        CustomerRiskState st = riskRepo.findById(customerNo).orElseGet(() -> new CustomerRiskState(customerNo));
        st.blockCredit(reason);
        riskRepo.save(st);

        // metrics: blocks
        metrics.incBlock(reason);

        audit.write(actor, "BLOCK_CREDIT", "CUSTOMER", customerNo, "{\"reason\":\""+escape(reason)+"\"}");

        // attach to latest OPEN/INVESTIGATING case (if any)
        var open = caseRepo.findTop50ByCustomerNoOrderByOpenedAtDesc(customerNo).stream()
                .filter(c -> "OPEN".equalsIgnoreCase(c.getStatus()) || "INVESTIGATING".equalsIgnoreCase(c.getStatus()))
                .findFirst()
                .orElse(null);

        if (open != null) {
            eventRepo.save(new CaseEvent(
                    open.getId(),
                    "ACTION",
                    "BLOCK_CREDIT",
                    actor,
                    "{\"reason\":\""+escape(reason)+"\"}"
            ));
        }

        return st;
    }

    @Transactional
    public CustomerRiskState unblockCredit(String customerNo, String actor) {
        CustomerRiskState st = riskRepo.findById(customerNo).orElseGet(() -> new CustomerRiskState(customerNo));
        st.unblockCredit();
        riskRepo.save(st);

        audit.write(actor, "UNBLOCK_CREDIT", "CUSTOMER", customerNo, "{}");

        var open = caseRepo.findTop50ByCustomerNoOrderByOpenedAtDesc(customerNo).stream()
                .filter(c -> "OPEN".equalsIgnoreCase(c.getStatus()) || "INVESTIGATING".equalsIgnoreCase(c.getStatus()))
                .findFirst()
                .orElse(null);

        if (open != null) {
            eventRepo.save(new CaseEvent(
                    open.getId(),
                    "ACTION",
                    "UNBLOCK_CREDIT",
                    actor,
                    "{}"
            ));
        }

        return st;
    }

    private String escape(String s){ return s == null ? "" : s.replace("\"","'"); }
}
