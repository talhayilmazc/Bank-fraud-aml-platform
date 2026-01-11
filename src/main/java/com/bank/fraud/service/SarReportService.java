package com.bank.fraud.service;

import com.bank.fraud.domain.CaseEvent;
import com.bank.fraud.domain.FraudCase;
import com.bank.fraud.domain.SarReport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

import static com.bank.fraud.service.Repositories.*;

@Service
public class SarReportService {

    private final ObjectMapper om = new ObjectMapper();
    private final FraudCaseRepo caseRepo;
    private final FraudAlertRepo alertRepo;
    private final CaseEventRepo eventRepo;
    private final SarReportRepo reportRepo;
    private final AuditService audit;

    public SarReportService(FraudCaseRepo caseRepo,
                            FraudAlertRepo alertRepo,
                            CaseEventRepo eventRepo,
                            SarReportRepo reportRepo,
                            AuditService audit) {
        this.caseRepo = caseRepo;
        this.alertRepo = alertRepo;
        this.eventRepo = eventRepo;
        this.reportRepo = reportRepo;
        this.audit = audit;
    }

    @Transactional
    public SarReport generate(Long caseId, String reportType, String actor, String narrative) throws Exception {
        FraudCase caze = caseRepo.findById(caseId).orElseThrow();

        int nextVersion = reportRepo.findTop1ByCaseIdOrderByVersionDesc(caseId)
                .map(r -> r.getVersion() + 1)
                .orElse(1);

        // Gather evidence
        var alerts = alertRepo.findTop200ByCaseIdOrderByCreatedAtAsc(caseId);
        var timeline = eventRepo.findTop500ByCaseIdOrderByCreatedAtAsc(caseId);

        Map<String, Object> header = new LinkedHashMap<>();
        header.put("reportType", (reportType == null || reportType.isBlank()) ? "SAR" : reportType.toUpperCase());
        header.put("version", nextVersion);
        header.put("generatedAt", Instant.now().toString());
        header.put("generatedBy", actor);

        Map<String, Object> subject = new LinkedHashMap<>();
        subject.put("customerNo", caze.getCustomerNo());
        subject.put("caseId", caze.getId());
        subject.put("caseStatus", caze.getStatus());
        subject.put("priority", caze.getPriority());
        subject.put("openedAt", caze.getOpenedAt().toString());

        // Summarize risk indicators from alerts
        List<Map<String, Object>> indicators = new ArrayList<>();
        for (var a : alerts) {
            indicators.add(Map.of(
                    "alertId", a.getId(),
                    "createdAt", a.getCreatedAt().toString(),
                    "type", a.getAlertType(),
                    "severity", a.getSeverity(),
                    "ruleId", a.getRuleId()
            ));
        }

        // Include timeline entries (compact)
        List<Map<String, Object>> events = new ArrayList<>();
        for (CaseEvent e : timeline) {
            events.add(Map.of(
                    "time", e.getCreatedAt().toString(),
                    "type", e.getEventType(),
                    "code", e.getEventCode(),
                    "actor", e.getActor()
            ));
        }

        Map<String, Object> assessment = new LinkedHashMap<>();
        assessment.put("narrative", narrative == null ? "" : narrative);
        assessment.put("recommendation", "Escalate to Compliance review; confirm customer intent; validate counterparties; consider account restrictions if risk persists.");

        // Final report object (bank-style)
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("header", header);
        report.put("subject", subject);
        report.put("riskIndicators", indicators);
        report.put("caseTimeline", events);
        report.put("evidenceReferences", Map.of(
                "alertsCount", alerts.size(),
                "timelineEventsCount", timeline.size()
        ));
        report.put("assessment", assessment);

        String reportJson = om.writerWithDefaultPrettyPrinter().writeValueAsString(report);

        SarReport saved = new SarReport(caseId, header.get("reportType").toString(), nextVersion, actor, reportJson);
        reportRepo.save(saved);

        // Timeline + audit
        eventRepo.save(new CaseEvent(
                caseId,
                "SYSTEM",
                "REPORT_GENERATED",
                actor,
                "{\"reportId\":"+saved.getId()+",\"reportType\":\""+saved.getReportType()+"\",\"version\":"+saved.getVersion()+"}"
        ));
        audit.write(actor, "GENERATE_REPORT", "SAR_REPORT", String.valueOf(saved.getId()),
                "{\"caseId\":"+caseId+",\"type\":\""+saved.getReportType()+"\",\"version\":"+saved.getVersion()+"\"}");

        return saved;
    }

    public List<SarReport> listByCase(Long caseId) {
        return reportRepo.findTop50ByCaseIdOrderByVersionDesc(caseId);
    }

    public SarReport get(Long reportId) {
        return reportRepo.findById(reportId).orElseThrow();
    }
}
