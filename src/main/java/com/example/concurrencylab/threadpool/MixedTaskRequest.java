package com.example.concurrencylab.threadpool;

import jakarta.validation.constraints.Min;

public record MixedTaskRequest(
        @Min(1) int count,
        @Min(0) long durationMs,
        @Min(1) long iterations
) {
}
