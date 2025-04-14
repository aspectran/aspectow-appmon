create table appmon_event_count (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime varchar(12) not null,
    total integer not null,
    delta integer not null,
    error integer not null,
    constraint appmon_event_count_pk primary key (domain, instance, event, datetime)
);

create table appmon_event_count_last (
    domain varchar(30) not null,
    instance varchar(30) not null,
    event varchar(30) not null,
    datetime varchar(12) not null,
    total integer not null,
    delta integer not null,
    error integer not null,
    reg_dt date default sysdate not null,
    constraint appmon_event_count_last_pk primary key (domain, instance, event)
);
