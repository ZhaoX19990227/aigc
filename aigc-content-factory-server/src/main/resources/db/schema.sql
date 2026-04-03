CREATE TABLE IF NOT EXISTS hotspot_record (
    id BIGINT PRIMARY KEY,
    source VARCHAR(32) NOT NULL,
    source_topic_id VARCHAR(64),
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(1000),
    score DECIMAL(10, 2),
    captured_at TIMESTAMP,
    raw_payload CLOB,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS content_task (
    id BIGINT PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_step VARCHAR(64),
    source_type VARCHAR(32),
    selected_topic VARCHAR(255),
    platforms_json VARCHAR(500),
    review_status VARCHAR(32),
    publish_status VARCHAR(32),
    error_message VARCHAR(1000),
    retry_count INT,
    manual_review_required BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS topic_candidate (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    reason VARCHAR(1000),
    target_audience VARCHAR(255),
    suggested_platforms VARCHAR(500),
    risk_flags VARCHAR(500),
    priority INT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS content_script (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    intro_hook VARCHAR(1000),
    segments_json CLOB,
    closing_cta VARCHAR(1000),
    tags_json VARCHAR(1000),
    estimated_duration_sec INT,
    voice_tone VARCHAR(64),
    image_prompt VARCHAR(1000),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS media_asset (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    asset_type VARCHAR(32) NOT NULL,
    file_url VARCHAR(1000) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    mime_type VARCHAR(128),
    duration_sec INT,
    file_size BIGINT,
    generation_params VARCHAR(1000),
    status VARCHAR(32),
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS publish_record (
    id BIGINT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    platform VARCHAR(32) NOT NULL,
    account_name VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    platform_content_id VARCHAR(128),
    response_message VARCHAR(1000),
    published_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
