-- Raw event count data (typically 5-minute intervals)
CREATE TABLE IF NOT EXISTS appmon_event_count (
    node_id varchar(30) NOT NULL,
    app_id varchar(30) NOT NULL,
    event_id varchar(30) NOT NULL,
    datetime timestamp NOT NULL,
    total bigint NOT NULL,
    delta bigint NOT NULL,
    error bigint NOT NULL,
    CONSTRAINT appmon_event_count_pk PRIMARY KEY (node_id, app_id, event_id, datetime)
);

COMMENT ON TABLE appmon_event_count IS 'Raw event count data';
COMMENT ON COLUMN appmon_event_count.node_id IS 'Monitoring node identifier';
COMMENT ON COLUMN appmon_event_count.app_id IS 'Application identifier';
COMMENT ON COLUMN appmon_event_count.event_id IS 'Event identifier';
COMMENT ON COLUMN appmon_event_count.datetime IS 'Data point timestamp';
COMMENT ON COLUMN appmon_event_count.total IS 'Cumulative total count (Gauge)';
COMMENT ON COLUMN appmon_event_count.delta IS 'Incremental count for the interval (Counter)';
COMMENT ON COLUMN appmon_event_count.error IS 'Incremental error count for the interval';

-- Hourly aggregated event count data
CREATE TABLE IF NOT EXISTS appmon_event_count_hourly (
    node_id varchar(30) NOT NULL,
    app_id varchar(30) NOT NULL,
    event_id varchar(30) NOT NULL,
    datetime timestamp NOT NULL,
    total bigint NOT NULL,
    delta bigint NOT NULL,
    error bigint NOT NULL,
    CONSTRAINT appmon_event_count_hourly_pk PRIMARY KEY (node_id, app_id, event_id, datetime)
);

COMMENT ON TABLE appmon_event_count_hourly IS 'Hourly aggregated event count data';
COMMENT ON COLUMN appmon_event_count_hourly.node_id IS 'Monitoring node identifier';
COMMENT ON COLUMN appmon_event_count_hourly.app_id IS 'Application identifier';
COMMENT ON COLUMN appmon_event_count_hourly.event_id IS 'Event identifier';
COMMENT ON COLUMN appmon_event_count_hourly.datetime IS 'Hourly truncated timestamp';
COMMENT ON COLUMN appmon_event_count_hourly.total IS 'Cumulative total count at the end of the hour';
COMMENT ON COLUMN appmon_event_count_hourly.delta IS 'Total incremental count for the hour';
COMMENT ON COLUMN appmon_event_count_hourly.error IS 'Total incremental error count for the hour';

-- Most recent event count state for incremental updates
CREATE TABLE IF NOT EXISTS appmon_event_count_last (
    node_id varchar(30) NOT NULL,
    app_id varchar(30) NOT NULL,
    event_id varchar(30) NOT NULL,
    datetime timestamp NOT NULL,
    total bigint NOT NULL,
    delta bigint NOT NULL,
    error bigint NOT NULL,
    reg_dt timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT appmon_event_count_last_pk PRIMARY KEY (node_id, app_id, event_id)
);

COMMENT ON TABLE appmon_event_count_last IS 'Most recent event count state';
COMMENT ON COLUMN appmon_event_count_last.node_id IS 'Monitoring node identifier';
COMMENT ON COLUMN appmon_event_count_last.app_id IS 'Application identifier';
COMMENT ON COLUMN appmon_event_count_last.event_id IS 'Event identifier';
COMMENT ON COLUMN appmon_event_count_last.datetime IS 'Last data point timestamp';
COMMENT ON COLUMN appmon_event_count_last.total IS 'Last cumulative total count';
COMMENT ON COLUMN appmon_event_count_last.delta IS 'Last incremental count';
COMMENT ON COLUMN appmon_event_count_last.error IS 'Last incremental error count';
COMMENT ON COLUMN appmon_event_count_last.reg_dt IS 'Database registration timestamp';
