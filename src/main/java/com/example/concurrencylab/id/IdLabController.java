package com.example.concurrencylab.id;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lab/id")
public class IdLabController {

    private final SegmentIdGenerator generator;

    public IdLabController(SegmentIdGenerator generator) {
        this.generator = generator;
    }

    @GetMapping("/{bizTag}")
    public IdResponse nextId(@PathVariable String bizTag) {
        return generator.nextId(bizTag);
    }

    @PostMapping("/{bizTag}/batch")
    public BatchIdResponse nextBatch(@PathVariable String bizTag, @Valid @RequestBody BatchIdRequest request) {
        return generator.nextBatch(bizTag, request.count());
    }

    @GetMapping("/{bizTag}/metrics")
    public LeafIdMetricsResponse metrics(@PathVariable String bizTag) {
        return generator.metrics(bizTag);
    }
}
