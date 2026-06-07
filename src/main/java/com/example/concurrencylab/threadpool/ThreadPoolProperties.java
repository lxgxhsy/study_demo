package com.example.concurrencylab.threadpool;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "concurrency-lab.thread-pool")
public class ThreadPoolProperties {

    private int corePoolSize = 4;
    private int maximumPoolSize = 8;
    private int queueCapacity = 64;
    private long keepAliveSeconds = 60;
    private boolean allowCoreThreadTimeOut = false;
    private RejectionPolicy rejectionPolicy = RejectionPolicy.ABORT;
    private int maxBatchSize = 1000;

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public void setMaximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public long getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(long keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public boolean isAllowCoreThreadTimeOut() {
        return allowCoreThreadTimeOut;
    }

    public void setAllowCoreThreadTimeOut(boolean allowCoreThreadTimeOut) {
        this.allowCoreThreadTimeOut = allowCoreThreadTimeOut;
    }

    public RejectionPolicy getRejectionPolicy() {
        return rejectionPolicy;
    }

    public void setRejectionPolicy(RejectionPolicy rejectionPolicy) {
        this.rejectionPolicy = rejectionPolicy;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }
}
