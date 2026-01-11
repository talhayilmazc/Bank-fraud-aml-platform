package com.bank.fraud.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "whitelist_entries", indexes = {
        @Index(name="ix_wl_type_value", columnList="type,value", unique = true),
        @Index(name="ix_wl_expires", columnList="expiresAt")
})
public class WhitelistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * CUSTOMER | IBAN | RULE_ID | MCC
     */
    @Column(nullable=false, length=32)
    private String type;

    /**
     * CUSTOMER: customerNo
     * IBAN: iban value (exact)
     * RULE_ID: rule id (e.g., betting_mcc_7995)
     * MCC: mcc code (e.g., 7995)
     */
    @Column(nullable=false, length=128)
    private String value;

    /**
     * Optional: true means "hard bypass" (skip alert/case/action)
     * false means "soft bypass" (only block is skipped, alerts remain)
     */
    @Column(nullable=false)
    private boolean hardBypass = true;

    @Column(nullable=false, length=256)
    private String reason;

    @Column(nullable=false, length=64)
    private String createdBy;

    @Column(length=64)
    private String ticketRef;

    private Instant expiresAt; // nullable => no expiry

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    public WhitelistEntry() {}

    public WhitelistEntry(String type, String value, boolean hardBypass, String reason, String createdBy, String ticketRef, Instant expiresAt) {
        this.type = type;
        this.value = value;
        this.hardBypass = hardBypass;
        this.reason = reason;
        this.createdBy = createdBy;
        this.ticketRef = ticketRef;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getType() { return type; }
    public String getValue() { return value; }
    public boolean isHardBypass() { return hardBypass; }
    public String getReason() { return reason; }
    public String getCreatedBy() { return createdBy; }
    public String getTicketRef() { return ticketRef; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }
}
