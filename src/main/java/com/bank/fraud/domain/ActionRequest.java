package com.bank.fraud.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="action_requests", indexes = {
        @Index(name="ix_ar_customer", columnList="customerNo"),
        @Index(name="ix_ar_status", columnList="status"),
        @Index(name="ix_ar_case", columnList="caseId")
})
public class ActionRequest {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false) private String customerNo;

    // optional: which case this action relates to
    private Long caseId;

    /**
     * BLOCK_CREDIT | UNBLOCK_CREDIT
     */
    @Column(nullable=false, length=32)
    private String actionType;

    @Column(nullable=false, length=16)
    private String status; // PENDING, APPROVED, REJECTED, EXECUTED

    @Column(nullable=false, length=64)
    private String requestedBy; // maker

    @Column(length=64)
    private String reviewedBy;  // checker

    @Column(nullable=false)
    private Instant requestedAt = Instant.now();

    private Instant reviewedAt;
    private Instant executedAt;

    @Column(columnDefinition="text")
    private String reason;

    @Column(columnDefinition="text")
    private String reviewNote;

    public ActionRequest() {}

    public ActionRequest(String customerNo, Long caseId, String actionType, String requestedBy, String reason) {
        this.customerNo = customerNo;
        this.caseId = caseId;
        this.actionType = actionType;
        this.requestedBy = requestedBy;
        this.reason = reason;
        this.status = "PENDING";
    }

    public Long getId() { return id; }
    public String getCustomerNo() { return customerNo; }
    public Long getCaseId() { return caseId; }
    public String getActionType() { return actionType; }
    public String getStatus() { return status; }
    public String getRequestedBy() { return requestedBy; }
    public String getReviewedBy() { return reviewedBy; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getReviewedAt() { return reviewedAt; }
    public Instant getExecutedAt() { return executedAt; }
    public String getReason() { return reason; }
    public String getReviewNote() { return reviewNote; }

    public void approve(String checker, String note) {
        this.status = "APPROVED";
        this.reviewedBy = checker;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public void reject(String checker, String note) {
        this.status = "REJECTED";
        this.reviewedBy = checker;
        this.reviewNote = note;
        this.reviewedAt = Instant.now();
    }

    public void executed() {
        this.status = "EXECUTED";
        this.executedAt = Instant.now();
    }
}
