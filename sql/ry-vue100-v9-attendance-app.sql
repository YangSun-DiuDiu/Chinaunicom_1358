-- ============================================
-- V9 考勤管理增强：应用条目管理
-- ============================================

-- ==================== 考勤应用条目表 ====================
DROP TABLE IF EXISTS `attendance_app`;
CREATE TABLE `attendance_app` (
    `app_id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '应用ID',
    `app_name`           VARCHAR(100) NOT NULL COMMENT '应用名称',
    `platform`           VARCHAR(20)  NOT NULL COMMENT '平台(DINGTALK/WECHAT)',
    `app_key`            VARCHAR(100) DEFAULT '' COMMENT 'AppKey/CorpId',
    `app_secret`         VARCHAR(100) DEFAULT '' COMMENT 'AppSecret',
    `agent_id`           VARCHAR(50)  DEFAULT '' COMMENT 'AgentId(企业微信)',
    `token`              VARCHAR(100) NOT NULL COMMENT '回调验证Token',
    `aes_key`            VARCHAR(100) NOT NULL COMMENT '回调加密AESKey(43位Base64)',
    `callback_url`       VARCHAR(500) NOT NULL COMMENT '回调地址(完整URL)',
    `description`        VARCHAR(500) DEFAULT '' COMMENT '描述',
    `status`             VARCHAR(20)  DEFAULT 'UNKNOWN' COMMENT '连接状态(ONLINE/UNKNOWN/OFFLINE/ERROR)',
    `last_test_time`     DATETIME     DEFAULT NULL COMMENT '最近测试时间',
    `last_test_result`   VARCHAR(200) DEFAULT '' COMMENT '最近测试结果',
    `last_callback_time` DATETIME     DEFAULT NULL COMMENT '最近收到回调时间',
    `total_callbacks`    INT          DEFAULT 0 COMMENT '累计回调次数',
    `create_by`          VARCHAR(64)  DEFAULT '',
    `create_time`        DATETIME     DEFAULT NULL,
    `update_by`          VARCHAR(64)  DEFAULT '',
    `update_time`        DATETIME     DEFAULT NULL,
    `remark`             VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`app_id`),
    INDEX `idx_platform` (`platform`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤应用条目表（钉钉/企业微信应用管理）';

-- 插入一条示例数据
INSERT INTO `attendance_app` (`app_name`, `platform`, `token`, `aes_key`, `callback_url`, `description`, `status`, `create_by`, `create_time`) VALUES
('示例钉钉应用', 'DINGTALK', 'test_token_ruoyi', 'test_aes_key_ruoyi1234567890123456789', '/attendance/callback/dingtalk', '用于对接钉钉考勤回调的示例应用', 'UNKNOWN', 'admin', NOW());
