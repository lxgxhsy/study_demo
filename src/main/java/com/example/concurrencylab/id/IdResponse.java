package com.example.concurrencylab.id;

public record IdResponse(
        String bizTag,
        long id,
        String requestId
) {
}
