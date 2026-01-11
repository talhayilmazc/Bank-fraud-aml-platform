package com.bank.fraud.api;

import com.bank.fraud.domain.ActionRequest;
import com.bank.fraud.domain.CustomerRiskState;
import com.bank.fraud.service.ActionRequestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/actions")
public class ActionRequestController {

    private final ActionRequestService svc;

    public ActionRequestController(ActionRequestService svc) {
        this.svc = svc;
    }

    /**
     * Maker creates request:
     * POST /v1/actions/request?customerNo=CUST00042&actionType=BLOCK_CREDIT&reason=Betting%20suspected&maker=talha&caseId=1
     */
    @PostMapping("/request")
    public ActionRequest request(
            @RequestParam String customerNo,
            @RequestParam String actionType, // BLOCK_CREDIT | UNBLOCK_CREDIT
            @RequestParam String reason,
            @RequestParam(defaultValue = "analyst") String maker,
            @RequestParam(required = false) Long caseId
    ) {
        return svc.request(customerNo, caseId, actionType, maker, reason);
    }

    /**
     * Checker approves and system executes:
     * POST /v1/actions/{id}/approve?checker=supervisor&note=Approved
     */
    @PostMapping("/{id}/approve")
    public CustomerRiskState approve(
            @PathVariable Long id,
            @RequestParam(defaultValue = "checker") String checker,
            @RequestParam(defaultValue = "") String note
    ) {
        return svc.approveAndExecute(id, checker, note);
    }

    /**
     * Checker rejects:
     * POST /v1/actions/{id}/reject?checker=supervisor&note=Insufficient%20evidence
     */
    @PostMapping("/{id}/reject")
    public ActionRequest reject(
            @PathVariable Long id,
            @RequestParam(defaultValue = "checker") String checker,
            @RequestParam(defaultValue = "") String note
    ) {
        return svc.reject(id, checker, note);
    }

    /**
     * List action requests:
     * GET /v1/actions?status=PENDING
     * GET /v1/actions?customerNo=CUST00042
     */
    @GetMapping
    public List<ActionRequest> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String customerNo
    ) {
        return svc.list(status, customerNo);
    }
}
