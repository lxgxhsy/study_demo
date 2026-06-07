package com.example.concurrencylab.id;

import com.example.concurrencylab.support.ApiException;
import com.example.concurrencylab.support.ErrorCode;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class LeafAllocRepository {

    private final JdbcTemplate jdbcTemplate;
    private final IdProperties properties;

    public LeafAllocRepository(JdbcTemplate jdbcTemplate, IdProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @Transactional
    public SegmentRange allocateSegment(String bizTag) {
        ensureRow(bizTag);

        for (int attempt = 0; attempt < 5; attempt++) {
            LeafAllocRow row = loadRow(bizTag);
            long newMaxId = row.maxId() + row.step();
            int updated = jdbcTemplate.update(
                    """
                            UPDATE leaf_alloc
                            SET max_id = ?, version = version + 1, update_time = ?
                            WHERE biz_tag = ? AND version = ?
                            """,
                    newMaxId,
                    Timestamp.from(Instant.now()),
                    bizTag,
                    row.version()
            );
            if (updated == 1) {
                return new SegmentRange(row.maxId() + 1, newMaxId, row.step());
            }
        }

        throw new ApiException(
                ErrorCode.ID_SEGMENT_ALLOC_FAILED,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to allocate ID segment after retries"
        );
    }

    private void ensureRow(String bizTag) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM leaf_alloc WHERE biz_tag = ?",
                Integer.class,
                bizTag
        );
        if (count != null && count > 0) {
            return;
        }
        try {
            jdbcTemplate.update(
                    "INSERT INTO leaf_alloc (biz_tag, max_id, step, version, update_time) VALUES (?, ?, ?, ?, ?)",
                    bizTag,
                    0L,
                    properties.getDefaultStep(),
                    0L,
                    Timestamp.from(Instant.now())
            );
        } catch (RuntimeException ignored) {
            // Another concurrent request may have inserted the row first.
        }
    }

    private LeafAllocRow loadRow(String bizTag) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT biz_tag, max_id, step, version FROM leaf_alloc WHERE biz_tag = ?",
                    (rs, rowNum) -> new LeafAllocRow(
                            rs.getString("biz_tag"),
                            rs.getLong("max_id"),
                            rs.getLong("step"),
                            rs.getLong("version")
                    ),
                    bizTag
            );
        } catch (EmptyResultDataAccessException e) {
            throw new ApiException(
                    ErrorCode.ID_SEGMENT_ALLOC_FAILED,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Leaf allocation row is missing for bizTag=" + bizTag
            );
        }
    }

    private record LeafAllocRow(
            String bizTag,
            long maxId,
            long step,
            long version
    ) {
    }
}
