package com.example.concurrencylab.orderlab;

import com.example.concurrencylab.id.IdResponse;
import com.example.concurrencylab.id.SegmentIdGenerator;
import com.example.concurrencylab.threadpool.DynamicThreadPoolManager;
import com.example.concurrencylab.threadpool.SleepTaskRequest;
import com.example.concurrencylab.threadpool.TaskSubmissionResponse;
import org.springframework.stereotype.Service;

@Service
public class OrderLabService {

    private final SegmentIdGenerator idGenerator;
    private final DynamicThreadPoolManager threadPoolManager;

    public OrderLabService(SegmentIdGenerator idGenerator, DynamicThreadPoolManager threadPoolManager) {
        this.idGenerator = idGenerator;
        this.threadPoolManager = threadPoolManager;
    }

    public OrderResponse createOrder(OrderRequest request) {
        IdResponse id = idGenerator.nextId(request.normalizedBizTag());
        TaskSubmissionResponse task = threadPoolManager.submitSleepTasks(new SleepTaskRequest(1, request.asyncTaskDurationMs()));
        return new OrderResponse(id.id(), task.acceptedCount(), task.rejectedCount(), task.requestId());
    }
}
