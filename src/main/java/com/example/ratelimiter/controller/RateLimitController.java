package com.example.ratelimiter.controller;

import com.example.ratelimiter.model.RateLimitResponse;
import com.example.ratelimiter.service.RateLimiterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RateLimitController {

    private final RateLimiterService rateLimiterService;

    public RateLimitController(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @GetMapping("/test")
    public ResponseEntity<RateLimitResponse> testRateLimit(@RequestParam(name = "clientId", required = false) String clientId) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId parameter is required");
        }

        RateLimiterService.RateLimitResult result = rateLimiterService.allowRequest(clientId);

        if (result.isAllowed()) {
            RateLimitResponse response = new RateLimitResponse(
                    HttpStatus.OK.value(),
                    "Request allowed",
                    clientId,
                    result.getRemainingTokens()
            );
            return ResponseEntity.ok(response);
        } else {
            RateLimitResponse response = new RateLimitResponse(
                    HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Rate limit exceeded",
                    clientId,
                    result.getRemainingTokens()
            );
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
    }
}
