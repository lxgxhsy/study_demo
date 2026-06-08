package com.example.concurrencylab.threadpool;

class TimedRunnable implements Runnable {

    private final Runnable delegate;
    private final ThreadPoolCounters counters;
    private final long submittedAtNanos;
    private final long metricsGeneration;

    TimedRunnable(Runnable delegate, ThreadPoolCounters counters, long metricsGeneration) {
        this.delegate = delegate;
        this.counters = counters;
        this.submittedAtNanos = System.nanoTime();
        this.metricsGeneration = metricsGeneration;
    }

    long metricsGeneration() {
        return metricsGeneration;
    }

    @Override
    public void run() {
        long startedAtNanos = System.nanoTime();
        counters.recordWaitNanos(metricsGeneration, startedAtNanos - submittedAtNanos);
        try {
            delegate.run();
        } finally {
            counters.recordExecutionNanos(metricsGeneration, System.nanoTime() - startedAtNanos);
            counters.incrementCompleted(metricsGeneration);
        }
    }
}
