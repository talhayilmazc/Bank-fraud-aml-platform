package com.bank.fraud.service;

import com.bank.fraud.domain.CustomerRiskState;
import com.bank.fraud.domain.FraudAlert;
import com.bank.fraud.domain.FraudCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public class Repositories {
    public interface FraudAlertRepo extends JpaRepository<FraudAlert, Long> {
        List<FraudAlert> findTop50ByCustomerNoOrderByCreatedAtDesc(String customerNo);
        List<FraudAlert> findTop200ByCaseIdOrderByCreatedAtAsc(Long caseId);

    }

    public interface FraudCaseRepo extends JpaRepository<FraudCase, Long> {
        List<FraudCase> findTop50ByCustomerNoOrderByOpenedAtDesc(String customerNo);
        List<FraudCase> findTop50ByStatusOrderByOpenedAtDesc(String status);
    }

    public interface CustomerRiskRepo extends JpaRepository<CustomerRiskState, String> {
        Optional<CustomerRiskState> findByCustomerNo(String customerNo);
    }
}

    public interface WhitelistRepo extends JpaRepository<com.bank.fraud.domain.WhitelistEntry, Long> {
        java.util.Optional<com.bank.fraud.domain.WhitelistEntry> findByTypeAndValue(String type, String value);
        java.util.List<com.bank.fraud.domain.WhitelistEntry> findTop200ByTypeOrderByCreatedAtDesc(String type);
        java.util.List<com.bank.fraud.domain.WhitelistEntry> findTop200ByOrderByCreatedAtDesc();
    }
        public interface CaseEventRepo extends JpaRepository<com.bank.fraud.domain.CaseEvent, Long> {
        java.util.List<com.bank.fraud.domain.CaseEvent> findTop500ByCaseIdOrderByCreatedAtAsc(Long caseId);
    }
        public interface ActionRequestRepo extends JpaRepository<com.bank.fraud.domain.ActionRequest, Long> {
        java.util.List<com.bank.fraud.domain.ActionRequest> findTop200ByStatusOrderByRequestedAtDesc(String status);
        java.util.List<com.bank.fraud.domain.ActionRequest> findTop200ByCustomerNoOrderByRequestedAtDesc(String customerNo);
    }

    public interface SarReportRepo extends JpaRepository<com.bank.fraud.domain.SarReport, Long> {
        java.util.List<com.bank.fraud.domain.SarReport> findTop50ByCaseIdOrderByVersionDesc(Long caseId);
        java.util.Optional<com.bank.fraud.domain.SarReport> findTop1ByCaseIdOrderByVersionDesc(Long caseId);
    }



