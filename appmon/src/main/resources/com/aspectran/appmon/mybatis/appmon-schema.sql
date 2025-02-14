create table if not exists counter (
    inst varchar(80) not null,
    ymd char(8) not null,
    tm char(4) not null,
    cnt1 int not null,
    cnt2 int not null,
    constraint pk_counter primary key (inst, ymd, tm)
);
