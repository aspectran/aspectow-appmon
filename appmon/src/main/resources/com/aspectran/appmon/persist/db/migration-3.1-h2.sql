ALTER TABLE appmon_event_count RENAME TO appmon_event_count_old;
ALTER TABLE appmon_event_count_last RENAME TO appmon_event_count_last_old;

ALTER TABLE appmon_event_count_old RENAME CONSTRAINT appmon_event_count_pk TO appmon_event_count_pk_old;
ALTER TABLE appmon_event_count_last_old RENAME CONSTRAINT appmon_event_count_last_pk TO appmon_event_count_last_pk_old;

create table if not exists appmon_event_count (
   domain varchar(30) not null,
   instance varchar(30) not null,
   event varchar(30) not null,
   datetime timestamp not null,
   total int not null,
   delta int not null,
   error int not null,
   constraint appmon_event_count_pk primary key (domain, instance, event, datetime)
);

create table if not exists appmon_event_count_last (
   domain varchar(30) not null,
   instance varchar(30) not null,
   event varchar(30) not null,
   datetime timestamp not null,
   total int not null,
   delta int not null,
   error int not null,
   reg_dt timestamp default now() not null,
   constraint appmon_event_count_last_pk primary key (domain, instance, event)
);

INSERT INTO appmon_event_count (domain, instance, event, datetime, total, delta, error)
SELECT domain, instance, event, PARSEDATETIME(datetime, 'yyyyMMddHHmm'), total, delta, error
FROM appmon_event_count_old;

INSERT INTO appmon_event_count_last (domain, instance, event, datetime, total, delta, error, reg_dt)
SELECT domain, instance, event, PARSEDATETIME(datetime, 'yyyyMMddHHmm'), total, delta, error, reg_dt
FROM appmon_event_count_last_old;

-- 집계 테이블 생성
create table if not exists appmon_event_count_hourly (
   domain varchar(30) not null,
   instance varchar(30) not null,
   event varchar(30) not null,
   datetime timestamp not null,
   total int not null,
   delta int not null,
   error int not null,
   constraint appmon_event_count_hourly_pk primary key (domain, instance, event, datetime)
);

create table if not exists appmon_event_count_daily (
   domain varchar(30) not null,
   instance varchar(30) not null,
   event varchar(30) not null,
   datetime timestamp not null,
   total int not null,
   delta int not null,
   error int not null,
   constraint appmon_event_count_daily_pk primary key (domain, instance, event, datetime)
);

-- 시간 단위 집계 데이터 생성
INSERT INTO appmon_event_count_hourly (domain, instance, event, datetime, total, delta, error)
SELECT
   domain,
   instance,
   event,
   DATE_TRUNC('HOUR', datetime),
   MAX(total),
   SUM(delta),
   SUM(error)
FROM appmon_event_count
GROUP BY domain, instance, event, DATE_TRUNC('DAY', datetime);

-- 일 단위 집계 데이터 생성
INSERT INTO appmon_event_count_daily (domain, instance, event, datetime, total, delta, error)
SELECT
   domain,
   instance,
   event,
   DATE_TRUNC('DAY', datetime),
   MAX(total),
   SUM(delta),
   SUM(error)
FROM appmon_event_count
GROUP BY domain, instance, event, DATE_TRUNC('DAY', datetime);
