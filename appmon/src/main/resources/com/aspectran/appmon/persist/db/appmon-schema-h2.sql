-- alter table appmon_event_count
--     rename to appmon_event_count_bak;
--
-- alter table appmon_event_count_bak
--     rename constraint appmon_event_count_pk to appmon_event_count_bak_pk;

create table if not exists appmon_event_count (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime varchar(12) not null,
    total int not null,
    delta int not null,
    error int not null,
    constraint appmon_event_count_pk primary key (domain, instance, event, datetime)
);

-- insert into appmon_event_count select domain, instance, event, concat(ymd, hh, mm), total, delta, 0 from appmon_event_count_bak;
--
-- drop table appmon_event_count_bak;
--
-- alter table appmon_event_count_last
--     rename to appmon_event_count_last_bak;
--
-- alter table appmon_event_count_last_bak
--     rename constraint appmon_event_count_last_pk to appmon_event_count_last_bak_pk;

create table if not exists appmon_event_count_last (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime varchar(12) not null,
    total int not null,
    delta int not null,
    error int not null,
    reg_dt timestamp default now() not null,
    constraint appmon_event_count_last_pk primary key (domain, instance, event)
);

-- insert into appmon_event_count_last select domain, instance, event, concat(ymd, hh, mm), total, delta, 0, reg_dt from appmon_event_count_last_bak;
--
-- drop table appmon_event_count_last_bak;
