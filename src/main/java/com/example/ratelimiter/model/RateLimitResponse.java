package com.example.ratelimiter.model;

public class RateLimitResponse {
    private int status;
    private String message;
    private String clientId;
    private long remainingTokens;

    public RateLimitResponse() {
    }

    public RateLimitResponse(int status, String message, String clientId, long remainingTokens) {
        this.status = status;
        this.message = message;
        this.clientId = clientId;
        this.remainingTokens = remainingTokens;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public long getRemainingTokens() {
        return remainingTokens;
    }

    public void setRemainingTokens(long remainingTokens) {
        this.remainingTokens = remainingTokens;
    }
}
