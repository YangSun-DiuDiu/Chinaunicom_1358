-- ============================================================
-- 修复表结构缺失字段 - 立即在 ry-vue100 数据库中执行
-- ============================================================

-- 1. iot_device_status_log：补充 create_by, create_time, update_by, update_time
ALTER TABLE `iot_device_status_log`
  ADD COLUMN `create_by`   varchar(64) DEFAULT ''                COMMENT '创建者'   AFTER `sms_recipient`,
  ADD COLUMN `create_time` datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `create_by`,
  ADD COLUMN `update_by`   varchar(64) DEFAULT ''                COMMENT '更新者'   AFTER `create_time`,
  ADD COLUMN `update_time` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `update_by`;

-- 2. iot_device_heartbeat_log：补充 create_by, create_time, update_by, update_time
ALTER TABLE `iot_device_heartbeat_log`
  ADD COLUMN `create_by`   varchar(64) DEFAULT ''                COMMENT '创建者'   AFTER `detect_time`,
  ADD COLUMN `create_time` datetime    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER `create_by`,
  ADD COLUMN `update_by`   varchar(64) DEFAULT ''                COMMENT '更新者'   AFTER `create_time`,
  ADD COLUMN `update_time` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `update_by`;

-- 3. sys_sms_log：补充 create_by, update_by, update_time
ALTER TABLE `sys_sms_log`
  ADD COLUMN `create_by`   varchar(64) DEFAULT ''                COMMENT '创建者'   AFTER `biz_id`,
  ADD COLUMN `update_by`   varchar(64) DEFAULT ''                COMMENT '更新者'   AFTER `create_time`,
  ADD COLUMN `update_time` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `update_by`;

-- 4. visitor_log：补充 update_by, update_time（remark 重复则先删除）
ALTER TABLE `visitor_log`
  ADD COLUMN `update_by`   varchar(64) DEFAULT ''                COMMENT '更新者'   AFTER `create_time`,
  ADD COLUMN `update_time` datetime    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `update_by`;
