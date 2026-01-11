package com.bank.fraud.service;

import com.bank.fraud.domain.CaseEvent;
import com.bank.fraud.domain.FraudAlert;
import com.bank.fraud.domain.FraudCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.bank.fraud.service.Repositories.*;

@Service
public class CaseService {

    private final FraudAlertRepo alertRepo;
    private final FraudCaseRepo caseRepo;
    private final CaseEventRepo eventRepo;
    private final MetricsService metrics;
    private final AuditService audit;

    public CaseService(FraudAlertRepo alertRepo,
                       FraudCaseRepo caseRepo,
                       CaseEventRepo eventRepo,
                       MetricsService metrics,
                       AuditService audit) {
        this.alertRepo = alertRepo;
        this.caseRepo = caseRepo;
        this.eventRepo = eventRepo;
        this.metrics = metrics;
        this.audit = audit;
    }

    @Transactional
    public FraudAlert createAlert(String eventId, String customerNo, String type, String severity, String ruleId, String detailsJson) {
        FraudAlert a = new FraudAlert(eventId, customerNo, type, severity, ruleId, detailsJson);
        alertRepo.save(a);
        return a;
    }

    /**
     * Ensure there is an OPEN/INVESTIGATING case. Reuse if possible, else create.
     */
    @Transactional
    public FraudCase openOrReuseOpenCase(String customerNo, String priority, String summary, String evidenceJson, String actor) {
        Optional<FraudCase> existingOpen = caseRepo.findTop50ByCustomerNoOrderByOpenedAtDesc(customerNo)
                .stream()
                .filter(c -> "OPEN".equalsIgnoreCase(c.getStatus()) || "INVESTIGATING".equalsIgnoreCase(c.getStatus()))
                .findFirst();

        FraudCase c;
        if (existingOpen.isPresent()) {
            c = existingOpen.get();
        } else {
            c = new FraudCase(customerNo, "OPEN", priority, summary, evidenceJson);
            caseRepo.save(c);

            // metrics: case opened
            metrics.incCaseOpened(priority);

            audit.write(actor, "OPEN_CASE", "CASE", String.valueOf(c.getId()), "{\"customerNo\":\""+customerNo+"\"}");
            eventRepo.save(new CaseEvent(
                    c.getId(),
                    "STATUS",
                    "CASE_OPENED",
                    actor,
                    "{\"priority\":\""+priority+"\",\"summary\":\""+safe(summary)+"\"}"
            ));
        }
        return c;
    }

    @Transactional
    public FraudAlert attachAlertToCase(Long caseId, FraudAlert alert, String actor) {
        alert.setCaseId(caseId);
        alertRepo.save(alert);

        eventRepo.save(new CaseEvent(
                caseId,
                "ALERT",
                alert.getRuleId(),
                actor,
                alert.getDetailsJson()
        ));
        return alert;
    }

    @Transactional
    public FraudCase updateStatus(Long caseId, String status, String actor) {
        FraudCase c = caseRepo.findById(caseId).orElseThrow();
        String old = c.getStatus();
        c.setStatus(status);
        caseRepo.save(c);

        audit.write(actor, "UPDATE_CASE_STATUS", "CASE", String.valueOf(caseId), "{\"status\":\""+status+"\"}");

        eventRepo.save(new CaseEvent(
                caseId,
                "STATUS",
                "STATUS_CHANGE",
                actor,
                "{\"from\":\""+safe(old)+"\",\"to\":\""+safe(status)+"\"}"
        ));
        return c;
    }

    @Transactional
    public CaseEvent addNote(Long caseId, String note, String actor) {
        CaseEvent ev = new CaseEvent(caseId, "NOTE", "ANALYST_NOTE", actor, "{\"note\":\""+safe(note)+"\"}");
        eventRepo.save(ev);
        audit.write(actor, "ADD_CASE_NOTE", "CASE", String.valueOf(caseId), "{\"note\":\""+safe(note)+"\"}");
        return ev;
    }

    public java.util.List<CaseEvent> timeline(Long caseId) {
        return eventRepo.findTop500ByCaseIdOrderByCreatedAtAsc(caseId);
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
