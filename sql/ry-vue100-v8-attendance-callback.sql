-- ============================================
-- V8 考勤管理增强：回调接口 + WebSocket + 对接文档
-- ============================================

-- ==================== 考勤配置表 ====================
DROP TABLE IF EXISTS `attendance_config`;
CREATE TABLE `attendance_config` (
    `config_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `config_key`         VARCHAR(100) NOT NULL COMMENT '配置键',
    `config_value`       VARCHAR(2000) DEFAULT '' COMMENT '配置值',
    `config_type`        VARCHAR(50)  DEFAULT 'DINGTALK' COMMENT '类型(DINGTALK/WECHAT/WEBSOCKET)',
    `description`        VARCHAR(500) DEFAULT '' COMMENT '配置说明',
    `status`             CHAR(1)      DEFAULT '0' COMMENT '状态(0=启用 1=停用)',
    `create_by`          VARCHAR(64)  DEFAULT '',
    `create_time`        DATETIME     DEFAULT NULL,
    `update_by`          VARCHAR(64)  DEFAULT '',
    `update_time`        DATETIME     DEFAULT NULL,
    `remark`             VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`config_id`),
    UNIQUE INDEX `idx_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤配置表（钉钉/企业微信回调配置）';

-- 默认配置数据
INSERT INTO `attendance_config` (`config_key`, `config_value`, `config_type`, `description`, `create_by`, `create_time`) VALUES
('dingtalk.app_key', '', 'DINGTALK', '钉钉应用AppKey', 'admin', NOW()),
('dingtalk.app_secret', '', 'DINGTALK', '钉钉应用AppSecret', 'admin', NOW()),
('dingtalk.aes_key', '', 'DINGTALK', '钉钉回调数据加密AESKey(43字符)', 'admin', NOW()),
('dingtalk.token', '', 'DINGTALK', '钉钉回调验证Token', 'admin', NOW()),
('dingtalk.callback_url', '/attendance/callback/dingtalk', 'DINGTALK', '钉钉回调地址(相对路径)', 'admin', NOW()),
('wecom.corp_id', '', 'WECHAT', '企业微信企业ID', 'admin', NOW()),
('wecom.agent_id', '', 'WECHAT', '企业微信应用AgentId', 'admin', NOW()),
('wecom.secret', '', 'WECHAT', '企业微信应用Secret', 'admin', NOW()),
('wecom.token', '', 'WECHAT', '企业微信回调验证Token', 'admin', NOW()),
('wecom.encoding_aes_key', '', 'WECHAT', '企业微信回调AESKey(43字符)', 'admin', NOW()),
('wecom.callback_url', '/attendance/callback/wecom', 'WECHAT', '企业微信回调地址(相对路径)', 'admin', NOW()),
('websocket.enabled', 'true', 'WEBSOCKET', '是否启用WebSocket实时推送', 'admin', NOW()),
('websocket.port', '8080', 'WEBSOCKET', 'WebSocket服务端口（与HTTP共用）', 'admin', NOW());

-- ==================== 考勤回调日志表 ====================
DROP TABLE IF EXISTS `attendance_callback_log`;
CREATE TABLE `attendance_callback_log` (
    `log_id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    `source`             VARCHAR(20)  NOT NULL COMMENT '来源(DINGTALK/WECHAT)',
    `callback_type`      VARCHAR(50)  NOT NULL COMMENT '回调类型(check_in/url_verify/event_subscribe等)',
    `request_body`       TEXT         COMMENT '原始请求体',
    `request_headers`    TEXT         COMMENT '请求头(JSON)',
    `process_result`     VARCHAR(20)  DEFAULT 'SUCCESS' COMMENT '处理结果(SUCCESS/FAILED)',
    `response_body`      TEXT         COMMENT '响应内容',
    `error_message`      VARCHAR(2000) DEFAULT '' COMMENT '错误信息',
    `user_name`          VARCHAR(100) DEFAULT '' COMMENT '关联用户名',
    `attendance_date`    DATE         DEFAULT NULL COMMENT '关联考勤日期',
    `ip_address`         VARCHAR(50)  DEFAULT '' COMMENT '请求来源IP',
    `cost_ms`            INT          DEFAULT 0 COMMENT '处理耗时(毫秒)',
    `create_time`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `remark`             VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`log_id`),
    INDEX `idx_source` (`source`),
    INDEX `idx_time` (`create_time`),
    INDEX `idx_user_date` (`user_name`, `attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤回调日志表';

-- ==================== 考勤记录表增强（安全ALTER，重复执行不报错） ====================
-- 通过存储过程实现 IF NOT EXISTS 语义
DROP PROCEDURE IF EXISTS `sp_add_column_if_not_exists`;

DELIMITER //
CREATE PROCEDURE `sp_add_column_if_not_exists`(
    IN table_name   VARCHAR(128),
    IN column_name  VARCHAR(128),
    IN column_def   VARCHAR(1024)
)
BEGIN
    DECLARE col_count INT DEFAULT 0;
    SELECT COUNT(*) INTO col_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = table_name
      AND COLUMN_NAME = column_name;
    IF col_count = 0 THEN
        SET @ddl = CONCAT('ALTER TABLE `', table_name, '` ADD COLUMN `', column_name, '` ', column_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END //
DELIMITER ;

-- 字段1：外部系统用户ID
CALL sp_add_column_if_not_exists('attendance_record', 'external_user_id',
    'VARCHAR(100) DEFAULT \'\' COMMENT \'外部系统用户ID(钉钉/企业微信userid)\' AFTER `user_name`');

-- 字段2：关联回调日志ID
CALL sp_add_column_if_not_exists('attendance_record', 'callback_log_id',
    'BIGINT DEFAULT NULL COMMENT \'关联回调日志ID\' AFTER `source`');

-- 字段3：签到位置
CALL sp_add_column_if_not_exists('attendance_record', 'location_result',
    'VARCHAR(200) DEFAULT \'\' COMMENT \'签到位置(WGS84/GCJ02坐标)\' AFTER `check_out_time`');

-- 字段4：签到设备信息
CALL sp_add_column_if_not_exists('attendance_record', 'device_info',
    'VARCHAR(200) DEFAULT \'\' COMMENT \'签到设备信息\' AFTER `location_result`');

DROP PROCEDURE IF EXISTS `sp_add_column_if_not_exists`;
