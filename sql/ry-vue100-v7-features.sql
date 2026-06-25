-- ============================================
-- V7 智能化管理 + 考勤管理 + 维修增强
-- ============================================

-- ==================== 人员通行设备管理 ====================
DROP TABLE IF EXISTS `iot_person_access_device`;
CREATE TABLE `iot_person_access_device` (
    `device_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    `device_name`        VARCHAR(100) NOT NULL COMMENT '设备名称',
    `device_brand`       VARCHAR(50)  DEFAULT '' COMMENT '设备品牌(HIKVISION/DAHUA/OTHER)',
    `ip_address`         VARCHAR(50)  NOT NULL COMMENT 'IP地址',
    `port`               INT          DEFAULT 80 COMMENT '端口号',
    `username`           VARCHAR(100) DEFAULT '' COMMENT '登录用户名',
    `password`           VARCHAR(100) DEFAULT '' COMMENT '登录密码',
    `location`           VARCHAR(200) DEFAULT '' COMMENT '安装位置',
    `status`             VARCHAR(20)  DEFAULT 'ONLINE' COMMENT '状态(ONLINE/OFFLINE)',
    `create_by`          VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`        DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`          VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`        DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`             VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员通行设备表';

-- ==================== 人员通行权限 ====================
DROP TABLE IF EXISTS `iot_person_access_permission`;
CREATE TABLE `iot_person_access_permission` (
    `perm_id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `target_type`       VARCHAR(20)  NOT NULL COMMENT '授权类型(DEPT=部门 PERSON=个人)',
    `target_id`         BIGINT       NOT NULL COMMENT '目标ID(部门ID或用户ID)',
    `target_name`       VARCHAR(100) DEFAULT '' COMMENT '目标名称(部门名或用户名)',
    `device_ids`        VARCHAR(500) DEFAULT '' COMMENT '授权设备ID列表(逗号分隔)',
    `start_time`        DATETIME     DEFAULT NULL COMMENT '生效开始时间',
    `end_time`          DATETIME     DEFAULT NULL COMMENT '生效结束时间',
    `status`            VARCHAR(20)  DEFAULT 'ENABLED' COMMENT '状态(ENABLED/DISABLED)',
    `create_by`         VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`       DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`         VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`       DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`            VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (`perm_id`),
    INDEX `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员通行权限表';

-- ==================== 车辆通行设备管理 ====================
DROP TABLE IF EXISTS `iot_vehicle_access_device`;
CREATE TABLE `iot_vehicle_access_device` (
    `device_id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '设备ID',
    `device_name`        VARCHAR(100) NOT NULL COMMENT '设备名称',
    `device_brand`       VARCHAR(50)  DEFAULT '' COMMENT '设备品牌(HIKVISION/DAHUA/OTHER)',
    `ip_address`         VARCHAR(50)  NOT NULL COMMENT 'IP地址',
    `port`               INT          DEFAULT 80 COMMENT '端口号',
    `username`           VARCHAR(100) DEFAULT '' COMMENT '登录用户名',
    `password`           VARCHAR(100) DEFAULT '' COMMENT '登录密码',
    `location`           VARCHAR(200) DEFAULT '' COMMENT '安装位置',
    `status`             VARCHAR(20)  DEFAULT 'ONLINE' COMMENT '状态(ONLINE/OFFLINE)',
    `create_by`          VARCHAR(64)  DEFAULT '' COMMENT '创建者',
    `create_time`        DATETIME     DEFAULT NULL COMMENT '创建时间',
    `update_by`          VARCHAR(64)  DEFAULT '' COMMENT '更新者',
    `update_time`        DATETIME     DEFAULT NULL COMMENT '更新时间',
    `remark`             VARCHAR(500) DEFAULT '' COMMENT '备注',
    PRIMARY KEY (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆通行设备表';

-- ==================== 车辆通行权限 ====================
DROP TABLE IF EXISTS `iot_vehicle_access_permission`;
CREATE TABLE `iot_vehicle_access_permission` (
    `perm_id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `target_type`       VARCHAR(20)  NOT NULL COMMENT '授权类型(DEPT=部门 PERSON=个人)',
    `target_id`         BIGINT       NOT NULL COMMENT '目标ID(部门ID或用户ID)',
    `target_name`       VARCHAR(100) DEFAULT '' COMMENT '目标名称',
    `vehicle_plate`     VARCHAR(20)  DEFAULT '' COMMENT '车牌号',
    `device_ids`        VARCHAR(500) DEFAULT '' COMMENT '授权设备ID列表',
    `start_time`        DATETIME     DEFAULT NULL COMMENT '生效开始时间',
    `end_time`          DATETIME     DEFAULT NULL COMMENT '生效结束时间',
    `status`            VARCHAR(20)  DEFAULT 'ENABLED' COMMENT '状态',
    `create_by`         VARCHAR(64)  DEFAULT '',
    `create_time`       DATETIME     DEFAULT NULL,
    `update_by`         VARCHAR(64)  DEFAULT '',
    `update_time`       DATETIME     DEFAULT NULL,
    `remark`            VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`perm_id`),
    INDEX `idx_target` (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆通行权限表';

-- ==================== 人员车辆绑定 ====================
DROP TABLE IF EXISTS `iot_person_vehicle`;
CREATE TABLE `iot_person_vehicle` (
    `bind_id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT       NOT NULL COMMENT '用户ID',
    `user_name`         VARCHAR(100) DEFAULT '' COMMENT '用户名',
    `dept_id`           BIGINT       DEFAULT NULL COMMENT '部门ID',
    `dept_name`         VARCHAR(100) DEFAULT '' COMMENT '部门名',
    `vehicle_plate`     VARCHAR(20)  NOT NULL COMMENT '车牌号',
    `vehicle_brand`     VARCHAR(50)  DEFAULT '' COMMENT '车辆品牌',
    `vehicle_color`     VARCHAR(20)  DEFAULT '' COMMENT '车辆颜色',
    `create_by`         VARCHAR(64)  DEFAULT '',
    `create_time`       DATETIME     DEFAULT NULL,
    `update_by`         VARCHAR(64)  DEFAULT '',
    `update_time`       DATETIME     DEFAULT NULL,
    `remark`            VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`bind_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_plate` (`vehicle_plate`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员车辆绑定表';

-- ==================== 视频监控设备 ====================
DROP TABLE IF EXISTS `iot_video_device`;
CREATE TABLE `iot_video_device` (
    `device_id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `device_name`        VARCHAR(100) NOT NULL COMMENT '设备名称',
    `device_brand`       VARCHAR(50)  DEFAULT '' COMMENT '品牌(HIKVISION/DAHUA/OTHER)',
    `ip_address`         VARCHAR(50)  NOT NULL COMMENT 'IP地址',
    `rtsp_port`          INT          DEFAULT 554 COMMENT 'RTSP端口',
    `rtsp_url`           VARCHAR(500) DEFAULT '' COMMENT 'RTSP流地址',
    `username`           VARCHAR(100) DEFAULT 'admin' COMMENT '用户名',
    `password`           VARCHAR(100) DEFAULT '' COMMENT '密码',
    `channel`            INT          DEFAULT 1 COMMENT '通道号',
    `stream_type`        VARCHAR(20)  DEFAULT 'MAIN' COMMENT '码流类型(MAIN/SUB)',
    `location`           VARCHAR(200) DEFAULT '' COMMENT '安装位置',
    `map_x`              DECIMAL(10,2) DEFAULT NULL COMMENT '地图X坐标',
    `map_y`              DECIMAL(10,2) DEFAULT NULL COMMENT '地图Y坐标',
    `status`             VARCHAR(20)  DEFAULT 'ONLINE' COMMENT '状态',
    `create_by`          VARCHAR(64)  DEFAULT '',
    `create_time`        DATETIME     DEFAULT NULL,
    `update_by`          VARCHAR(64)  DEFAULT '',
    `update_time`        DATETIME     DEFAULT NULL,
    `remark`             VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`device_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='视频监控设备表';

-- ==================== 底图管理 ====================
DROP TABLE IF EXISTS `iot_map_base`;
CREATE TABLE `iot_map_base` (
    `map_id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `map_name`           VARCHAR(100) NOT NULL COMMENT '底图名称',
    `map_image`          VARCHAR(500) NOT NULL COMMENT '底图文件路径',
    `map_width`          INT          DEFAULT 1920 COMMENT '底图宽度',
    `map_height`         INT          DEFAULT 1080 COMMENT '底图高度',
    `create_by`          VARCHAR(64)  DEFAULT '',
    `create_time`        DATETIME     DEFAULT NULL,
    `update_by`          VARCHAR(64)  DEFAULT '',
    `update_time`        DATETIME     DEFAULT NULL,
    `remark`             VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`map_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地图底图表';

-- ==================== 维修工单增强：增加照片和配件使用 ====================
ALTER TABLE `iot_device_repair` ADD COLUMN `photo_before` VARCHAR(500) DEFAULT '' COMMENT '维修前照片(逗号分隔多张)' AFTER `complete_token`;
ALTER TABLE `iot_device_repair` ADD COLUMN `photo_after` VARCHAR(500) DEFAULT '' COMMENT '维修后照片(逗号分隔多张)' AFTER `photo_before`;
ALTER TABLE `iot_device_repair` ADD COLUMN `has_parts` VARCHAR(1) DEFAULT '0' COMMENT '是否使用配件(0=否 1=是)' AFTER `photo_after`;
ALTER TABLE `iot_device_repair` ADD COLUMN `parts_desc` VARCHAR(500) DEFAULT '' COMMENT '配件使用描述' AFTER `has_parts`;

-- ==================== 配件管理 ====================
DROP TABLE IF EXISTS `iot_parts`;
CREATE TABLE `iot_parts` (
    `part_id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `part_name`          VARCHAR(100) NOT NULL COMMENT '配件名称',
    `part_code`          VARCHAR(50)  DEFAULT '' COMMENT '配件编码',
    `part_model`         VARCHAR(100) DEFAULT '' COMMENT '规格型号',
    `unit`               VARCHAR(20)  DEFAULT '个' COMMENT '单位',
    `quantity`           INT          DEFAULT 0 COMMENT '当前库存数量',
    `alert_quantity`     INT          DEFAULT 5 COMMENT '预警库存数量',
    `price`              DECIMAL(10,2) DEFAULT 0 COMMENT '单价',
    `photo`              VARCHAR(500) DEFAULT '' COMMENT '配件照片',
    `create_by`          VARCHAR(64)  DEFAULT '',
    `create_time`        DATETIME     DEFAULT NULL,
    `update_by`          VARCHAR(64)  DEFAULT '',
    `update_time`        DATETIME     DEFAULT NULL,
    `remark`             VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`part_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配件管理表';

-- ==================== 配件使用记录 ====================
DROP TABLE IF EXISTS `iot_parts_usage`;
CREATE TABLE `iot_parts_usage` (
    `usage_id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `repair_id`          BIGINT       NOT NULL COMMENT '维修工单ID',
    `part_id`            BIGINT       NOT NULL COMMENT '配件ID',
    `part_name`          VARCHAR(100) DEFAULT '' COMMENT '配件名称',
    `quantity`           INT          DEFAULT 1 COMMENT '使用数量',
    `used_by`            VARCHAR(100) DEFAULT '' COMMENT '使用人',
    `used_time`          DATETIME     DEFAULT NULL COMMENT '使用时间',
    `photo`              VARCHAR(500) DEFAULT '' COMMENT '使用照片',
    `create_by`          VARCHAR(64)  DEFAULT '',
    `create_time`        DATETIME     DEFAULT NULL,
    `remark`             VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`usage_id`),
    INDEX `idx_repair` (`repair_id`),
    INDEX `idx_part` (`part_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配件使用记录表';

-- ==================== 考勤记录 ====================
DROP TABLE IF EXISTS `attendance_record`;
CREATE TABLE `attendance_record` (
    `record_id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_name`          VARCHAR(100) NOT NULL COMMENT '用户名',
    `dept_name`          VARCHAR(100) DEFAULT '' COMMENT '部门名称',
    `attendance_date`    DATE         NOT NULL COMMENT '考勤日期',
    `check_in_time`      DATETIME     DEFAULT NULL COMMENT '签到时间',
    `check_out_time`     DATETIME     DEFAULT NULL COMMENT '签退时间',
    `status`             VARCHAR(20)  DEFAULT 'NORMAL' COMMENT '状态(NORMAL/LATE/EARLY/ABSENT/OVERTIME)',
    `source`             VARCHAR(20)  DEFAULT 'MANUAL' COMMENT '数据来源(DINGTALK/WECHAT/MANUAL)',
    `create_by`          VARCHAR(64)  DEFAULT '',
    `create_time`        DATETIME     DEFAULT NULL,
    `remark`             VARCHAR(500) DEFAULT '',
    PRIMARY KEY (`record_id`),
    INDEX `idx_user_date` (`user_name`, `attendance_date`),
    INDEX `idx_date` (`attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考勤记录表';

-- ==================== 菜单数据 ====================
-- 一级菜单：智能化管理
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('智能化管理', 0, 3, 'smart', NULL, 1, 0, 'M', '0', '0', NULL, 'cpu', 'admin', NOW());

SET @smart_id = (SELECT menu_id FROM sys_menu WHERE menu_name='智能化管理' AND parent_id=0 LIMIT 1);

-- 二级菜单：人员通行管理
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('人员通行管理', @smart_id, 1, 'person-access', NULL, 1, 0, 'M', '0', '0', NULL, 'people', 'admin', NOW());

SET @person_id = (SELECT menu_id FROM sys_menu WHERE menu_name='人员通行管理' AND parent_id=@smart_id LIMIT 1);

-- 三级菜单：人员通行设备管理
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('人员通行设备', @person_id, 1, 'device', 'smart/person-access/device/index', 1, 0, 'C', '0', '0', 'smart:personAccess:list', 'monitor', 'admin', NOW());

-- 三级菜单：人员通行权限管理
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('通行权限管理', @person_id, 2, 'permission', 'smart/person-access/permission/index', 1, 0, 'C', '0', '0', 'smart:personPerm:list', 'lock', 'admin', NOW());

-- 二级菜单：车辆通行管理
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('车辆通行管理', @smart_id, 2, 'vehicle-access', NULL, 1, 0, 'M', '0', '0', NULL, 'car', 'admin', NOW());

SET @vehicle_id = (SELECT menu_id FROM sys_menu WHERE menu_name='车辆通行管理' AND parent_id=@smart_id LIMIT 1);

-- 三级菜单：车辆通行设备管理
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('车辆通行设备', @vehicle_id, 1, 'device', 'smart/vehicle-access/device/index', 1, 0, 'C', '0', '0', 'smart:vehicleAccess:list', 'monitor', 'admin', NOW());

-- 三级菜单：车辆通行权限管理
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('车辆通行权限', @vehicle_id, 2, 'permission', 'smart/vehicle-access/permission/index', 1, 0, 'C', '0', '0', 'smart:vehiclePerm:list', 'lock', 'admin', NOW());

-- 二级菜单：视频监控管理
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('视频监控管理', @smart_id, 3, 'video', NULL, 1, 0, 'M', '0', '0', NULL, 'video', 'admin', NOW());

SET @video_id = (SELECT menu_id FROM sys_menu WHERE menu_name='视频监控管理' AND parent_id=@smart_id LIMIT 1);

-- 三级菜单：视频监控设备管理
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('监控设备管理', @video_id, 1, 'device', 'smart/video/device/index', 1, 0, 'C', '0', '0', 'smart:videoDevice:list', 'camera', 'admin', NOW());

-- 三级菜单：视频监控预览
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('视频预览', @video_id, 2, 'preview', 'smart/video/preview/index', 1, 0, 'C', '0', '0', 'smart:videoPreview:list', 'play', 'admin', NOW());

-- 三级菜单：地图模式
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('地图模式', @video_id, 3, 'map', 'smart/video/map/index', 1, 0, 'C', '0', '0', 'smart:videoMap:list', 'map', 'admin', NOW());

-- ==================== 设备管理：配件管理 ====================
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('配件管理', (SELECT menu_id FROM sys_menu WHERE menu_name='设备管理' AND parent_id=0 LIMIT 1), 7, 'parts', 'device/parts/index', 1, 0, 'C', '0', '0', 'device:parts:list', 'component', 'admin', NOW());

-- ==================== 一级菜单：考勤管理 ====================
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('考勤管理', 0, 6, 'attendance', NULL, 1, 0, 'M', '0', '0', NULL, 'date', 'admin', NOW());

SET @att_id = (SELECT menu_id FROM sys_menu WHERE menu_name='考勤管理' AND parent_id=0 LIMIT 1);

-- 二级菜单：数据对接
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('数据对接', @att_id, 1, 'import', 'attendance/import/index', 1, 0, 'C', '0', '0', 'attendance:import:list', 'upload', 'admin', NOW());

-- 二级菜单：考勤数据
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`)
VALUES ('考勤数据', @att_id, 2, 'data', 'attendance/data/index', 1, 0, 'C', '0', '0', 'attendance:data:list', 'chart', 'admin', NOW());
