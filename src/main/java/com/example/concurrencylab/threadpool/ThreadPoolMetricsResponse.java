package com.example.concurrencylab.threadpool;

import java.time.Instant;

public record ThreadPoolMetricsResponse(
        int activeThreadCount,
        int poolSize,
        int largestPoolSize,
        int queueSize,
        int queueCapacity,
        long taskCount,
        long completedTaskCount,
        long submittedTaskCount,
        long rejectedTaskCount,
        RejectionPolicy rejectionPolicy,
        long keepAliveSeconds,
        boolean allowCoreThreadTimeOut,
        double waitTimeMsAvg,
        double executionTimeMsAvg,
        long waitSampleCount,
        long executionSampleCount,
        Instant metricsResetAt
) {
}
