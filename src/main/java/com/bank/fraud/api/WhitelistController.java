package com.bank.fraud.api;

import com.bank.fraud.domain.WhitelistEntry;
import com.bank.fraud.service.WhitelistService;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/admin/whitelist")
public class WhitelistController {

    private final WhitelistService wl;

    public WhitelistController(WhitelistService wl) {
        this.wl = wl;
    }

    @GetMapping
    public List<WhitelistEntry> list(@RequestParam(required = false) String type) {
        return wl.list(type);
    }

    /**
     * Example:
     * POST /v1/admin/whitelist/upsert?type=CUSTOMER&value=CUST00042&hardBypass=true&reason=VIP&createdBy=talha&ticketRef=INC-123&expiresAt=2026-02-01T00:00:00Z
     */
    @PostMapping("/upsert")
    public WhitelistEntry upsert(
            @RequestParam String type,
            @RequestParam String value,
            @RequestParam(defaultValue = "true") boolean hardBypass,
            @RequestParam String reason,
            @RequestParam(defaultValue = "admin") String createdBy,
            @RequestParam(required = false) String ticketRef,
            @RequestParam(required = false) String expiresAt
    ) {
        Instant exp = null;
        if (expiresAt != null && !expiresAt.isBlank()) exp = Instant.parse(expiresAt);
        return wl.upsert(type, value, hardBypass, reason, createdBy, ticketRef, exp);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id, @RequestParam(defaultValue = "admin") String actor) {
        wl.delete(id, actor);
    }
}
