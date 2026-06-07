package com.example.concurrencylab.threadpool;

import jakarta.validation.constraints.Min;

public record CpuTaskRequest(
        @Min(1) int count,
        @Min(1) long iterations
) {
}
