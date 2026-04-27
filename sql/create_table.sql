-- AI Tourism MVP Demo schema
-- MySQL 8+

CREATE DATABASE IF NOT EXISTS ai_tourism_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE ai_tourism_demo;

CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL COMMENT '业务用户ID',
    phone VARCHAR(20) NULL,
    password_hash VARCHAR(200) NULL,
    nickname VARCHAR(64) NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=正常,0=禁用',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_userid UNIQUE (user_id),
    CONSTRAINT uk_user_phone UNIQUE (phone)
) COMMENT='用户表';

CREATE TABLE IF NOT EXISTS t_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_role_code UNIQUE (role_code)
) COMMENT='角色表';

CREATE TABLE IF NOT EXISTS t_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    perm_code VARCHAR(128) NOT NULL,
    perm_name VARCHAR(128) NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_perm_code UNIQUE (perm_code)
) COMMENT='权限表';

CREATE TABLE IF NOT EXISTS t_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_code),
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES t_user (user_id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_code) REFERENCES t_role (role_code)
) COMMENT='用户-角色关联表';

CREATE TABLE IF NOT EXISTS t_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL,
    perm_code VARCHAR(128) NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_role_perm UNIQUE (role_code, perm_code),
    CONSTRAINT fk_rp_role FOREIGN KEY (role_code) REFERENCES t_role (role_code),
    CONSTRAINT fk_rp_perm FOREIGN KEY (perm_code) REFERENCES t_permission (perm_code)
) COMMENT='角色-权限关联表';

CREATE TABLE IF NOT EXISTS t_refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    refresh_token VARCHAR(255) NOT NULL,
    expire_at DATETIME NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_token UNIQUE (refresh_token),
    INDEX idx_refresh_user_id (user_id),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) COMMENT='刷新令牌表';

CREATE TABLE IF NOT EXISTS t_ai_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(64) NOT NULL COMMENT '业务用户ID',
    title VARCHAR(255) NOT NULL COMMENT '会话标题',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=有效,0=关闭',
    last_message_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_session_id UNIQUE (session_id),
    INDEX idx_ai_session_user_time (user_id, last_message_time DESC),
    CONSTRAINT fk_ai_session_user FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) COMMENT='AI会话表';

CREATE TABLE IF NOT EXISTS t_ai_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_id VARCHAR(64) NOT NULL COMMENT '消息ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    user_id VARCHAR(64) NOT NULL COMMENT '业务用户ID',
    role VARCHAR(32) NOT NULL COMMENT 'user/assistant/system/tool',
    content MEDIUMTEXT NOT NULL COMMENT '消息内容',
    token_input INT NULL,
    token_output INT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_msg_id UNIQUE (msg_id),
    INDEX idx_ai_msg_session_time (session_id, created_time),
    INDEX idx_ai_msg_user_time (user_id, created_time),
    CONSTRAINT fk_ai_msg_session FOREIGN KEY (session_id) REFERENCES t_ai_session (session_id),
    CONSTRAINT fk_ai_msg_user FOREIGN KEY (user_id) REFERENCES t_user (user_id)
) COMMENT='AI消息表';

CREATE TABLE IF NOT EXISTS t_poi (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    poi_name VARCHAR(255) NOT NULL,
    city_name VARCHAR(255) NOT NULL,
    poi_description TEXT NULL,
    poi_longitude DECIMAL(10, 6) NULL,
    poi_latitude DECIMAL(10, 6) NULL,
    poi_rank_in_city INT NULL,
    poi_rank_in_china INT NULL,
    source VARCHAR(64) NOT NULL DEFAULT 'amap',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_poi_city_rank (city_name, poi_rank_in_city),
    INDEX idx_poi_city_name (city_name, poi_name)
) COMMENT='景点信息表';

-- seed: role and permission
INSERT INTO t_role(role_code, role_name) VALUES
('USER', '普通用户'),
('ROOT', '超级管理员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO t_permission(perm_code, perm_name) VALUES
('ai:chat', '发送聊天消息'),
('ai:history', '查询会话历史'),
('ai:session', '查询会话列表'),
('user:set-root', '设置ROOT角色'),
('user:disable', '禁用用户')
ON DUPLICATE KEY UPDATE perm_name = VALUES(perm_name);

INSERT INTO t_role_permission(role_code, perm_code) VALUES
('USER', 'ai:chat'),
('USER', 'ai:history'),
('USER', 'ai:session')
ON DUPLICATE KEY UPDATE perm_code = VALUES(perm_code);

INSERT INTO t_role_permission(role_code, perm_code)
SELECT 'ROOT', p.perm_code FROM t_permission p
ON DUPLICATE KEY UPDATE perm_code = VALUES(perm_code);

-- seed: demo user
INSERT INTO t_user(user_id, phone, nickname, status)
VALUES ('demo-user', '13800000000', '演示用户', 1)
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname), status = VALUES(status);

INSERT INTO t_user_role(user_id, role_code)
VALUES ('demo-user', 'USER')
ON DUPLICATE KEY UPDATE role_code = VALUES(role_code);
