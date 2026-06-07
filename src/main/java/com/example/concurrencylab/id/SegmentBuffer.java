package com.example.concurrencylab.id;

import java.util.concurrent.atomic.AtomicLong;

public class SegmentBuffer {

    private final long start;
    private final long end;
    private final long step;
    private final AtomicLong value;

    public SegmentBuffer(SegmentRange range) {
        this.start = range.start();
        this.end = range.end();
        this.step = range.step();
        this.value = new AtomicLong(start);
    }

    public long nextId() {
        long next = value.getAndIncrement();
        return next <= end ? next : -1;
    }

    public long remaining() {
        long current = value.get();
        return Math.max(0, end - current + 1);
    }

    public boolean belowThreshold(double ratio) {
        return remaining() <= Math.max(1, Math.ceil(step * ratio));
    }

    public long start() {
        return start;
    }

    public long end() {
        return end;
    }

    public long currentValue() {
        return Math.min(value.get(), end + 1);
    }
}
