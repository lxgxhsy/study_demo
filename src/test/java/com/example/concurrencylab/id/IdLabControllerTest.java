package com.example.concurrencylab.id;

import com.example.concurrencylab.support.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "concurrency-lab.id.default-step=10",
        "concurrency-lab.id.max-batch-size=30"
})
@ActiveProfiles("local-h2")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IdLabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearLeafState() {
        jdbcTemplate.update("DELETE FROM leaf_alloc");
    }

    @Test
    void singleIdsIncreaseForBizTag() throws Exception {
        mockMvc.perform(get("/api/lab/id/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bizTag", is("order")))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.requestId", notNullValue()));

        mockMvc.perform(get("/api/lab/id/order"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(2)));
    }

    @Test
    void batchGenerationCrossesSegmentsWithoutDuplicatesInFunctionalProfile() throws Exception {
        mockMvc.perform(post("/api/lab/id/order/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 25
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(25)))
                .andExpect(jsonPath("$.duplicateCount", is(0)))
                .andExpect(jsonPath("$.ids[0]", is(1)))
                .andExpect(jsonPath("$.ids[24]", is(25)));

        mockMvc.perform(get("/api/lab/id/order/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dbAllocationCount", greaterThan(1)))
                .andExpect(jsonPath("$.remaining", greaterThan(-1)));
    }

    @Test
    void batchSizeLimitIsRejected() throws Exception {
        mockMvc.perform(post("/api/lab/id/order/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "count": 31
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCode.ID_BATCH_SIZE_TOO_LARGE.name())));
    }
}
