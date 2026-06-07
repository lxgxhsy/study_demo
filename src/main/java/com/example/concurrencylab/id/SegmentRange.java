package com.example.concurrencylab.id;

public record SegmentRange(
        long start,
        long end,
        long step
) {
}
