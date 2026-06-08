package com.example.concurrencylab.threadpool;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadPoolCounters {

    private final AtomicLong submittedTaskCount = new AtomicLong();
    private final AtomicLong completedTaskCount = new AtomicLong();
    private final AtomicLong rejectedTaskCount = new AtomicLong();
    private final AtomicLong callerRunsTaskCount = new AtomicLong();
    private final AtomicLong waitTimeNanosTotal = new AtomicLong();
    private final AtomicLong executionTimeNanosTotal = new AtomicLong();
    private final AtomicLong waitSampleCount = new AtomicLong();
    private final AtomicLong executionSampleCount = new AtomicLong();
    private final AtomicLong metricsGeneration = new AtomicLong();
    private volatile Instant metricsResetAt = Instant.now();

    public synchronized long registerSubmittedAndGetGeneration() {
        long generation = metricsGeneration.get();
        submittedTaskCount.incrementAndGet();
        return generation;
    }

    public synchronized long incrementRejected(long generation) {
        if (generation != metricsGeneration.get()) {
            return rejectedTaskCount.get();
        }
        return rejectedTaskCount.incrementAndGet();
    }

    public synchronized long incrementCallerRuns(long generation) {
        if (generation != metricsGeneration.get()) {
            return callerRunsTaskCount.get();
        }
        return callerRunsTaskCount.incrementAndGet();
    }

    public synchronized long incrementCompleted(long generation) {
        if (generation != metricsGeneration.get()) {
            return completedTaskCount.get();
        }
        return completedTaskCount.incrementAndGet();
    }

    public long currentGeneration() {
        return metricsGeneration.get();
    }

    public synchronized void recordWaitNanos(long generation, long nanos) {
        if (generation != metricsGeneration.get()) {
            return;
        }
        waitTimeNanosTotal.addAndGet(Math.max(0, nanos));
        waitSampleCount.incrementAndGet();
    }

    public synchronized void recordExecutionNanos(long generation, long nanos) {
        if (generation != metricsGeneration.get()) {
            return;
        }
        executionTimeNanosTotal.addAndGet(Math.max(0, nanos));
        executionSampleCount.incrementAndGet();
    }

    public synchronized void reset() {
        metricsGeneration.incrementAndGet();
        submittedTaskCount.set(0);
        completedTaskCount.set(0);
        rejectedTaskCount.set(0);
        callerRunsTaskCount.set(0);
        waitTimeNanosTotal.set(0);
        executionTimeNanosTotal.set(0);
        waitSampleCount.set(0);
        executionSampleCount.set(0);
        metricsResetAt = Instant.now();
    }

    public long submittedTaskCount() {
        return submittedTaskCount.get();
    }

    public long completedTaskCount() {
        return completedTaskCount.get();
    }

    public long rejectedTaskCount() {
        return rejectedTaskCount.get();
    }

    public long callerRunsTaskCount() {
        return callerRunsTaskCount.get();
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

    public long metricsGeneration() {
        return metricsGeneration.get();
    }

    private static double nanosToMillis(long nanos) {
        return nanos / 1_000_000.0;
    }
}
