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
