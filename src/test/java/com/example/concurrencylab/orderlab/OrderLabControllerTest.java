package com.example.concurrencylab.orderlab;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "concurrency-lab.id.default-step=10")
@ActiveProfiles("local-h2")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OrderLabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearLeafState() {
        jdbcTemplate.update("DELETE FROM leaf_alloc");
    }

    @Test
    void createOrderReturnsOrderIdAndAsyncTaskCounts() throws Exception {
        mockMvc.perform(post("/api/lab/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bizTag": "order",
                                  "asyncTaskDurationMs": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId", is(1)))
                .andExpect(jsonPath("$.acceptedAsyncTasks", is(1)))
                .andExpect(jsonPath("$.rejectedAsyncTasks", is(0)))
                .andExpect(jsonPath("$.requestId", notNullValue()));
    }
}
