-- Raw event count data (typically 5-minute intervals)
create table if not exists appmon_event_count (
    node_id varchar(30) not null comment 'Monitoring node identifier',
    instance_id varchar(30) not null comment 'Application instance identifier',
    event_id varchar(30) not null comment 'Event identifier',
    datetime datetime not null comment 'Data point timestamp',
    total int not null comment 'Cumulative total count (Gauge)',
    delta int not null comment 'Incremental count for the interval (Counter)',
    error int not null comment 'Incremental error count for the interval',
    constraint appmon_event_count_pk primary key (node_id, instance_id, event_id, datetime)
)
    engine = MyISAM
    charset = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    comment = 'Raw event count data';

-- Hourly aggregated event count data
create table if not exists appmon_event_count_hourly (
    node_id varchar(30) not null comment 'Monitoring node identifier',
    instance_id varchar(30) not null comment 'Application instance identifier',
    event_id varchar(30) not null comment 'Event identifier',
    datetime datetime not null comment 'Hourly truncated timestamp',
    total int not null comment 'Cumulative total count at the end of the hour',
    delta int not null comment 'Total incremental count for the hour',
    error int not null comment 'Total incremental error count for the hour',
    constraint appmon_event_count_hourly_pk primary key (node_id, instance_id, event_id, datetime)
)
    engine = MyISAM
    charset = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    comment = 'Hourly aggregated event count data';

-- Most recent event count state for incremental updates
create table if not exists appmon_event_count_last (
    node_id varchar(30) not null comment 'Monitoring node identifier',
    instance_id varchar(30) not null comment 'Application instance identifier',
    event_id varchar(30) not null comment 'Event identifier',
    datetime datetime not null comment 'Last data point timestamp',
    total int not null comment 'Last cumulative total count',
    delta int not null comment 'Last incremental count',
    error int not null comment 'Last incremental error count',
    reg_dt timestamp default now() not null comment 'Database registration timestamp',
    constraint appmon_event_count_last_pk primary key (node_id, instance_id, event_id)
)
    engine = MyISAM
    charset = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    comment = 'Most recent event count state';
