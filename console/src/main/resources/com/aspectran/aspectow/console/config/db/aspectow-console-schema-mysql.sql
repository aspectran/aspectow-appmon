-----------------------
-- For Aspectow Appmon
-----------------------

-- Raw event count data (typically 5-minute intervals)
create table if not exists appmon_event_count (
    node_id varchar(30) not null comment 'Monitoring node identifier',
    group_id varchar(30) not null comment 'Monitoring node group identifier',
    app_id varchar(30) not null comment 'Application identifier',
    event_id varchar(30) not null comment 'Event identifier',
    datetime datetime not null comment 'Data point timestamp',
    total int not null comment 'Cumulative total count (Gauge)',
    delta int not null comment 'Incremental count for the interval (Counter)',
    error int not null comment 'Incremental error count for the interval',
    constraint appmon_event_count_pk primary key (node_id, app_id, event_id, datetime)
)
    engine = MyISAM
    charset = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    comment = 'Raw event count data';

create index appmon_event_count_ix_group on appmon_event_count (group_id, app_id, event_id, datetime);

-- Hourly aggregated event count data
create table if not exists appmon_event_count_hourly (
    node_id varchar(30) not null comment 'Monitoring node identifier',
    group_id varchar(30) not null comment 'Monitoring node group identifier',
    app_id varchar(30) not null comment 'Application identifier',
    event_id varchar(30) not null comment 'Event identifier',
    datetime datetime not null comment 'Hourly truncated timestamp',
    total int not null comment 'Cumulative total count at the end of the hour',
    delta int not null comment 'Total incremental count for the hour',
    error int not null comment 'Total incremental error count for the hour',
    constraint appmon_event_count_hourly_pk primary key (node_id, app_id, event_id, datetime)
)
    engine = MyISAM
    charset = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    comment = 'Hourly aggregated event count data';

create index appmon_event_count_hourly_ix_group on appmon_event_count_hourly (group_id, app_id, event_id, datetime);

-- Most recent event count state for incremental updates
create table if not exists appmon_event_count_last (
    node_id varchar(30) not null comment 'Monitoring node identifier',
    group_id varchar(30) not null comment 'Monitoring node group identifier',
    app_id varchar(30) not null comment 'Application identifier',
    event_id varchar(30) not null comment 'Event identifier',
    datetime datetime not null comment 'Last data point timestamp',
    total int not null comment 'Last cumulative total count',
    delta int not null comment 'Last incremental count',
    error int not null comment 'Last incremental error count',
    reg_dt timestamp default now() not null comment 'Database registration timestamp',
    constraint appmon_event_count_last_pk primary key (node_id, app_id, event_id)
)
    engine = MyISAM
    charset = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    comment = 'Most recent event count state';

create index appmon_event_count_last_ix_group on appmon_event_count_last (group_id, app_id, event_id);

------------------------
-- For Aspectow Console
------------------------

-- User accounts
create table if not exists asc_user (
    user_id bigint not null auto_increment,
    username varchar(50) not null unique,
    password varchar(100) not null,
    nickname varchar(50),
    email varchar(100),
    status varchar(10) default 'NORMAL' not null, -- NORMAL, LOCKED, EXPIRED
    allowed_ips varchar(500),
    last_login_at timestamp null,
    created_at timestamp default current_timestamp not null,
    updated_at timestamp default current_timestamp not null on update current_timestamp,
    primary key (user_id)
) comment = 'User accounts';

-- Roles
create table if not exists asc_role (
    role_id bigint not null auto_increment,
    role_name varchar(50) not null unique,
    description varchar(200),
    primary key (role_id)
) comment = 'Roles';

-- User-Role mapping
create table if not exists asc_user_role (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    foreign key (user_id) references asc_user(user_id) on delete cascade,
    foreign key (role_id) references asc_role(role_id) on delete cascade
) comment = 'User-Role mapping';

-- Permissions
create table if not exists asc_permission (
    perm_id bigint not null auto_increment,
    perm_code varchar(50) not null unique,
    description varchar(200),
    primary key (perm_id)
) comment = 'Permissions';

-- Role-Permission mapping
create table if not exists asc_role_permission (
    role_id bigint not null,
    perm_id bigint not null,
    primary key (role_id, perm_id),
    foreign key (role_id) references asc_role(role_id) on delete cascade,
    foreign key (perm_id) references asc_permission(perm_id) on delete cascade
) comment = 'Role-Permission mapping';

-- Login History
create table if not exists asc_login_history (
    history_id bigint not null auto_increment,
    username varchar(50) not null,
    login_at timestamp default current_timestamp not null,
    ip_address varchar(45),
    user_agent varchar(500),
    success_yn char(1) default 'Y' not null,
    primary key (history_id)
) comment = 'Login History';

-- Vault (Encrypted Tokens)
create table if not exists asc_vault (
    vault_id bigint not null auto_increment,
    label varchar(100) not null,
    token_type varchar(20) default 'SIMPLE' not null, -- SIMPLE, PERSISTENT, TIME_LIMITED
    encrypted_value varchar(500) not null,
    description varchar(500),
    valid_until timestamp null,
    created_at timestamp default current_timestamp not null,
    updated_at timestamp default current_timestamp not null,
    primary key (vault_id)
) comment = 'Vault (Encrypted Tokens)';

-- Security Audit Log
create table if not exists asc_audit_log (
    audit_id bigint not null auto_increment,
    username varchar(50),
    event_type varchar(50) not null,
    target varchar(100),
    details varchar(1000),
    ip_address varchar(45),
    created_at timestamp default current_timestamp not null,
    primary key (audit_id)
) comment = 'Security Audit Log';

-- Initial data
insert ignore into asc_role (role_name, description) values ('SUPER_ADMIN', 'Super administrator with full access');
insert ignore into asc_role (role_name, description) values ('ADMIN', 'Administrator with limited management access');
insert ignore into asc_role (role_name, description) values ('VIEWER', 'User with read-only access');
insert ignore into asc_role (role_name, description) values ('DEMO', 'Demo user with simulation access');

insert ignore into asc_permission (perm_code, description) values ('MONITOR_VIEW', 'Access to monitoring dashboard');
insert ignore into asc_permission (perm_code, description) values ('MONITOR_CONTROL', 'Control monitoring settings');
insert ignore into asc_permission (perm_code, description) values ('USER_MANAGE', 'Manage users and roles');
insert ignore into asc_permission (perm_code, description) values ('NODE_MANAGE', 'Manage and restart cluster nodes');
insert ignore into asc_permission (perm_code, description) values ('COMMAND_EXECUTE', 'Execute remote commands');

-- Map permissions to SUPER_ADMIN
insert ignore into asc_role_permission (role_id, perm_id) select 1, perm_id from asc_permission;
-- Map permissions to ADMIN
insert ignore into asc_role_permission (role_id, perm_id) select 2, perm_id from asc_permission where perm_code in ('MONITOR_VIEW', 'COMMAND_EXECUTE', 'NODE_MANAGE');
-- Map permissions to VIEWER
insert ignore into asc_role_permission (role_id, perm_id) select 3, perm_id from asc_permission where perm_code = 'MONITOR_VIEW';
-- Map permissions to DEMO
insert ignore into asc_role_permission (role_id, perm_id) select 4, perm_id from asc_permission;

-- Initial Super Admin user (password: admin123)
insert ignore into asc_user (username, password, nickname, email) values ('admin', 'admin123', 'Super Admin', 'admin@aspectow.com');
insert ignore into asc_user_role (user_id, role_id) values (1, 1);
