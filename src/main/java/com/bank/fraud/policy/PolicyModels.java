package com.bank.fraud.policy;

import java.util.List;

public class PolicyModels {
    public static class Policy {
        public String policyId;
        public List<Rule> rules;
    }

    public static class Rule {
        public String id;
        public String type;       // BETTING_IBAN_PREFIX, BETTING_MCC, KEYWORD
        public String severity;   // LOW, MEDIUM, HIGH
        public String action;     // ALERT_ONLY, BLOCK_CREDIT
        public String value;      // prefix or mcc or keyword
    }
}
