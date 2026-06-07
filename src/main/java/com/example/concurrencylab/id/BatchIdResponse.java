package com.example.concurrencylab.id;

import java.util.List;

public record BatchIdResponse(
        String bizTag,
        List<Long> ids,
        int count,
        int duplicateCount,
        String requestId
) {
}
