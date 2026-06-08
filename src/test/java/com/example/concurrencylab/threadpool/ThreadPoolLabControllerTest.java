package com.example.concurrencylab.threadpool;

import com.example.concurrencylab.support.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local-h2")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ThreadPoolLabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    void configCanBeRead() throws Exception {
        mockMvc.perform(get("/api/lab/thread-pool/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corePoolSize", is(4)))
                .andExpect(jsonPath("$.maximumPoolSize", is(8)))
                .andExpect(jsonPath("$.queueCapacity", is(64)))
                .andExpect(jsonPath("$.rejectionPolicy", is("ABORT")));
    }

    @Test
    void coreGreaterThanMaxIsRejected() throws Exception {
        mockMvc.perform(put("/api/lab/thread-pool/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "corePoolSize": 9,
                                  "maximumPoolSize": 8,
                                  "queueCapacity": 64,
                                  "keepAliveSeconds": 60,
                                  "allowCoreThreadTimeOut": false,
                                  "rejectionPolicy": "ABORT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCode.INVALID_THREAD_POOL_CONFIG.name())));
    }

    @Test
    void queueShrinkBelowCurrentSizeIsRejected() throws Exception {
        mockMvc.perform(post("/api/lab/thread-pool/tasks/sleep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 40,
                                  "durationMs": 300
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/lab/thread-pool/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "corePoolSize": 1,
                                  "maximumPoolSize": 1,
                                  "queueCapacity": 1,
                                  "keepAliveSeconds": 60,
                                  "allowCoreThreadTimeOut": false,
                                  "rejectionPolicy": "ABORT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCode.QUEUE_CAPACITY_TOO_SMALL.name())));
    }

    @Test
    void unsupportedRejectionPolicyIsRejectedWithStableCode() throws Exception {
        mockMvc.perform(put("/api/lab/thread-pool/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "corePoolSize": 1,
                                  "maximumPoolSize": 1,
                                  "queueCapacity": 1,
                                  "keepAliveSeconds": 60,
                                  "allowCoreThreadTimeOut": false,
                                  "rejectionPolicy": "DISCARD"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCode.UNSUPPORTED_REJECTION_POLICY.name())));
    }

    @Test
    void sleepTaskSubmissionReturnsAcceptedAndRejectedCounts() throws Exception {
        mockMvc.perform(put("/api/lab/thread-pool/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "corePoolSize": 1,
                                  "maximumPoolSize": 1,
                                  "queueCapacity": 1,
                                  "keepAliveSeconds": 60,
                                  "allowCoreThreadTimeOut": false,
                                  "rejectionPolicy": "ABORT"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lab/thread-pool/tasks/sleep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 20,
                                  "durationMs": 200
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedCount", is(2)))
                .andExpect(jsonPath("$.rejectedCount", is(18)))
                .andExpect(jsonPath("$.callerRunsCount", is(0)))
                .andExpect(jsonPath("$.submittedCount", is(20)))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }

    @Test
    void metricsResetClearsCumulativeCounters() throws Exception {
        mockMvc.perform(post("/api/lab/thread-pool/tasks/sleep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 1,
                                  "durationMs": 1
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lab/thread-pool/metrics/reset"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/lab/thread-pool/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.submittedTaskCount", is(0)))
                .andExpect(jsonPath("$.completedTaskCount", is(0)))
                .andExpect(jsonPath("$.rejectedTaskCount", is(0)))
                .andExpect(jsonPath("$.callerRunsTaskCount", is(0)))
                .andExpect(jsonPath("$.metricsResetAt", notNullValue()));
    }

    @Test
    void callerRunsIsTrackedSeparatelyFromRejectedTasks() throws Exception {
        mockMvc.perform(put("/api/lab/thread-pool/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "corePoolSize": 1,
                                  "maximumPoolSize": 1,
                                  "queueCapacity": 1,
                                  "keepAliveSeconds": 60,
                                  "allowCoreThreadTimeOut": false,
                                  "rejectionPolicy": "CALLER_RUNS"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lab/thread-pool/tasks/sleep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 6,
                                  "durationMs": 50
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedCount", is(6)))
                .andExpect(jsonPath("$.rejectedCount", is(0)))
                .andExpect(jsonPath("$.callerRunsCount", greaterThan(0)))
                .andExpect(jsonPath("$.submittedCount", is(6)));

        mockMvc.perform(get("/api/lab/thread-pool/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rejectedTaskCount", is(0)))
                .andExpect(jsonPath("$.callerRunsTaskCount", greaterThan(0)));
    }

    @Test
    void metricsResetStartsNewGenerationAndIgnoresOldRunningTasks() throws Exception {
        mockMvc.perform(put("/api/lab/thread-pool/config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "corePoolSize": 1,
                                  "maximumPoolSize": 1,
                                  "queueCapacity": 4,
                                  "keepAliveSeconds": 60,
                                  "allowCoreThreadTimeOut": false,
                                  "rejectionPolicy": "ABORT"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/lab/thread-pool/tasks/sleep")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 5,
                                  "durationMs": 150
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedCount", is(5)));

        mockMvc.perform(get("/api/lab/thread-pool/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricsGeneration", is(0)));

        mockMvc.perform(post("/api/lab/thread-pool/metrics/reset"))
                .andExpect(status().isNoContent());

        Thread.sleep(900);

        mockMvc.perform(get("/api/lab/thread-pool/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricsGeneration", is(1)))
                .andExpect(jsonPath("$.submittedTaskCount", is(0)))
                .andExpect(jsonPath("$.completedTaskCount", is(0)))
                .andExpect(jsonPath("$.waitSampleCount", is(0)))
                .andExpect(jsonPath("$.executionSampleCount", is(0)));
    }
}
