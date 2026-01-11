package com.bank.fraud.api;

import com.bank.fraud.domain.CaseEvent;
import com.bank.fraud.domain.FraudCase;
import com.bank.fraud.service.CaseService;
import com.bank.fraud.service.Repositories;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/cases")
public class CaseController {

    private final Repositories.FraudCaseRepo caseRepo;
    private final CaseService caseService;

    public CaseController(Repositories.FraudCaseRepo caseRepo, CaseService caseService) {
        this.caseRepo = caseRepo;
        this.caseService = caseService;
    }

    @GetMapping
    public List<FraudCase> list(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String customerNo) {
        if (customerNo != null) return caseRepo.findTop50ByCustomerNoOrderByOpenedAtDesc(customerNo);
        if (status != null) return caseRepo.findTop50ByStatusOrderByOpenedAtDesc(status);
        return caseRepo.findTop50ByStatusOrderByOpenedAtDesc("OPEN");
    }

    @PostMapping("/{caseId}/status")
    public FraudCase setStatus(@PathVariable Long caseId, @RequestParam String status, @RequestParam(defaultValue = "analyst") String actor) {
        return caseService.updateStatus(caseId, status, actor);
    }

    @GetMapping("/{caseId}/timeline")
    public List<CaseEvent> timeline(@PathVariable Long caseId) {
        return caseService.timeline(caseId);
    }

    @PostMapping("/{caseId}/notes")
    public CaseEvent addNote(@PathVariable Long caseId, @RequestParam String note, @RequestParam(defaultValue = "analyst") String actor) {
        return caseService.addNote(caseId, note, actor);
    }
}
