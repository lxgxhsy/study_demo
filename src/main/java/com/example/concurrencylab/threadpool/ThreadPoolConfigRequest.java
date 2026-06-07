package com.example.concurrencylab.threadpool;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ThreadPoolConfigRequest(
        @Min(0) int corePoolSize,
        @Min(1) int maximumPoolSize,
        @Min(1) int queueCapacity,
        @Min(1) long keepAliveSeconds,
        boolean allowCoreThreadTimeOut,
        @NotNull RejectionPolicy rejectionPolicy
) {
}
