package com.bank.fraud.service;

import com.bank.fraud.domain.ActionRequest;
import com.bank.fraud.domain.CaseEvent;
import com.bank.fraud.domain.CustomerRiskState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.bank.fraud.service.Repositories.*;

@Service
public class ActionRequestService {

    private final ActionRequestRepo reqRepo;
    private final CaseEventRepo eventRepo;
    private final AuditService audit;
    private final ActionService actionService;
    private final FraudCaseRepo caseRepo;

    public ActionRequestService(ActionRequestRepo reqRepo,
                                CaseEventRepo eventRepo,
                                AuditService audit,
                                ActionService actionService,
                                FraudCaseRepo caseRepo) {
        this.reqRepo = reqRepo;
        this.eventRepo = eventRepo;
        this.audit = audit;
        this.actionService = actionService;
        this.caseRepo = caseRepo;
    }

    /**
     * Maker creates a request (PENDING)
     */
    @Transactional
    public ActionRequest request(String customerNo, Long caseId, String actionType, String maker, String reason) {
        ActionRequest r = new ActionRequest(customerNo, caseId, actionType, maker, reason);
        reqRepo.save(r);

        audit.write(maker, "REQUEST_ACTION", "ACTION_REQUEST", String.valueOf(r.getId()),
                "{\"customerNo\":\""+customerNo+"\",\"actionType\":\""+actionType+"\"}");

        Long effectiveCaseId = resolveCaseId(customerNo, caseId);
        if (effectiveCaseId != null) {
            eventRepo.save(new CaseEvent(
                    effectiveCaseId,
                    "ACTION",
                    "REQUEST_" + actionType,
                    maker,
                    "{\"requestId\":"+r.getId()+",\"reason\":\""+safe(reason)+"\"}"
            ));
        }

        return r;
    }

    /**
     * Checker approves -> system executes -> EXECUTED
     */
    @Transactional
    public CustomerRiskState approveAndExecute(Long requestId, String checker, String note) {
        ActionRequest r = reqRepo.findById(requestId).orElseThrow();

        if (!"PENDING".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Request is not PENDING");
        }

        r.approve(checker, note);
        reqRepo.save(r);

        audit.write(checker, "APPROVE_ACTION", "ACTION_REQUEST", String.valueOf(requestId),
                "{\"note\":\""+safe(note)+"\"}");

        Long effectiveCaseId = resolveCaseId(r.getCustomerNo(), r.getCaseId());
        if (effectiveCaseId != null) {
            eventRepo.save(new CaseEvent(
                    effectiveCaseId,
                    "ACTION",
                    "APPROVE_" + r.getActionType(),
                    checker,
                    "{\"requestId\":"+r.getId()+",\"note\":\""+safe(note)+"\"}"
            ));
        }

        // Execute as "system"
        CustomerRiskState result;
        if ("BLOCK_CREDIT".equalsIgnoreCase(r.getActionType())) {
            result = actionService.blockCredit(r.getCustomerNo(), "Approved: " + safe(r.getReason()), "system");
        } else if ("UNBLOCK_CREDIT".equalsIgnoreCase(r.getActionType())) {
            result = actionService.unblockCredit(r.getCustomerNo(), "system");
        } else {
            throw new IllegalArgumentException("Unknown actionType: " + r.getActionType());
        }

        r.executed();
        reqRepo.save(r);

        if (effectiveCaseId != null) {
            eventRepo.save(new CaseEvent(
                    effectiveCaseId,
                    "ACTION",
                    "EXECUTE_" + r.getActionType(),
                    "system",
                    "{\"requestId\":"+r.getId()+"}"
            ));
        }

        audit.write("system", "EXECUTE_ACTION", "ACTION_REQUEST", String.valueOf(requestId), "{}");
        return result;
    }

    @Transactional
    public ActionRequest reject(Long requestId, String checker, String note) {
        ActionRequest r = reqRepo.findById(requestId).orElseThrow();

        if (!"PENDING".equalsIgnoreCase(r.getStatus())) {
            throw new IllegalStateException("Request is not PENDING");
        }

        r.reject(checker, note);
        reqRepo.save(r);

        audit.write(checker, "REJECT_ACTION", "ACTION_REQUEST", String.valueOf(requestId),
                "{\"note\":\""+safe(note)+"\"}");

        Long effectiveCaseId = resolveCaseId(r.getCustomerNo(), r.getCaseId());
        if (effectiveCaseId != null) {
            eventRepo.save(new CaseEvent(
                    effectiveCaseId,
                    "ACTION",
                    "REJECT_" + r.getActionType(),
                    checker,
                    "{\"requestId\":"+r.getId()+",\"note\":\""+safe(note)+"\"}"
            ));
        }

        return r;
    }

    public java.util.List<ActionRequest> list(String status, String customerNo) {
        if (customerNo != null && !customerNo.isBlank()) {
            return reqRepo.findTop200ByCustomerNoOrderByRequestedAtDesc(customerNo);
        }
        if (status != null && !status.isBlank()) {
            return reqRepo.findTop200ByStatusOrderByRequestedAtDesc(status);
        }
        return reqRepo.findTop200ByStatusOrderByRequestedAtDesc("PENDING");
    }

    private Long resolveCaseId(String customerNo, Long caseId) {
        if (caseId != null) return caseId;
        var open = caseRepo.findTop50ByCustomerNoOrderByOpenedAtDesc(customerNo).stream()
                .filter(c -> "OPEN".equalsIgnoreCase(c.getStatus()) || "INVESTIGATING".equalsIgnoreCase(c.getStatus()))
                .findFirst()
                .orElse(null);
        return open == null ? null : open.getId();
    }

    private String safe(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
