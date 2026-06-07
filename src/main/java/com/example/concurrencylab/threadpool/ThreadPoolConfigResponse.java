package com.example.concurrencylab.threadpool;

public record ThreadPoolConfigResponse(
        int corePoolSize,
        int maximumPoolSize,
        int queueCapacity,
        int currentQueueSize,
        long keepAliveSeconds,
        boolean allowCoreThreadTimeOut,
        RejectionPolicy rejectionPolicy
) {
}
