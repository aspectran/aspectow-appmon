create table if not exists event_count (
    inst varchar(30) not null,
    evt varchar(30) not null,
    ymd varchar(8) not null,
    hh varchar(2) not null,
    mm varchar(2) not null,
    total int not null,
    delta int not null,
    constraint pk_event_count primary key (inst, evt, ymd, hh, mm)
);

create table if not exists event_count_final (
    inst varchar(30) not null,
    evt varchar(30) not null,
    ymd varchar(8) not null,
    hh varchar(2) not null,
    mm varchar(2) not null,
    total int not null,
    delta int not null,
    constraint pk_event_count_final primary key (inst, evt)
);
