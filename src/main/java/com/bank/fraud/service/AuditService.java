package com.bank.fraud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    public void write(String actor, String action, String resource, String resourceId, String detailsJson) {
        log.info("actor={} action={} resource={} resourceId={} details={}", actor, action, resource, resourceId, detailsJson);
    }
}
