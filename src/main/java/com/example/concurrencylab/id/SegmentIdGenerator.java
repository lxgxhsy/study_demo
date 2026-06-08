package com.example.concurrencylab.id;

import com.example.concurrencylab.support.ApiException;
import com.example.concurrencylab.support.ErrorCode;
import jakarta.annotation.PreDestroy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SegmentIdGenerator {

    private final LeafAllocRepository repository;
    private final IdProperties properties;
    private final Map<String, GeneratorState> states = new ConcurrentHashMap<>();
    private final ExecutorService preloadExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "leaf-preload");
        thread.setDaemon(true);
        return thread;
    });

    public SegmentIdGenerator(LeafAllocRepository repository, IdProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    public IdResponse nextId(String bizTag) {
        long id = stateFor(bizTag).nextId();
        return new IdResponse(bizTag, id, UUID.randomUUID().toString());
    }

    public BatchIdResponse nextBatch(String bizTag, int count) {
        if (count > properties.getMaxBatchSize()) {
            throw new ApiException(
                    ErrorCode.ID_BATCH_SIZE_TOO_LARGE,
                    HttpStatus.BAD_REQUEST,
                    "count exceeds maxBatchSize=" + properties.getMaxBatchSize()
            );
        }
        List<Long> ids = new ArrayList<>(count);
        GeneratorState state = stateFor(bizTag);
        for (int i = 0; i < count; i++) {
            ids.add(state.nextId());
        }
        int duplicateCount = ids.size() - new HashSet<>(ids).size();
        return new BatchIdResponse(bizTag, ids, ids.size(), duplicateCount, UUID.randomUUID().toString());
    }

    public LeafIdMetricsResponse metrics(String bizTag) {
        return stateFor(bizTag).metrics();
    }

    @PreDestroy
    public void shutdown() {
        preloadExecutor.shutdownNow();
    }

    private GeneratorState stateFor(String bizTag) {
        return states.computeIfAbsent(bizTag, GeneratorState::new);
    }

    private class GeneratorState {
        private final String bizTag;
        private final AtomicLong dbAllocationCount = new AtomicLong();
        private final AtomicLong segmentSwitchCount = new AtomicLong();
        private final AtomicBoolean preloadRunning = new AtomicBoolean(false);
        private volatile SegmentBuffer current;
        private volatile SegmentBuffer next;

        GeneratorState(String bizTag) {
            this.bizTag = bizTag;
            this.current = loadSegment();
        }

        long nextId() {
            while (true) {
                SegmentBuffer localCurrent = current;
                long id = localCurrent.nextId();
                if (id > 0) {
                    maybePreload(localCurrent);
                    return id;
                }
                switchSegment();
            }
        }

        synchronized void switchSegment() {
            if (current.remaining() > 0) {
                return;
            }
            waitForPreloadIfRunning();
            if (next != null) {
                current = next;
                next = null;
                preloadRunning.set(false);
                segmentSwitchCount.incrementAndGet();
                maybePreload(current);
                return;
            }
            current = loadSegment();
            segmentSwitchCount.incrementAndGet();
            preloadRunning.set(false);
        }

        private void waitForPreloadIfRunning() {
            long deadline = System.currentTimeMillis() + properties.getPreloadWaitTimeoutMs();
            while (next == null && preloadRunning.get()) {
                try {
                    long waitMs = deadline - System.currentTimeMillis();
                    if (waitMs <= 0) {
                        throw new ApiException(
                                ErrorCode.ID_SEGMENT_ALLOC_FAILED,
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Timed out while waiting for preloaded ID segment"
                        );
                    }
                    wait(waitMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new ApiException(
                            ErrorCode.ID_SEGMENT_ALLOC_FAILED,
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Interrupted while waiting for preloaded ID segment"
                    );
                }
            }
        }

        void maybePreload(SegmentBuffer segment) {
            if (!segment.belowThreshold(properties.getPreloadThresholdRatio())) {
                return;
            }
            if (next != null || !preloadRunning.compareAndSet(false, true)) {
                return;
            }
            preloadExecutor.submit(() -> {
                try {
                    SegmentBuffer loaded = loadSegment();
                    synchronized (this) {
                        if (next == null && loaded.end() > current.end()) {
                            next = loaded;
                        }
                    }
                } finally {
                    synchronized (this) {
                        preloadRunning.set(false);
                        notifyAll();
                    }
                }
            });
        }

        SegmentBuffer loadSegment() {
            SegmentRange range = repository.allocateSegment(bizTag);
            dbAllocationCount.incrementAndGet();
            return new SegmentBuffer(range);
        }

        LeafIdMetricsResponse metrics() {
            SegmentBuffer localCurrent = current;
            return new LeafIdMetricsResponse(
                    bizTag,
                    localCurrent.start(),
                    localCurrent.end(),
                    localCurrent.currentValue(),
                    localCurrent.remaining(),
                    next != null,
                    preloadRunning.get(),
                    dbAllocationCount.get(),
                    segmentSwitchCount.get()
            );
        }
    }
}
