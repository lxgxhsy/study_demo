package com.example.concurrencylab.threadpool;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MonitoredThreadPoolExecutor extends ThreadPoolExecutor {

    private final ResizableCapacityBlockingQueue<Runnable> queue;
    private final ThreadPoolCounters counters = new ThreadPoolCounters();
    private volatile RejectionPolicy rejectionPolicy;

    public MonitoredThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveSeconds,
            boolean allowCoreThreadTimeOut,
            RejectionPolicy rejectionPolicy,
            ResizableCapacityBlockingQueue<Runnable> queue
    ) {
        super(corePoolSize, maximumPoolSize, keepAliveSeconds, TimeUnit.SECONDS, queue);
        this.queue = queue;
        allowCoreThreadTimeOut(allowCoreThreadTimeOut);
        applyRejectionPolicy(rejectionPolicy);
    }

    @Override
    public void execute(Runnable command) {
        long generation = counters.registerSubmittedAndGetGeneration();
        super.execute(new TimedRunnable(command, counters, generation));
    }

    public void applyRejectionPolicy(RejectionPolicy policy) {
        this.rejectionPolicy = policy;
        setRejectedExecutionHandler(toHandler(policy));
    }

    public RejectionPolicy rejectionPolicy() {
        return rejectionPolicy;
    }

    public int queueCapacity() {
        return queue.capacity();
    }

    public void setQueueCapacity(int queueCapacity) {
        queue.setCapacity(queueCapacity);
    }

    public void resetMetrics() {
        counters.reset();
    }

    public ThreadPoolMetricsResponse metrics() {
        return new ThreadPoolMetricsResponse(
                getActiveCount(),
                getPoolSize(),
                getLargestPoolSize(),
                queue.size(),
                queue.capacity(),
                getTaskCount(),
                counters.completedTaskCount(),
                counters.submittedTaskCount(),
                counters.rejectedTaskCount(),
                counters.callerRunsTaskCount(),
                rejectionPolicy,
                getKeepAliveTime(TimeUnit.SECONDS),
                allowsCoreThreadTimeOut(),
                counters.waitTimeMsAvg(),
                counters.executionTimeMsAvg(),
                counters.waitSampleCount(),
                counters.executionSampleCount(),
                counters.metricsGeneration(),
                counters.metricsResetAt()
        );
    }

    public long rejectedTaskCount() {
        return counters.rejectedTaskCount();
    }

    public long callerRunsTaskCount() {
        return counters.callerRunsTaskCount();
    }

    private RejectedExecutionHandler toHandler(RejectionPolicy policy) {
        return switch (policy) {
            case ABORT -> (r, executor) -> {
                counters.incrementRejected(metricsGenerationOf(r));
                throw new RejectedExecutionException("Task rejected from lab thread pool");
            };
            case CALLER_RUNS -> (r, executor) -> {
                if (!executor.isShutdown()) {
                    counters.incrementCallerRuns(metricsGenerationOf(r));
                    r.run();
                } else {
                    counters.incrementRejected(metricsGenerationOf(r));
                }
            };
        };
    }

    private long metricsGenerationOf(Runnable runnable) {
        if (runnable instanceof TimedRunnable timedRunnable) {
            return timedRunnable.metricsGeneration();
        }
        return counters.currentGeneration();
    }
}
