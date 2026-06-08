package com.example.concurrencylab.id;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "concurrency-lab.id.default-step=1000",
        "concurrency-lab.id.segment-allocation-max-retries=128",
        "concurrency-lab.id.preload-wait-timeout-ms=10000"
})
@ActiveProfiles("mysql-leaf")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EnabledIfEnvironmentVariable(named = "MYSQL_LEAF_IT_ENABLED", matches = "true")
class MySqlLeafConcurrencyIT {

    private static final int CONCURRENCY = 64;
    private static final long STEP = 1000L;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private LeafAllocRepository repository;

    @Autowired
    private SegmentIdGenerator idGenerator;

    @BeforeEach
    void prepareSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS leaf_alloc (
                    biz_tag VARCHAR(128) PRIMARY KEY,
                    max_id BIGINT NOT NULL,
                    step BIGINT NOT NULL,
                    version BIGINT NOT NULL,
                    update_time TIMESTAMP NOT NULL
                ) ENGINE=InnoDB
                """);
        jdbcTemplate.update("DELETE FROM leaf_alloc WHERE biz_tag LIKE 'mysql-it-%'");
    }

    @Test
    void concurrentSegmentAllocationDoesNotOverlapUnderMySqlProfile() throws Exception {
        String bizTag = "mysql-it-repo";
        List<SegmentRange> ranges = runConcurrently(CONCURRENCY, () -> repository.allocateSegment(bizTag));
        ranges.sort(Comparator.comparingLong(SegmentRange::start));

        assertEquals(CONCURRENCY, ranges.size());
        for (int i = 0; i < ranges.size(); i++) {
            SegmentRange range = ranges.get(i);
            assertEquals(STEP, range.step());
            assertEquals(STEP, range.end() - range.start() + 1);
            if (i > 0) {
                assertTrue(range.start() > ranges.get(i - 1).end(), "segment ranges must not overlap");
            }
        }

        Long maxId = jdbcTemplate.queryForObject(
                "SELECT max_id FROM leaf_alloc WHERE biz_tag = ?",
                Long.class,
                bizTag
        );
        Long version = jdbcTemplate.queryForObject(
                "SELECT version FROM leaf_alloc WHERE biz_tag = ?",
                Long.class,
                bizTag
        );
        assertEquals(CONCURRENCY * STEP, maxId);
        assertEquals(CONCURRENCY, version);
    }

    @Test
    void concurrentIdGenerationHasNoDuplicatesAndCrossesAtLeastThreeSegments() throws Exception {
        String bizTag = "mysql-it-generator";
        int idsPerThread = 47;
        int expectedCount = CONCURRENCY * idsPerThread;
        assertTrue(expectedCount >= STEP * 3);

        Set<Long> ids = ConcurrentHashMap.newKeySet();
        List<Integer> generatedCounts = runConcurrently(CONCURRENCY, () -> {
            int generated = 0;
            for (int i = 0; i < idsPerThread; i++) {
                if (ids.add(idGenerator.nextId(bizTag).id())) {
                    generated++;
                }
            }
            return generated;
        });

        assertEquals(expectedCount, generatedCounts.stream().mapToInt(Integer::intValue).sum());
        assertEquals(expectedCount, ids.size());

        LeafIdMetricsResponse metrics = idGenerator.metrics(bizTag);
        assertTrue(metrics.dbAllocationCount() >= 4, "expected to allocate enough segments for step * 3 IDs");
        assertTrue(metrics.segmentSwitchCount() >= 3, "expected to switch across multiple segments");
    }

    private static <T> List<T> runConcurrently(int concurrency, ThrowingSupplier<T> supplier) throws Exception {
        CountDownLatch ready = new CountDownLatch(concurrency);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        List<Future<T>> futures = new ArrayList<>(concurrency);

        try {
            for (int i = 0; i < concurrency; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS), "timed out waiting for concurrent start");
                    return supplier.get();
                }));
            }

            assertTrue(ready.await(10, TimeUnit.SECONDS), "workers did not become ready");
            start.countDown();

            List<T> results = new ArrayList<>(concurrency);
            for (Future<T> future : futures) {
                results.add(future.get(60, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS), "executor did not stop");
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
}
