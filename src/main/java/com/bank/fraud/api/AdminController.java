package com.bank.fraud.api;

import com.bank.fraud.domain.ActionRequest;
import com.bank.fraud.service.ActionRequestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final ActionRequestService actionReq;

    public AdminController(ActionRequestService actionReq) {
        this.actionReq = actionReq;
    }

    /**
     * Admin can also create a request (still requires checker approval)
     */
    @PostMapping("/customers/{customerNo}/request-block-credit")
    public ActionRequest requestBlock(@PathVariable String customerNo,
                                      @RequestParam String reason,
                                      @RequestParam(defaultValue="admin") String maker,
                                      @RequestParam(required = false) Long caseId) {
        return actionReq.request(customerNo, caseId, "BLOCK_CREDIT", maker, reason);
    }

    @PostMapping("/customers/{customerNo}/request-unblock-credit")
    public ActionRequest requestUnblock(@PathVariable String customerNo,
                                        @RequestParam String reason,
                                        @RequestParam(defaultValue="admin") String maker,
                                        @RequestParam(required = false) Long caseId) {
        return actionReq.request(customerNo, caseId, "UNBLOCK_CREDIT", maker, reason);
    }
}
