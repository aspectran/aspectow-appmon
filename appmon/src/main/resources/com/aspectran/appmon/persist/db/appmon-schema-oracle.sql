-- Raw event count data (typically 5-minute intervals)
create table appmon_event_count (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime date not null,
    total integer not null,
    delta integer not null,
    error integer not null,
    constraint appmon_event_count_pk primary key (domain, instance, event, datetime)
);

comment on table appmon_event_count is 'Raw event count data';
comment on column appmon_event_count.domain is 'Monitoring domain name';
comment on column appmon_event_count.instance is 'Application instance name';
comment on column appmon_event_count.event is 'Event name';
comment on column appmon_event_count.datetime is 'Data point timestamp';
comment on column appmon_event_count.total is 'Cumulative total count (Gauge)';
comment on column appmon_event_count.delta is 'Incremental count for the interval (Counter)';
comment on column appmon_event_count.error is 'Incremental error count for the interval';

-- Hourly aggregated event count data
create table appmon_event_count_hourly (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime date not null,
    total integer not null,
    delta integer not null,
    error integer not null,
    constraint appmon_event_count_hourly_pk primary key (domain, instance, event, datetime)
);

comment on table appmon_event_count_hourly is 'Hourly aggregated event count data';
comment on column appmon_event_count_hourly.datetime is 'Hourly truncated timestamp';
comment on column appmon_event_count_hourly.total is 'Cumulative total count at the end of the hour';
comment on column appmon_event_count_hourly.delta is 'Total incremental count for the hour';
comment on column appmon_event_count_hourly.error is 'Total incremental error count for the hour';

-- Daily aggregated event count data
create table appmon_event_count_daily (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime date not null,
    total integer not null,
    delta integer not null,
    error integer not null,
    constraint appmon_event_count_daily_pk primary key (domain, instance, event, datetime)
);

comment on table appmon_event_count_daily is 'Daily aggregated event count data';
comment on column appmon_event_count_daily.datetime is 'Daily truncated timestamp';
comment on column appmon_event_count_daily.total is 'Cumulative total count at the end of the day';
comment on column appmon_event_count_daily.delta is 'Total incremental count for the day';
comment on column appmon_event_count_daily.error is 'Total incremental error count for the day';

-- Most recent event count state for incremental updates
create table appmon_event_count_last (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime date not null,
    total integer not null,
    delta integer not null,
    error integer not null,
    reg_dt date default sysdate not null,
    constraint appmon_event_count_last_pk primary key (domain, instance, event)
);

comment on table appmon_event_count_last is 'Most recent event count state';
comment on column appmon_event_count_last.reg_dt is 'Database registration timestamp';
