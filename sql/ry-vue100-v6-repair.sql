-- ============================================
-- V6 设备维修工单系统 - 数据库迁移脚本
-- ============================================

-- 1. 创建维修工单表
DROP TABLE IF EXISTS `iot_device_repair`;
CREATE TABLE `iot_device_repair` (
    `repair_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '工单ID',
    `device_id`          BIGINT       NOT NULL COMMENT '设备ID',
    `device_name`        VARCHAR(100) DEFAULT '' COMMENT '设备名称',
    `device_ip`          VARCHAR(50)  DEFAULT '' COMMENT '设备IP',
    `fault_description`  VARCHAR(500) DEFAULT '' COMMENT '故障描述',
    `original_responsible` VARCHAR(100) DEFAULT '' COMMENT '原责任人',
    `original_phone`     VARCHAR(20)  DEFAULT '' COMMENT '原责任人电话',
    `current_responsible` VARCHAR(100) DEFAULT '' COMMENT '当前责任人',
    `current_phone`      VARCHAR(20)  DEFAULT '' COMMENT '当前责任人电话',
    `status`             VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING待处理/ASSIGNED已派发/ACCEPTED已接收/REJECTED已拒绝/COMPLETED已完成',
    `transfer_from`      VARCHAR(100) DEFAULT '' COMMENT '转派发起人',
    `transfer_to`        VARCHAR(100) DEFAULT '' COMMENT '转派接收人',
    `transfer_reason`    VARCHAR(500) DEFAULT '' COMMENT '转派/拒绝原因',
    `repair_result`      VARCHAR(500) DEFAULT '' COMMENT '维修结果说明',
    `repair_time`        DATETIME     DEFAULT NULL COMMENT '维修完成时间',
    `complete_token`     VARCHAR(64)  DEFAULT '' COMMENT '维修确认token(短信链接使用)',
    `create_by`          VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`        DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`          VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`        DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`             VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (`repair_id`),
    INDEX `idx_device_id` (`device_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_current_responsible` (`current_responsible`),
    INDEX `idx_complete_token` (`complete_token`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维修工单表';

-- 2. 设备状态日志表增加维修工单关联
ALTER TABLE `iot_device_status_log` ADD COLUMN `repair_id` BIGINT DEFAULT NULL COMMENT '关联维修工单ID' AFTER `sms_recipient`;

-- 3. 短信模板增加维修结果确认URL配置
INSERT IGNORE INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`)
VALUES ('维修确认页面URL', 'sms.repair.callback.url', 'http://1.94.26.126:3000/repair-complete', 'Y', 'admin', NOW());
