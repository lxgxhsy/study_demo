package com.example.concurrencylab.orderlab;

import jakarta.validation.constraints.Min;

public record OrderRequest(
        String bizTag,
        @Min(0) long asyncTaskDurationMs
) {
    public String normalizedBizTag() {
        return bizTag == null || bizTag.isBlank() ? "order" : bizTag.trim();
    }
}
