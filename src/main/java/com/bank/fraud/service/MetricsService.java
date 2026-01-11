package com.bank.fraud.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class MetricsService {

    private final MeterRegistry registry;

    public MetricsService(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incKafkaProcessed() {
        Counter.builder("kafka_events_processed_total")
                .description("Total kafka transaction events processed")
                .register(registry)
                .increment();
    }

    public void incAlert(String type, String severity) {
        Counter.builder("fraud_alerts_total")
                .description("Total alerts created")
                .tag("type", safe(type))
                .tag("severity", safe(severity))
                .register(registry)
                .increment();
    }

    public void incCaseOpened(String priority) {
        Counter.builder("fraud_cases_opened_total")
                .description("Total cases opened")
                .tag("priority", safe(priority))
                .register(registry)
                .increment();
    }

    public void incBlock(String reason) {
        Counter.builder("credit_blocks_total")
                .description("Total credit blocks executed")
                .tag("reason", safe(reason))
                .register(registry)
                .increment();
    }

    private String safe(String s) {
        if (s == null || s.isBlank()) return "unknown";
        return s.replace(" ", "_").toLowerCase();
    }
}
