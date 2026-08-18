package com.example.ratelimiter.exception;

import com.example.ratelimiter.model.RateLimitResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<RateLimitResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        RateLimitResponse response = new RateLimitResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                null,
                0
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RateLimitResponse> handleGenericException(Exception ex) {
        ex.printStackTrace();
        RateLimitResponse response = new RateLimitResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error: " + ex.getMessage(),
                null,
                0
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
