create table if not exists counter (
    inst varchar(30) not null,
    evt varchar(30) not null,
    ymd varchar(8) not null,
    hh varchar(2) not null,
    mm varchar(2) not null,
    cnt1 int not null,
    cnt2 int not null,
    constraint pk_counter primary key (inst, evt, ymd, hh, mm)
);
