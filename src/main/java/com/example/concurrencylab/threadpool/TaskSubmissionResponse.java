package com.example.concurrencylab.threadpool;

public record TaskSubmissionResponse(
        int acceptedCount,
        int rejectedCount,
        int submittedCount,
        String requestId
) {
}
