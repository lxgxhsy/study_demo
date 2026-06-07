package com.example.concurrencylab.threadpool;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPoolCounters {

    private final AtomicLong submittedTaskCount = new AtomicLong();
    private final AtomicLong rejectedTaskCount = new AtomicLong();
    private final AtomicLong waitTimeNanosTotal = new AtomicLong();
    private final AtomicLong executionTimeNanosTotal = new AtomicLong();
    private final AtomicLong waitSampleCount = new AtomicLong();
    private final AtomicLong executionSampleCount = new AtomicLong();
    private volatile Instant metricsResetAt = Instant.now();

    public long incrementSubmitted() {
        return submittedTaskCount.incrementAndGet();
    }

    public long incrementRejected() {
        return rejectedTaskCount.incrementAndGet();
    }

    public void recordWaitNanos(long nanos) {
        waitTimeNanosTotal.addAndGet(Math.max(0, nanos));
        waitSampleCount.incrementAndGet();
    }

    public void recordExecutionNanos(long nanos) {
        executionTimeNanosTotal.addAndGet(Math.max(0, nanos));
        executionSampleCount.incrementAndGet();
    }

    public void reset() {
        submittedTaskCount.set(0);
        rejectedTaskCount.set(0);
        waitTimeNanosTotal.set(0);
        executionTimeNanosTotal.set(0);
        waitSampleCount.set(0);
        executionSampleCount.set(0);
        metricsResetAt = Instant.now();
    }

    public long submittedTaskCount() {
        return submittedTaskCount.get();
    }

    public long rejectedTaskCount() {
        return rejectedTaskCount.get();
    }

    public double waitTimeMsAvg() {
        long samples = waitSampleCount.get();
        return samples == 0 ? 0.0 : nanosToMillis(waitTimeNanosTotal.get()) / samples;
    }

    public double executionTimeMsAvg() {
        long samples = executionSampleCount.get();
        return samples == 0 ? 0.0 : nanosToMillis(executionTimeNanosTotal.get()) / samples;
    }

    public long waitSampleCount() {
        return waitSampleCount.get();
    }

    public long executionSampleCount() {
        return executionSampleCount.get();
    }

    public Instant metricsResetAt() {
        return metricsResetAt;
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
