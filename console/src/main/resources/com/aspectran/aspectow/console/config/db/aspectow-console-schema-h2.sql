-- H2 MariaDB mode compatible
-- Set the current schema
-- SET SCHEMA console;

-- User accounts
create table if not exists asc_user (
    user_id bigint not null auto_increment,
    username varchar(50) not null unique,
    password varchar(100) not null,
    nickname varchar(50),
    email varchar(100),
    status varchar(10) default 'NORMAL' not null, -- NORMAL, LOCKED, EXPIRED
    allowed_ips varchar(500),
    last_login_at timestamp,
    created_at timestamp default current_timestamp not null,
    updated_at timestamp default current_timestamp not null,
    primary key (user_id)
);

comment on table asc_user is 'User accounts';

-- Roles
create table if not exists asc_role (
    role_id bigint not null auto_increment,
    role_name varchar(50) not null unique,
    description varchar(200),
    primary key (role_id)
);

comment on table asc_role is 'Roles';

-- User-Role mapping
create table if not exists asc_user_role (
    user_id bigint not null,
    role_id bigint not null,
    primary key (user_id, role_id),
    foreign key (user_id) references asc_user(user_id) on delete cascade,
    foreign key (role_id) references asc_role(role_id) on delete cascade
);

comment on table asc_user_role is 'User-Role mapping';

-- Permissions
create table if not exists asc_permission (
    perm_id bigint not null auto_increment,
    perm_code varchar(50) not null unique,
    description varchar(200),
    primary key (perm_id)
);

comment on table asc_permission is 'Permissions';

-- Role-Permission mapping
create table if not exists asc_role_permission (
    role_id bigint not null,
    perm_id bigint not null,
    primary key (role_id, perm_id),
    foreign key (role_id) references asc_role(role_id) on delete cascade,
    foreign key (perm_id) references asc_permission(perm_id) on delete cascade
);

comment on table asc_role_permission is 'Role-Permission mapping';

-- Login History
create table if not exists asc_login_history (
    history_id bigint not null auto_increment,
    username varchar(50) not null,
    login_at timestamp default current_timestamp not null,
    ip_address varchar(45),
    user_agent varchar(500),
    success_yn char(1) default 'Y' not null,
    primary key (history_id)
);

comment on table asc_login_history is 'Login History';

-- Vault (Encrypted Tokens)
create table if not exists asc_vault (
    vault_id bigint not null auto_increment,
    label varchar(100) not null,
    token_type varchar(20) default 'SIMPLE' not null, -- SIMPLE, PERSISTENT, TIME_LIMITED
    encrypted_value varchar(500) not null,
    description varchar(500),
    valid_until timestamp,
    created_at timestamp default current_timestamp not null,
    updated_at timestamp default current_timestamp not null,
    primary key (vault_id)
);

comment on table asc_vault is 'Vault (Encrypted Tokens)';

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
);

comment on table asc_audit_log is 'Security Audit Log';

-- Build & Deployment Audit History Master
create table if not exists asc_build_history (
    history_id bigint not null auto_increment,
    execution_id varchar(64) not null unique,
    target_node_id varchar(100) not null,
    script_name varchar(100) not null,
    requester varchar(50) default 'SYSTEM' not null,
    status varchar(20) default 'PENDING' not null, -- PENDING, RUNNING, SUCCESS, FAILED, CANCELLED, TIMEOUT
    exit_code int,
    started_at timestamp default current_timestamp not null,
    finished_at timestamp,
    duration_ms bigint,
    git_branch varchar(100),
    git_commit_before varchar(64),
    git_commit_after varchar(64),
    git_commit_msg varchar(500),
    integrity_hash varchar(64), -- SHA-256 integrity digest for compliance audit
    error_summary varchar(1000),
    created_at timestamp default current_timestamp not null,
    primary key (history_id)
);

comment on table asc_build_history is 'Build and Deployment Audit History Master';

-- Build Console Logs (supports raw or GZIP compressed payload)
create table if not exists asc_build_log (
    log_id bigint not null auto_increment,
    history_id bigint not null,
    execution_id varchar(64) not null,
    log_content clob,
    compressed_yn char(1) default 'N' not null,
    line_count int default 0 not null,
    byte_size bigint default 0 not null,
    created_at timestamp default current_timestamp not null,
    primary key (log_id),
    foreign key (history_id) references asc_build_history(history_id) on delete cascade
);

comment on table asc_build_log is 'Build and Deployment Console Output Logs';

-- Initial data for testing
insert IGNORE into asc_role (role_name, description) values ('SUPER_ADMIN', 'Super administrator with full access');
insert IGNORE into asc_role (role_name, description) values ('ADMIN', 'Administrator with limited management access');
insert IGNORE into asc_role (role_name, description) values ('BUILDER', 'Build and deployment engineer');
insert IGNORE into asc_role (role_name, description) values ('VIEWER', 'User with read-only access');
insert IGNORE into asc_role (role_name, description) values ('DEMO', 'Demo user with simulation access');

insert IGNORE into asc_permission (perm_code, description) values ('MONITOR_VIEW', 'Access to monitoring dashboard');
insert IGNORE into asc_permission (perm_code, description) values ('MONITOR_CONTROL', 'Control monitoring settings');
insert IGNORE into asc_permission (perm_code, description) values ('USER_MANAGE', 'Manage users and roles');
insert IGNORE into asc_permission (perm_code, description) values ('NODE_MANAGE', 'Manage and restart cluster nodes');
insert IGNORE into asc_permission (perm_code, description) values ('COMMAND_EXECUTE', 'Execute remote commands');
insert IGNORE into asc_permission (perm_code, description) values ('BUILD_VIEW', 'View build and deployment status');
insert IGNORE into asc_permission (perm_code, description) values ('BUILD_EXECUTE', 'Execute build and deployment scripts');

-- Map permissions to SUPER_ADMIN
insert IGNORE into asc_role_permission (role_id, perm_id)
select r.role_id, p.perm_id from asc_role r cross join asc_permission p where r.role_name = 'SUPER_ADMIN';

-- Map permissions to ADMIN
insert IGNORE into asc_role_permission (role_id, perm_id)
select r.role_id, p.perm_id from asc_role r cross join asc_permission p
 where r.role_name = 'ADMIN' and p.perm_code in ('MONITOR_VIEW', 'COMMAND_EXECUTE', 'NODE_MANAGE', 'BUILD_VIEW', 'BUILD_EXECUTE');

-- Map permissions to BUILDER
insert IGNORE into asc_role_permission (role_id, perm_id)
select r.role_id, p.perm_id from asc_role r cross join asc_permission p
 where r.role_name = 'BUILDER' and p.perm_code in ('BUILD_VIEW', 'BUILD_EXECUTE');

-- Map permissions to VIEWER
insert IGNORE into asc_role_permission (role_id, perm_id)
select r.role_id, p.perm_id from asc_role r cross join asc_permission p
 where r.role_name = 'VIEWER' and p.perm_code in ('MONITOR_VIEW', 'BUILD_VIEW');

-- Map permissions to DEMO
insert IGNORE into asc_role_permission (role_id, perm_id)
select r.role_id, p.perm_id from asc_role r cross join asc_permission p where r.role_name = 'DEMO';

-- Initial Super Admin user (password: admin123)
insert IGNORE into asc_user (username, password, nickname, email) values ('admin', 'admin123', 'Super Admin', 'admin@aspectow.com');
insert IGNORE into asc_user_role (user_id, role_id)
select u.user_id, r.role_id from asc_user u, asc_role r where u.username = 'admin' and r.role_name = 'SUPER_ADMIN';
