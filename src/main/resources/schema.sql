CREATE TABLE IF NOT EXISTS leaf_alloc (
    biz_tag VARCHAR(128) PRIMARY KEY,
    max_id BIGINT NOT NULL,
    step BIGINT NOT NULL,
    version BIGINT NOT NULL,
    update_time TIMESTAMP NOT NULL
);
