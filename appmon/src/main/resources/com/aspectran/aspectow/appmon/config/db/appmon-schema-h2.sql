-- Raw event count data (typically 5-minute intervals)
create table if not exists appmon_event_count (
    node_id varchar(30) not null,
    app_id varchar(30) not null,
    event_id varchar(30) not null,
    datetime timestamp not null,
    total int not null,
    delta int not null,
    error int not null,
    constraint appmon_event_count_pk primary key (node_id, app_id, event_id, datetime)
);

comment on table appmon_event_count is 'Raw event count data';
comment on column appmon_event_count.node_id is 'Monitoring node identifier';
comment on column appmon_event_count.app_id is 'Application identifier';
comment on column appmon_event_count.event_id is 'Event identifier';
comment on column appmon_event_count.datetime is 'Data point timestamp';
comment on column appmon_event_count.total is 'Cumulative total count (Gauge)';
comment on column appmon_event_count.delta is 'Incremental count for the interval (Counter)';
comment on column appmon_event_count.error is 'Incremental error count for the interval';

-- Hourly aggregated event count data
create table if not exists appmon_event_count_hourly (
    node_id varchar(30) not null,
    app_id varchar(30) not null,
    event_id varchar(30) not null,
    datetime timestamp not null,
    total int not null,
    delta int not null,
    error int not null,
    constraint appmon_event_count_hourly_pk primary key (node_id, app_id, event_id, datetime)
);

comment on table appmon_event_count_hourly is 'Hourly aggregated event count data';
comment on column appmon_event_count_hourly.node_id is 'Monitoring node identifier';
comment on column appmon_event_count_hourly.app_id is 'Application identifier';
comment on column appmon_event_count_hourly.event_id is 'Event identifier';
comment on column appmon_event_count_hourly.datetime is 'Hourly truncated timestamp';
comment on column appmon_event_count_hourly.total is 'Cumulative total count at the end of the hour';
comment on column appmon_event_count_hourly.delta is 'Total incremental count for the hour';
comment on column appmon_event_count_hourly.error is 'Total incremental error count for the hour';

-- Most recent event count state for incremental updates
create table if not exists appmon_event_count_last (
    node_id varchar(30) not null,
    app_id varchar(30) not null,
    event_id varchar(30) not null,
    datetime timestamp not null,
    total int not null,
    delta int not null,
    error int not null,
    reg_dt timestamp default now() not null,
    constraint appmon_event_count_last_pk primary key (node_id, app_id, event_id)
);

comment on table appmon_event_count_last is 'Most recent event count state';
comment on column appmon_event_count_last.node_id is 'Monitoring node identifier';
comment on column appmon_event_count_last.app_id is 'Application identifier';
comment on column appmon_event_count_last.event_id is 'Event identifier';
comment on column appmon_event_count_last.datetime is 'Last data point timestamp';
comment on column appmon_event_count_last.total is 'Last cumulative total count';
comment on column appmon_event_count_last.delta is 'Last incremental count';
comment on column appmon_event_count_last.error is 'Last incremental error count';
comment on column appmon_event_count_last.reg_dt is 'Database registration timestamp';
