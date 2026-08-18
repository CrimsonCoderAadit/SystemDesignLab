package com.example.ratelimiter.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Service
public class RateLimiterService {

    private final RedisTemplate<String, String> redisTemplate;
    private final RedisScript<List> tokenBucketScript;

    @Value("${rate-limiter.bucket-capacity}")
    private int bucketCapacity;

    @Value("${rate-limiter.refill-rate}")
    private int refillRate;

    public RateLimiterService(RedisTemplate<String, String> redisTemplate, RedisScript<List> tokenBucketScript) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
    }

    /**
     * Checks if a request for the given clientId is allowed.
     * @param clientId The client identifier
     * @return RateLimitResult containing allowed status and remaining tokens
     */
    public RateLimitResult allowRequest(String clientId) {
        String key = "rate_limit:" + clientId;
        long now = Instant.now().getEpochSecond();

        List<Long> result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(key),
                String.valueOf(bucketCapacity),
                String.valueOf(refillRate),
                String.valueOf(now)
        );

        if (result == null || result.size() != 2) {
            throw new RuntimeException("Unexpected result from Redis Lua script");
        }

        boolean allowed = result.get(0) == 1L;
        long remainingTokens = result.get(1);

        return new RateLimitResult(allowed, remainingTokens);
    }

    public static class RateLimitResult {
        private final boolean allowed;
        private final long remainingTokens;

        public RateLimitResult(boolean allowed, long remainingTokens) {
            this.allowed = allowed;
            this.remainingTokens = remainingTokens;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRemainingTokens() {
            return remainingTokens;
        }
    }
}
