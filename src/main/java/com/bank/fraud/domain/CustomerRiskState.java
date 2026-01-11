package com.bank.fraud.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="customer_risk_state", indexes = {
        @Index(name="ix_risk_customer", columnList="customerNo", unique = true)
})
public class CustomerRiskState {
    @Id
    @Column(nullable=false)
    private String customerNo;

    @Column(nullable=false) private boolean creditBlocked = false;
    @Column(nullable=false) private String blockReason = "";
    @Column(nullable=false) private Instant updatedAt = Instant.now();

    public CustomerRiskState() {}

    public CustomerRiskState(String customerNo) {
        this.customerNo = customerNo;
    }

    public String getCustomerNo() { return customerNo; }
    public boolean isCreditBlocked() { return creditBlocked; }
    public String getBlockReason() { return blockReason; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void blockCredit(String reason) {
        this.creditBlocked = true;
        this.blockReason = reason;
        this.updatedAt = Instant.now();
    }

    public void unblockCredit() {
        this.creditBlocked = false;
        this.blockReason = "";
        this.updatedAt = Instant.now();
    }
}
