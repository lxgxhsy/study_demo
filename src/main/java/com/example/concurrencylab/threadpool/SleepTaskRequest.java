package com.example.concurrencylab.threadpool;

import jakarta.validation.constraints.Min;

public record SleepTaskRequest(
        @Min(1) int count,
        @Min(0) long durationMs
) {
}
