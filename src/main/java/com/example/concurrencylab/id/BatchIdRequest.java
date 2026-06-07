package com.example.concurrencylab.id;

import jakarta.validation.constraints.Min;

public record BatchIdRequest(
        @Min(1) int count
) {
}
