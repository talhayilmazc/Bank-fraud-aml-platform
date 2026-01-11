package com.bank.fraud.service;

import com.bank.fraud.domain.TransactionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Map;

@Service
public class VelocityService {

    private final JedisPool pool;
    private final int windowSeconds;
    private final int maxTxCount;
    private final long maxTotalAmountCents;

    public VelocityService(
            JedisPool pool,
            @Value("${app.velocity.windowSeconds}") int windowSeconds,
            @Value("${app.velocity.maxTxCount}") int maxTxCount,
            @Value("${app.velocity.maxTotalAmountCents}") long maxTotalAmountCents
    ) {
        this.pool = pool;
        this.windowSeconds = windowSeconds;
        this.maxTxCount = maxTxCount;
        this.maxTotalAmountCents = maxTotalAmountCents;
    }

    /**
     * Returns velocity verdict:
     * - count in window
     * - sum amount in window
     * - violated boolean
     */
    public Verdict registerAndCheck(TransactionEvent ev) {
        String keyCount = "vel:cnt:" + ev.customerNo();
        String keySum = "vel:sum:" + ev.customerNo();

        try (Jedis j = pool.getResource()) {
            long cnt = j.incr(keyCount);
            if (cnt == 1) j.expire(keyCount, windowSeconds);

            long sum = j.incrBy(keySum, ev.amountCents());
            if (sum == ev.amountCents()) j.expire(keySum, windowSeconds);

            boolean violated = (cnt >= maxTxCount) || (sum >= maxTotalAmountCents);

            return new Verdict(cnt, sum, violated, windowSeconds, maxTxCount, maxTotalAmountCents);
        }
    }

    public record Verdict(
            long countInWindow,
            long sumAmountCentsInWindow,
            boolean violated,
            int windowSeconds,
            int maxTxCount,
            long maxTotalAmountCents
    ) {
        public Map<String, Object> toMap() {
            return Map.of(
                    "countInWindow", countInWindow,
                    "sumAmountCentsInWindow", sumAmountCentsInWindow,
                    "violated", violated,
                    "windowSeconds", windowSeconds,
                    "maxTxCount", maxTxCount,
                    "maxTotalAmountCents", maxTotalAmountCents
            );
        }
    }
}
