package com.example.concurrencylab.id;

public record LeafIdMetricsResponse(
        String bizTag,
        long currentStart,
        long currentEnd,
        long currentValue,
        long remaining,
        boolean nextReady,
        boolean preloadRunning,
        long dbAllocationCount,
        long segmentSwitchCount
) {
}
