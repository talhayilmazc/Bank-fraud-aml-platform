package com.bank.fraud.api;

import com.bank.fraud.domain.SarReport;
import com.bank.fraud.service.SarReportService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/reports")
public class ReportController {

    private final SarReportService svc;

    public ReportController(SarReportService svc) {
        this.svc = svc;
    }

    /**
     * Generate report from a case:
     * POST /v1/reports/generate?caseId=1&type=SAR&actor=talha&narrative=Suspicious%20betting%20pattern
     */
    @PostMapping("/generate")
    public SarReport generate(@RequestParam Long caseId,
                              @RequestParam(defaultValue = "SAR") String type,
                              @RequestParam(defaultValue = "analyst") String actor,
                              @RequestParam(defaultValue = "") String narrative) throws Exception {
        return svc.generate(caseId, type, actor, narrative);
    }

    /**
     * List reports for a case:
     * GET /v1/reports?caseId=1
     */
    @GetMapping
    public List<SarReport> list(@RequestParam Long caseId) {
        return svc.listByCase(caseId);
    }

    /**
     * Fetch report JSON:
     * GET /v1/reports/10/json
     */
    @GetMapping(value="/{id}/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String json(@PathVariable Long id) {
        return svc.get(id).getReportJson();
    }
}
