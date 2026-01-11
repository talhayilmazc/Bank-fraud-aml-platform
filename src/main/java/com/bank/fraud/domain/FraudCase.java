package com.bank.fraud.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="fraud_cases", indexes = {
        @Index(name="ix_case_customer", columnList="customerNo"),
        @Index(name="ix_case_status", columnList="status")
})
public class FraudCase {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false) private String customerNo;
    @Column(nullable=false) private String status; // OPEN, INVESTIGATING, ACTIONED, CLOSED
    @Column(nullable=false) private String priority; // P1,P2,P3
    @Column(nullable=false) private Instant openedAt = Instant.now();

    @Column(columnDefinition="text") private String summary;
    @Column(columnDefinition="text") private String evidenceJson;

    public FraudCase() {}

    public FraudCase(String customerNo, String status, String priority, String summary, String evidenceJson) {
        this.customerNo = customerNo;
        this.status = status;
        this.priority = priority;
        this.summary = summary;
        this.evidenceJson = evidenceJson;
    }

    public Long getId() { return id; }
    public String getCustomerNo() { return customerNo; }
    public String getStatus() { return status; }
    public String getPriority() { return priority; }
    public Instant getOpenedAt() { return openedAt; }
    public String getSummary() { return summary; }
    public String getEvidenceJson() { return evidenceJson; }

    public void setStatus(String status) { this.status = status; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
}
