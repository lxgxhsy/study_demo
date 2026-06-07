package com.example.concurrencylab.threadpool;

class TimedRunnable implements Runnable {

    private final Runnable delegate;
    private final ThreadPoolCounters counters;
    private final long submittedAtNanos;

    TimedRunnable(Runnable delegate, ThreadPoolCounters counters) {
        this.delegate = delegate;
        this.counters = counters;
        this.submittedAtNanos = System.nanoTime();
    }

    @Override
    public void run() {
        long startedAtNanos = System.nanoTime();
        counters.recordWaitNanos(startedAtNanos - submittedAtNanos);
        try {
            delegate.run();
        } finally {
            counters.recordExecutionNanos(System.nanoTime() - startedAtNanos);
        }
    }
}
