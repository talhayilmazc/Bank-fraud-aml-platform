package com.bank.fraud.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="case_events", indexes = {
        @Index(name="ix_case_events_case", columnList="caseId"),
        @Index(name="ix_case_events_time", columnList="createdAt")
})
public class CaseEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long caseId;

    /**
     * ALERT | ACTION | STATUS | NOTE | SYSTEM
     */
    @Column(nullable=false, length=16)
    private String eventType;

    /**
     * short identifier: e.g. "velocity_rule_v1", "BLOCK_CREDIT", "STATUS_CHANGE"
     */
    @Column(nullable=false, length=64)
    private String eventCode;

    /**
     * actor: "system", "analyst", "admin", etc.
     */
    @Column(nullable=false, length=64)
    private String actor;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @Column(columnDefinition="text")
    private String payloadJson;

    public CaseEvent() {}

    public CaseEvent(Long caseId, String eventType, String eventCode, String actor, String payloadJson) {
        this.caseId = caseId;
        this.eventType = eventType;
        this.eventCode = eventCode;
        this.actor = actor;
        this.payloadJson = payloadJson;
    }

    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public String getEventType() { return eventType; }
    public String getEventCode() { return eventCode; }
    public String getActor() { return actor; }
    public Instant getCreatedAt() { return createdAt; }
    public String getPayloadJson() { return payloadJson; }
}
