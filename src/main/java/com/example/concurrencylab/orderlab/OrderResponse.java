package com.example.concurrencylab.orderlab;

public record OrderResponse(
        long orderId,
        int acceptedAsyncTasks,
        int rejectedAsyncTasks,
        int callerRunsAsyncTasks,
        String requestId
) {
}
