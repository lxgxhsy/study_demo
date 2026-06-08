package com.example.concurrencylab.threadpool;

import com.example.concurrencylab.support.ApiException;
import com.example.concurrencylab.support.ErrorCode;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

@Service
public class DynamicThreadPoolManager {

    private final ThreadPoolProperties properties;
    private final MonitoredThreadPoolExecutor executor;

    public DynamicThreadPoolManager(ThreadPoolProperties properties) {
        this.properties = properties;
        ResizableCapacityBlockingQueue<Runnable> queue =
                new ResizableCapacityBlockingQueue<>(properties.getQueueCapacity());
        this.executor = new MonitoredThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaximumPoolSize(),
                properties.getKeepAliveSeconds(),
                properties.isAllowCoreThreadTimeOut(),
                properties.getRejectionPolicy(),
                queue
        );
    }

    public synchronized ThreadPoolConfigResponse currentConfig() {
        return new ThreadPoolConfigResponse(
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.queueCapacity(),
                executor.getQueue().size(),
                executor.getKeepAliveTime(TimeUnit.SECONDS),
                executor.allowsCoreThreadTimeOut(),
                executor.rejectionPolicy()
        );
    }

    public synchronized ThreadPoolConfigResponse updateConfig(ThreadPoolConfigRequest request) {
        validateConfig(request);

        if (request.queueCapacity() < executor.getQueue().size()) {
            throw new ApiException(
                    ErrorCode.QUEUE_CAPACITY_TOO_SMALL,
                    HttpStatus.BAD_REQUEST,
                    "newQueueCapacity must be greater than or equal to currentQueueSize"
            );
        }

        executor.setQueueCapacity(request.queueCapacity());
        if (request.maximumPoolSize() < executor.getCorePoolSize()) {
            executor.setCorePoolSize(request.corePoolSize());
            executor.setMaximumPoolSize(request.maximumPoolSize());
        } else {
            executor.setMaximumPoolSize(request.maximumPoolSize());
            executor.setCorePoolSize(request.corePoolSize());
        }
        executor.setKeepAliveTime(request.keepAliveSeconds(), TimeUnit.SECONDS);
        executor.allowCoreThreadTimeOut(request.allowCoreThreadTimeOut());
        executor.applyRejectionPolicy(request.rejectionPolicy());
        return currentConfig();
    }

    public ThreadPoolMetricsResponse metrics() {
        return executor.metrics();
    }

    public void resetMetrics() {
        executor.resetMetrics();
    }

    public TaskSubmissionResponse submitSleepTasks(SleepTaskRequest request) {
        validateBatch(request.count());
        return submitBatch(request.count(), () -> sleep(request.durationMs()));
    }

    public TaskSubmissionResponse submitCpuTasks(CpuTaskRequest request) {
        validateBatch(request.count());
        return submitBatch(request.count(), () -> consumeCpu(request.iterations()));
    }

    public TaskSubmissionResponse submitMixedTasks(MixedTaskRequest request) {
        validateBatch(request.count());
        return submitBatch(request.count(), () -> {
            consumeCpu(request.iterations());
            sleep(request.durationMs());
        });
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    private TaskSubmissionResponse submitBatch(int count, Runnable task) {
        String requestId = UUID.randomUUID().toString();
        long rejectedBefore = executor.rejectedTaskCount();
        long callerRunsBefore = executor.callerRunsTaskCount();
        int thrownRejections = 0;
        for (int i = 0; i < count; i++) {
            try {
                executor.execute(task);
            } catch (RejectedExecutionException ignored) {
                thrownRejections++;
            }
        }
        int rejectedCount = Math.max(
                thrownRejections,
                Math.toIntExact(Math.max(0, executor.rejectedTaskCount() - rejectedBefore))
        );
        int callerRunsCount = Math.toIntExact(Math.max(0, executor.callerRunsTaskCount() - callerRunsBefore));
        int acceptedCount = Math.max(0, count - rejectedCount);
        return new TaskSubmissionResponse(acceptedCount, rejectedCount, callerRunsCount, count, requestId);
    }

    private void validateConfig(ThreadPoolConfigRequest request) {
        if (request.corePoolSize() > request.maximumPoolSize()) {
            throw new ApiException(
                    ErrorCode.INVALID_THREAD_POOL_CONFIG,
                    HttpStatus.BAD_REQUEST,
                    "corePoolSize must be less than or equal to maximumPoolSize"
            );
        }
    }

    private void validateBatch(int count) {
        if (count > properties.getMaxBatchSize()) {
            throw new ApiException(
                    ErrorCode.INVALID_TASK_REQUEST,
                    HttpStatus.BAD_REQUEST,
                    "count exceeds maxBatchSize=" + properties.getMaxBatchSize()
            );
        }
    }

    private static void sleep(long durationMs) {
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void consumeCpu(long iterations) {
        long value = 0;
        for (long i = 0; i < iterations; i++) {
            value += (i * 31) ^ (value >>> 3);
        }
        if (value == Long.MIN_VALUE) {
            throw new IllegalStateException("unreachable guard to keep CPU loop visible");
        }
    }
}
