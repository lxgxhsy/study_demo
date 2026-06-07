package com.example.concurrencylab.support;

import java.time.Instant;

public record ApiErrorResponse(
        ErrorCode code,
        String message,
        Instant timestamp
) {
    public static ApiErrorResponse of(ErrorCode code, String message) {
        return new ApiErrorResponse(code, message, Instant.now());
    }
}
