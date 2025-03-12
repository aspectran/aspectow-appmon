create table if not exists appmon_event_count (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    ymd varchar(8) not null,
    hh varchar(2) not null,
    mm varchar(2) not null,
    total int not null,
    delta int not null,
    constraint pk_appmon_event_count primary key (domain, instance, event, ymd, hh, mm)
);

create table if not exists appmon_event_count_last (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    ymd varchar(8) not null,
    hh varchar(2) not null,
    mm varchar(2) not null,
    total int not null,
    delta int not null,
    upd_dt timestamp default now() not null,
    constraint pk_appmon_event_count_last primary key (domain, instance, event)
);
