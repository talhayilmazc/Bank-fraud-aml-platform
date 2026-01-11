package com.bank.fraud.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="fraud_alerts")
public class FraudAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false) private String eventId;
    @Column(nullable=false) private String customerNo;
    @Column(nullable=false) private String alertType; // BETTING_EXPOSURE, VELOCITY, AML_PATTERN
    @Column(nullable=false) private String severity;  // LOW, MEDIUM, HIGH
    @Column(nullable=false) private String ruleId;

    @Column(nullable=false) private Instant createdAt = Instant.now();

    @Column(columnDefinition="text") private String detailsJson;

    @Column private Long caseId; // nullable: not always mapped to a case

    @Column private Long caseId; // nullable: not always mapped to a case



    public FraudAlert() {}

    public FraudAlert(String eventId, String customerNo, String alertType, String severity, String ruleId, String detailsJson) {
        this.eventId = eventId;
        this.customerNo = customerNo;
        this.alertType = alertType;
        this.severity = severity;
        this.ruleId = ruleId;
        this.detailsJson = detailsJson;
    }

    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public String getEventId() { return eventId; }
    public String getCustomerNo() { return customerNo; }
    public String getAlertType() { return alertType; }
    public String getSeverity() { return severity; }
    public String getRuleId() { return ruleId; }
    public Instant getCreatedAt() { return createdAt; }
    public String getDetailsJson() { return detailsJson; }
    public void setCaseId(Long caseId) { this.caseId = caseId; }

}
