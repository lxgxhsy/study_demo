package com.example.concurrencylab.threadpool;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lab/thread-pool")
public class ThreadPoolLabController {

    private final DynamicThreadPoolManager manager;

    public ThreadPoolLabController(DynamicThreadPoolManager manager) {
        this.manager = manager;
    }

    @GetMapping("/config")
    public ThreadPoolConfigResponse currentConfig() {
        return manager.currentConfig();
    }

    @PutMapping("/config")
    public ThreadPoolConfigResponse updateConfig(@Valid @RequestBody ThreadPoolConfigRequest request) {
        return manager.updateConfig(request);
    }

    @GetMapping("/metrics")
    public ThreadPoolMetricsResponse metrics() {
        return manager.metrics();
    }

    @PostMapping("/metrics/reset")
    public ResponseEntity<Void> resetMetrics() {
        manager.resetMetrics();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tasks/sleep")
    public TaskSubmissionResponse submitSleepTasks(@Valid @RequestBody SleepTaskRequest request) {
        return manager.submitSleepTasks(request);
    }

    @PostMapping("/tasks/cpu")
    public TaskSubmissionResponse submitCpuTasks(@Valid @RequestBody CpuTaskRequest request) {
        return manager.submitCpuTasks(request);
    }

    @PostMapping("/tasks/mixed")
    public TaskSubmissionResponse submitMixedTasks(@Valid @RequestBody MixedTaskRequest request) {
        return manager.submitMixedTasks(request);
    }
}
