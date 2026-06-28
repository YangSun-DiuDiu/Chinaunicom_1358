-- ============================================================
-- 多厂商短信中台 — 数据库迁移脚本 (Task 1)
-- 操作: DROP旧表 + 创建五表 + 预置数据 + 清理旧配置/旧菜单 + 新增菜单
-- 目标库: ry101
-- ============================================================
USE ry101;

-- ============================================================
-- 1. 删除旧表
-- ============================================================
DROP TABLE IF EXISTS `sys_sms_log`;

-- ============================================================
-- 2. 创建五张新表
-- ============================================================

-- 2.1 短信渠道表 sys_sms_channel
DROP TABLE IF EXISTS `sys_sms_channel`;
CREATE TABLE `sys_sms_channel` (
  `channel_id`        bigint          NOT NULL AUTO_INCREMENT COMMENT '渠道主键ID',
  `channel_name`      varchar(100)    NOT NULL                COMMENT '渠道名称',
  `channel_type`      varchar(30)     NOT NULL                COMMENT '渠道类型: ALIYUN/TENCENT/API',
  `access_key_id`     varchar(200)    DEFAULT ''              COMMENT '密钥1(AccessKey/AppId)',
  `access_key_secret` varchar(500)    DEFAULT ''              COMMENT '密钥2(加密存储)',
  `endpoint`          varchar(300)    DEFAULT ''              COMMENT '网关/接口地址',
  `api_method`        varchar(10)     DEFAULT 'POST'          COMMENT 'API渠道专用: GET/POST',
  `api_headers`       varchar(2000)   DEFAULT ''              COMMENT 'API渠道专用: 自定义请求头JSON',
  `api_body_template` varchar(2000)   DEFAULT ''              COMMENT 'API渠道专用: 请求体模板',
  `api_success_rule`  varchar(500)    DEFAULT ''              COMMENT 'API渠道专用: 成功判定规则',
  `timeout`           int             DEFAULT 5000            COMMENT '请求超时时间(毫秒)',
  `retry_count`       int             DEFAULT 0               COMMENT '渠道失败重试次数',
  `status`            char(1)         DEFAULT '0'             COMMENT '状态(0启用 1禁用)',
  `remark`            varchar(500)    DEFAULT ''              COMMENT '备注',
  `create_by`         varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time`       datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time`       datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`          char(1)         DEFAULT '0'             COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`channel_id`),
  KEY `idx_channel_type` (`channel_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='短信渠道配置表';

-- 2.2 短信签名模板表 sys_sms_sign_template
DROP TABLE IF EXISTS `sys_sms_sign_template`;
CREATE TABLE `sys_sms_sign_template` (
  `st_id`                   bigint          NOT NULL AUTO_INCREMENT COMMENT '签名模板主键ID',
  `channel_id`              bigint          NOT NULL                COMMENT '关联渠道ID',
  `sign_name`               varchar(100)    NOT NULL                COMMENT '短信签名',
  `template_code`           varchar(100)    NOT NULL                COMMENT '模板CODE/模板ID',
  `template_param_mapping`  varchar(1000)   DEFAULT ''              COMMENT '第三方API参数映射规则JSON',
  `template_desc`           varchar(500)    DEFAULT ''              COMMENT '模板用途描述',
  `status`                  char(1)         DEFAULT '0'             COMMENT '状态(0启用 1禁用)',
  `create_by`               varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time`             datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`               varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time`             datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`                char(1)         DEFAULT '0'             COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`st_id`),
  KEY `idx_channel_id` (`channel_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_st_channel` FOREIGN KEY (`channel_id`) REFERENCES `sys_sms_channel` (`channel_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='短信签名模板表';

-- 2.3 业务短信绑定表 sys_sms_biz
DROP TABLE IF EXISTS `sys_sms_biz`;
CREATE TABLE `sys_sms_biz` (
  `biz_id`            bigint          NOT NULL AUTO_INCREMENT COMMENT '业务绑定主键ID',
  `biz_code`          varchar(50)     NOT NULL                COMMENT '全局唯一业务编码(调用唯一标识)',
  `biz_name`          varchar(100)    NOT NULL                COMMENT '业务名称',
  `st_id`             bigint          NOT NULL                COMMENT '关联签名模板ID',
  `backup_channel_id` bigint          DEFAULT NULL            COMMENT '备用渠道ID(主渠道故障自动切换)',
  `minute_limit`      int             DEFAULT 5               COMMENT '每分钟发送上限',
  `day_limit`         int             DEFAULT 100             COMMENT '每日发送上限',
  `status`            char(1)         DEFAULT '0'             COMMENT '状态(0启用 1禁用)',
  `remark`            varchar(500)    DEFAULT ''              COMMENT '备注',
  `create_by`         varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time`       datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`         varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time`       datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`          char(1)         DEFAULT '0'             COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`biz_id`),
  UNIQUE KEY `uk_biz_code` (`biz_code`),
  KEY `idx_st_id` (`st_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_biz_st` FOREIGN KEY (`st_id`) REFERENCES `sys_sms_sign_template` (`st_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='业务短信绑定表';

-- 2.4 短信黑名单表 sys_sms_blacklist
DROP TABLE IF EXISTS `sys_sms_blacklist`;
CREATE TABLE `sys_sms_blacklist` (
  `phone`       varchar(20)     NOT NULL                COMMENT '黑名单手机号',
  `reason`      varchar(500)    DEFAULT ''              COMMENT '拉黑原因',
  `status`      char(1)         DEFAULT '0'             COMMENT '状态(0启用 1禁用)',
  `create_by`   varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time` datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`   varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time` datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `del_flag`    char(1)         DEFAULT '0'             COMMENT '删除标志(0正常 1删除)',
  PRIMARY KEY (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='短信黑名单表';

-- 2.5 短信发送日志表 sys_sms_log (重建)
DROP TABLE IF EXISTS `sys_sms_log`;
CREATE TABLE `sys_sms_log` (
  `log_id`        bigint          NOT NULL AUTO_INCREMENT COMMENT '日志主键ID',
  `biz_code`      varchar(50)     NOT NULL                COMMENT '业务编码',
  `channel_type`  varchar(30)     NOT NULL                COMMENT '渠道类型(ALIYUN/TENCENT/API)',
  `phone`         varchar(20)     NOT NULL                COMMENT '手机号码',
  `template_code` varchar(100)    DEFAULT ''              COMMENT '模板ID',
  `sign_name`     varchar(100)    DEFAULT ''              COMMENT '签名',
  `param`         varchar(2000)   DEFAULT ''              COMMENT '模板参数JSON',
  `send_mode`     tinyint         DEFAULT 1               COMMENT '发送模式(1即时 2定时)',
  `send_time`     datetime        DEFAULT NULL            COMMENT '定时发送时间(即时为null)',
  `task_status`   varchar(20)     DEFAULT 'WAIT'          COMMENT '任务状态: WAIT待执行/SUCCESS发送成功/FAIL发送失败/CANCEL已撤销',
  `retry_count`   int             DEFAULT 0               COMMENT '已重试次数',
  `result_msg`    varchar(1000)   DEFAULT ''              COMMENT '返回结果/错误信息',
  `create_by`     varchar(64)     DEFAULT ''              COMMENT '创建者',
  `create_time`   datetime        DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by`     varchar(64)     DEFAULT ''              COMMENT '更新者',
  `update_time`   datetime        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark`        varchar(500)    DEFAULT ''              COMMENT '备注',
  PRIMARY KEY (`log_id`),
  KEY `idx_biz_code` (`biz_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_send_mode` (`send_mode`),
  KEY `idx_task_status` (`task_status`),
  KEY `idx_send_time` (`send_time`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='短信发送日志表(合规审计)';

-- ============================================================
-- 3. 删除旧短信配置
-- ============================================================
DELETE FROM `sys_config` WHERE `config_key` LIKE 'sms.%';

-- ============================================================
-- 4. 预置一条阿里云渠道记录
-- ============================================================
INSERT INTO `sys_sms_channel` (`channel_id`, `channel_name`, `channel_type`, `access_key_id`, `access_key_secret`, `endpoint`, `timeout`, `retry_count`, `status`, `remark`, `create_by`, `create_time`)
VALUES (1, '阿里云短信', 'ALIYUN', 'YOUR_ACCESS_KEY_ID', 'YOUR_ACCESS_KEY_SECRET', 'dysmsapi.aliyuncs.com', 5000, 0, '0', '阿里云短信渠道(源自旧sms.config)', 'admin', NOW());

-- ============================================================
-- 5. 预置一条签名模板记录
-- ============================================================
INSERT INTO `sys_sms_sign_template` (`st_id`, `channel_id`, `sign_name`, `template_code`, `template_desc`, `status`, `create_by`, `create_time`)
VALUES (1, 1, '智慧园区', 'SMS_123456789', '默认短信签名模板', '0', 'admin', NOW());

-- ============================================================
-- 6. 预置10条业务编码记录
-- ============================================================
INSERT INTO `sys_sms_biz` (`biz_code`, `biz_name`, `st_id`, `backup_channel_id`, `minute_limit`, `day_limit`, `status`, `remark`, `create_by`, `create_time`) VALUES
('device_repair',            '设备维修通知',        1, NULL, 5,  100, '0', '设备维修工单创建时发送',    'admin', NOW()),
('device_repair_transfer',   '设备维修转单通知',    1, NULL, 5,  100, '0', '设备维修工单转单时发送',    'admin', NOW()),
('device_offline_alert',     '设备离线告警',        1, NULL, 10, 200, '0', '设备心跳检测离线告警',      'admin', NOW()),
('smart_device_repair',      '智能设备维修通知',    1, NULL, 5,  100, '0', '智能设备维修工单通知',      'admin', NOW()),
('device_fault_repair',      '设备故障维修通知',    1, NULL, 5,  100, '0', '设备故障报修通知',          'admin', NOW()),
('device_repair_alert',      '设备维修告警',        1, NULL, 5,  100, '0', '设备维修超时告警通知',      'admin', NOW()),
('device_online_notify',     '设备上线通知',        1, NULL, 10, 200, '0', '设备恢复在线通知',          'admin', NOW()),
('visitor_approval',         '访客审批通知',        1, NULL, 10, 200, '0', '访客预约审批结果通知',      'admin', NOW()),
('sms_test',                 '短信发送测试',        1, NULL, 1,  10,  '0', '短信功能测试用业务编码',    'admin', NOW()),
('device_offline_old',       '旧版设备离线通知',    1, NULL, 5,  100, '0', '旧版设备离线通知(兼容保留)', 'admin', NOW());

-- ============================================================
-- 7. 清理旧短信菜单项
-- ============================================================
DELETE FROM `sys_menu` WHERE `menu_id` IN (2086, 2111, 2112);

-- ============================================================
-- 8. 新增短信中台菜单
-- ============================================================

-- 8.1 一级菜单: 短信中台 (parent=0, order=8)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('短信中台', 0, 8, 'sms', NULL, 1, 0, 'M', '0', '0', '', 'message', 'admin', NOW());

SET @sms_parent_id = LAST_INSERT_ID();

-- 8.2 二级菜单: 短信渠道管理
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('短信渠道管理', @sms_parent_id, 1, 'smsChannel', 'sms/channel/index', 1, 0, 'C', '0', '0', 'sms:channel:list', 'component', 'admin', NOW());

SET @channel_menu_id = LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
('渠道查询', @channel_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'sms:channel:query', '#', 'admin', NOW()),
('渠道新增', @channel_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'sms:channel:add',   '#', 'admin', NOW()),
('渠道修改', @channel_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'sms:channel:edit',  '#', 'admin', NOW()),
('渠道删除', @channel_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'sms:channel:remove','#', 'admin', NOW());

-- 8.3 二级菜单: 签名模板管理
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('签名模板管理', @sms_parent_id, 2, 'smsSignTemplate', 'sms/signtemplate/index', 1, 0, 'C', '0', '0', 'sms:signtemplate:list', 'documentation', 'admin', NOW());

SET @st_menu_id = LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
('模板查询', @st_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'sms:signtemplate:query', '#', 'admin', NOW()),
('模板新增', @st_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'sms:signtemplate:add',   '#', 'admin', NOW()),
('模板修改', @st_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'sms:signtemplate:edit',  '#', 'admin', NOW()),
('模板删除', @st_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'sms:signtemplate:remove','#', 'admin', NOW());

-- 8.4 二级菜单: 业务短信配置
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('业务短信配置', @sms_parent_id, 3, 'smsBiz', 'sms/biz/index', 1, 0, 'C', '0', '0', 'sms:biz:list', 'example', 'admin', NOW());

SET @biz_menu_id = LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
('业务查询', @biz_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'sms:biz:query', '#', 'admin', NOW()),
('业务新增', @biz_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'sms:biz:add',   '#', 'admin', NOW()),
('业务修改', @biz_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'sms:biz:edit',  '#', 'admin', NOW()),
('业务删除', @biz_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'sms:biz:remove','#', 'admin', NOW());

-- 8.5 二级菜单: 短信黑名单
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('短信黑名单', @sms_parent_id, 4, 'smsBlacklist', 'sms/blacklist/index', 1, 0, 'C', '0', '0', 'sms:blacklist:list', 'lock', 'admin', NOW());

SET @blacklist_menu_id = LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
('黑名单查询', @blacklist_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'sms:blacklist:query', '#', 'admin', NOW()),
('黑名单新增', @blacklist_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'sms:blacklist:add',   '#', 'admin', NOW()),
('黑名单修改', @blacklist_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'sms:blacklist:edit',  '#', 'admin', NOW()),
('黑名单删除', @blacklist_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'sms:blacklist:remove','#', 'admin', NOW());

-- 8.6 二级菜单: 定时短信任务
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('定时短信任务', @sms_parent_id, 5, 'smsSchedule', 'sms/schedule/index', 1, 0, 'C', '0', '0', 'sms:schedule:list', 'time', 'admin', NOW());

SET @schedule_menu_id = LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
('任务查询', @schedule_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'sms:schedule:query',  '#', 'admin', NOW()),
('任务撤销', @schedule_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'sms:schedule:cancel', '#', 'admin', NOW()),
('任务重发', @schedule_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'sms:schedule:retry',  '#', 'admin', NOW());

-- 8.7 二级菜单: 短信发送日志 (保留原有日志查询，升级为完整日志页面)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('短信发送日志', @sms_parent_id, 6, 'smsLog', 'sms/log/index', 1, 0, 'C', '0', '0', 'sms:log:list', 'list', 'admin', NOW());

SET @log_menu_id = LAST_INSERT_ID();
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`) VALUES
('日志查询', @log_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'sms:log:query',  '#', 'admin', NOW()),
('日志导出', @log_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'sms:log:export', '#', 'admin', NOW()),
('任务撤销', @log_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'sms:log:cancel', '#', 'admin', NOW()),
('任务重发', @log_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'sms:log:retry',  '#', 'admin', NOW());
