-- 1. 기존 테이블 이름 변경 (백업용)
RENAME TABLE appmon_event_count TO appmon_event_count_old;
RENAME TABLE appmon_event_count_last TO appmon_event_count_last_old;

-- 2. 새 테이블 생성 (네이티브 DATETIME 타입 반영)
CREATE TABLE appmon_event_count (
   domain varchar(30) not null,
   instance varchar(30) not null,
   event varchar(30) not null,
   datetime datetime not null,
   total int not null,
   delta int not null,
   error int not null,
   CONSTRAINT appmon_event_count_pk PRIMARY KEY (domain, instance, event, datetime)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE appmon_event_count_last (
   domain varchar(30) not null,
   instance varchar(30) not null,
   event varchar(30) not null,
   datetime datetime not null,
   total int not null,
   delta int not null,
   error int not null,
   reg_dt timestamp default current_timestamp() not null,
   CONSTRAINT appmon_event_count_last_pk PRIMARY KEY (domain, instance, event)
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 데이터 마이그레이션 (STR_TO_DATE 사용)
-- 'yyyyMMddHHmm' 형식을 파싱하기 위해 '%Y%m%d%H%i' 포맷 마스크를 사용합니다.
INSERT INTO appmon_event_count (domain, instance, event, datetime, total, delta, error)
SELECT
   domain,
   instance,
   event,
   STR_TO_DATE(datetime, '%Y%m%d%H%i'),
   total,
   delta,
   error
FROM appmon_event_count_old;

INSERT INTO appmon_event_count_last (domain, instance, event, datetime, total, delta, error, reg_dt)
SELECT
   domain,
   instance,
   event,
   STR_TO_DATE(datetime, '%Y%m%d%H%i'),
   total,
   delta,
   error,
   reg_dt
FROM appmon_event_count_last_old;

-- 4. 데이터 검증
-- 데이터 건수가 백업 테이블과 일치하는지 확인하세요.
SELECT COUNT(*) AS new_count FROM appmon_event_count;
SELECT COUNT(*) AS old_count FROM appmon_event_count_old;