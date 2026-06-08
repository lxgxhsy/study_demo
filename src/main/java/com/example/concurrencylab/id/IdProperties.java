package com.example.concurrencylab.id;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "concurrency-lab.id")
public class IdProperties {

    private long defaultStep = 1000;
    private double preloadThresholdRatio = 0.1;
    private int maxBatchSize = 10000;
    private long preloadWaitTimeoutMs = 5000;
    private int segmentAllocationMaxRetries = 128;

    public long getDefaultStep() {
        return defaultStep;
    }

    public void setDefaultStep(long defaultStep) {
        this.defaultStep = defaultStep;
    }

    public double getPreloadThresholdRatio() {
        return preloadThresholdRatio;
    }

    public void setPreloadThresholdRatio(double preloadThresholdRatio) {
        this.preloadThresholdRatio = preloadThresholdRatio;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    public void setMaxBatchSize(int maxBatchSize) {
        this.maxBatchSize = maxBatchSize;
    }

    public long getPreloadWaitTimeoutMs() {
        return preloadWaitTimeoutMs;
    }

    public void setPreloadWaitTimeoutMs(long preloadWaitTimeoutMs) {
        this.preloadWaitTimeoutMs = preloadWaitTimeoutMs;
    }

    public int getSegmentAllocationMaxRetries() {
        return segmentAllocationMaxRetries;
    }

    public void setSegmentAllocationMaxRetries(int segmentAllocationMaxRetries) {
        this.segmentAllocationMaxRetries = segmentAllocationMaxRetries;
    }
}
