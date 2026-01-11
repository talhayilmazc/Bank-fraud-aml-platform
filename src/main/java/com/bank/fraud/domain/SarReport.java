package com.bank.fraud.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name="sar_reports", indexes = {
        @Index(name="ix_sar_case", columnList="caseId"),
        @Index(name="ix_sar_created", columnList="createdAt")
})
public class SarReport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long caseId;

    @Column(nullable=false, length=32)
    private String reportType; // SAR or STR

    @Column(nullable=false)
    private int version; // 1..n

    @Column(nullable=false, length=64)
    private String createdBy;

    @Column(nullable=false)
    private Instant createdAt = Instant.now();

    @Column(columnDefinition="text", nullable=false)
    private String reportJson;

    public SarReport() {}

    public SarReport(Long caseId, String reportType, int version, String createdBy, String reportJson) {
        this.caseId = caseId;
        this.reportType = reportType;
        this.version = version;
        this.createdBy = createdBy;
        this.reportJson = reportJson;
    }

    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public String getReportType() { return reportType; }
    public int getVersion() { return version; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public String getReportJson() { return reportJson; }
}
