-- ============================================================
-- SmartIoT 管理系统数据库迁移脚本
-- 数据库: ry-vue100
-- 基于若依框架精简重构，新增设备管理、访客管理模块
-- ============================================================

-- 1. 设备管理模块 - 设备信息表
DROP TABLE IF EXISTS `iot_device`;
CREATE TABLE `iot_device` (
  `device_id` bigint NOT NULL AUTO_INCREMENT COMMENT '设备ID',
  `device_name` varchar(100) NOT NULL COMMENT '设备名称',
  `device_type` varchar(20) NOT NULL DEFAULT 'OTHER' COMMENT '设备类型(NETWORK:网络设备,MONITOR:监控设备,OTHER:其他设备)',
  `ip_address` varchar(50) DEFAULT '' COMMENT 'IP地址',
  `mac_address` varchar(50) DEFAULT '' COMMENT 'MAC地址',
  `model` varchar(100) DEFAULT '' COMMENT '设备型号',
  `location` varchar(200) DEFAULT '' COMMENT '设备位置',
  `port_info` varchar(500) DEFAULT '' COMMENT '端口信息(JSON)',
  `status` varchar(20) NOT NULL DEFAULT 'UNKNOWN' COMMENT '设备状态(ONLINE:在线,OFFLINE:离线,UNKNOWN:未知)',
  `snmp_community` varchar(100) DEFAULT 'public' COMMENT 'SNMP Community',
  `snmp_port` int DEFAULT 161 COMMENT 'SNMP端口',
  `snmp_version` varchar(10) DEFAULT 'v2c' COMMENT 'SNMP版本(v2c/v3)',
  `responsible` varchar(50) DEFAULT '' COMMENT '负责人',
  `responsible_phone` varchar(20) DEFAULT '' COMMENT '负责人电话(接收告警短信)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `parent_id` bigint DEFAULT NULL COMMENT '上级设备ID(拓扑关联)',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`device_id`),
  KEY `idx_device_type` (`device_type`),
  KEY `idx_status` (`status`),
  KEY `idx_tenant_id` (`tenant_id`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备信息表';

-- 2. 设备端口表
DROP TABLE IF EXISTS `iot_device_port`;
CREATE TABLE `iot_device_port` (
  `port_id` bigint NOT NULL AUTO_INCREMENT COMMENT '端口ID',
  `device_id` bigint NOT NULL COMMENT '所属设备ID',
  `port_name` varchar(100) NOT NULL COMMENT '端口名称',
  `port_type` varchar(20) DEFAULT '' COMMENT '端口类型',
  `port_status` varchar(20) DEFAULT 'DOWN' COMMENT '端口状态(UP/DOWN)',
  `connected_device_id` bigint DEFAULT NULL COMMENT '连接的对端设备ID',
  `connected_port_id` bigint DEFAULT NULL COMMENT '连接的对端端口ID',
  `remark` varchar(200) DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`port_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_connected_device` (`connected_device_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备端口表';

-- 3. 设备状态变更日志表
DROP TABLE IF EXISTS `iot_device_status_log`;
CREATE TABLE `iot_device_status_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `device_name` varchar(100) DEFAULT '' COMMENT '设备名称',
  `old_status` varchar(20) DEFAULT '' COMMENT '变更前状态',
  `new_status` varchar(20) DEFAULT '' COMMENT '变更后状态',
  `change_type` varchar(20) DEFAULT '' COMMENT '变更类型(OFFLINE:离线,ONLINE:上线)',
  `change_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  `sms_sent` char(1) DEFAULT 'N' COMMENT '是否发送短信(Y/N)',
  `sms_recipient` varchar(50) DEFAULT '' COMMENT '短信接收人',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`log_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_change_time` (`change_time`),
  KEY `idx_change_type` (`change_type`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备状态变更日志表';

-- 4. 设备心跳检测日志表
DROP TABLE IF EXISTS `iot_device_heartbeat_log`;
CREATE TABLE `iot_device_heartbeat_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `device_id` bigint NOT NULL COMMENT '设备ID',
  `device_name` varchar(100) DEFAULT '' COMMENT '设备名称',
  `ip_address` varchar(50) DEFAULT '' COMMENT 'IP地址',
  `ping_result` varchar(20) DEFAULT '' COMMENT 'Ping结果(SUCCESS/FAIL)',
  `ping_latency` bigint DEFAULT 0 COMMENT 'Ping延迟(毫秒)',
  `detect_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '检测时间',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`log_id`),
  KEY `idx_device_id` (`device_id`),
  KEY `idx_detect_time` (`detect_time`),
  KEY `idx_ping_result` (`ping_result`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='设备心跳检测日志表';

-- 5. 访客预约表
DROP TABLE IF EXISTS `visitor_appointment`;
CREATE TABLE `visitor_appointment` (
  `appointment_id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约ID',
  `visitor_name` varchar(50) NOT NULL COMMENT '访客姓名',
  `visitor_phone` varchar(20) NOT NULL COMMENT '访客电话',
  `visitor_id_card` varchar(20) DEFAULT '' COMMENT '访客身份证号(预留人脸识别)',
  `visitor_company` varchar(100) DEFAULT '' COMMENT '访客单位',
  `visit_reason` varchar(200) DEFAULT '' COMMENT '访问事由',
  `host_name` varchar(50) DEFAULT '' COMMENT '被访人姓名',
  `host_dept` varchar(100) DEFAULT '' COMMENT '被访人部门',
  `host_phone` varchar(20) DEFAULT '' COMMENT '被访人电话',
  `visit_time` datetime DEFAULT NULL COMMENT '计划来访时间',
  `leave_time` datetime DEFAULT NULL COMMENT '实际离开时间',
  `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态(PENDING:待审批,APPROVED:已通过,REJECTED:已拒绝,CANCELLED:已取消,VISITING:来访中,COMPLETED:已完成)',
  `approver_id` bigint DEFAULT NULL COMMENT '审批人ID',
  `approve_time` datetime DEFAULT NULL COMMENT '审批时间',
  `approve_remark` varchar(500) DEFAULT '' COMMENT '审批备注',
  `sms_sent` char(1) DEFAULT 'N' COMMENT '是否发送短信(Y/N)',
  `sms_content` varchar(500) DEFAULT '' COMMENT '短信内容',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`appointment_id`),
  KEY `idx_visitor_phone` (`visitor_phone`),
  KEY `idx_status` (`status`),
  KEY `idx_visit_time` (`visit_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='访客预约表';

-- 6. 来访记录表
DROP TABLE IF EXISTS `visitor_log`;
CREATE TABLE `visitor_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `appointment_id` bigint DEFAULT NULL COMMENT '关联预约ID(现场登记为空)',
  `visitor_name` varchar(50) NOT NULL COMMENT '访客姓名',
  `visitor_phone` varchar(20) DEFAULT '' COMMENT '访客电话',
  `visitor_id_card` varchar(20) DEFAULT '' COMMENT '访客身份证号',
  `visitor_company` varchar(100) DEFAULT '' COMMENT '访客单位',
  `visit_reason` varchar(200) DEFAULT '' COMMENT '访问事由',
  `host_name` varchar(50) DEFAULT '' COMMENT '被访人姓名',
  `host_dept` varchar(100) DEFAULT '' COMMENT '被访人部门',
  `entry_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '进入时间',
  `exit_time` datetime DEFAULT NULL COMMENT '离开时间',
  `register_type` varchar(20) DEFAULT 'WALKIN' COMMENT '登记类型(APPOINTMENT:预约,WALKIN:现场登记)',
  `tenant_id` bigint DEFAULT NULL COMMENT '租户ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`log_id`),
  KEY `idx_appointment_id` (`appointment_id`),
  KEY `idx_visitor_name` (`visitor_name`),
  KEY `idx_entry_time` (`entry_time`),
  KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='来访记录表';

-- 7. 短信通知日志表
DROP TABLE IF EXISTS `sys_sms_log`;
CREATE TABLE `sys_sms_log` (
  `sms_id` bigint NOT NULL AUTO_INCREMENT COMMENT '短信ID',
  `recipient` varchar(100) DEFAULT '' COMMENT '接收人姓名',
  `phone_number` varchar(20) NOT NULL COMMENT '手机号码',
  `content` varchar(500) DEFAULT '' COMMENT '短信内容',
  `send_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `send_result` varchar(20) DEFAULT 'SUCCESS' COMMENT '发送结果(SUCCESS/FAIL)',
  `biz_type` varchar(30) DEFAULT '' COMMENT '业务类型(DEVICE_OFFLINE/DEVICE_ONLINE/VISITOR_APPROVE/REPAIR)',
  `biz_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `create_by` varchar(64) DEFAULT '' COMMENT '创建者',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) DEFAULT '' COMMENT '更新者',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(200) DEFAULT '' COMMENT '备注',
  PRIMARY KEY (`sms_id`),
  KEY `idx_phone_number` (`phone_number`),
  KEY `idx_biz_type` (`biz_type`),
  KEY `idx_send_time` (`send_time`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='短信通知日志表';

-- 8. 插入测试设备数据
INSERT INTO `iot_device` (`device_name`, `device_type`, `ip_address`, `model`, `location`, `status`, `snmp_community`, `responsible`, `responsible_phone`, `remark`) VALUES
('华为防火墙USG6565', 'NETWORK', '116.148.212.150', 'USG6565', '数据中心机房A区', 'ONLINE', 'public', '张工', '13800001111', '测试设备-华为防火墙'),
('海康威视摄像头', 'MONITOR', '116.148.212.151', 'DS-2CD2T46WD-I8', '大门口岗亭', 'OFFLINE', 'public', '李工', '13800002222', '测试设备-海康摄像头');

-- 9. 更新原有菜单数据（如果存在则清理冗余菜单，保留核心菜单）
-- 删除若依原生冗余菜单（字典管理、代码生成、在线用户、服务监控、缓存监控等）
DELETE FROM `sys_menu` WHERE `menu_name` IN ('字典管理', '字典类型', '字典数据', '代码生成', '代码生成示例', '在线用户', '服务监控', '缓存监控', '系统接口', '表单构建', '定时任务', '定时任务调度', '定时任务日志');
DELETE FROM `sys_menu` WHERE `path` IN ('dict', 'gen', 'online', 'server', 'cache', 'swagger', 'build', 'job');

-- 10. 新增设备管理菜单
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
('设备管理', 0, 4, 'device', NULL, NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'monitor', 'admin', NOW(), '', NULL, '设备管理目录'),
('设备列表', (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='设备管理' LIMIT 1) AS t), 1, 'list', 'device/list/index', NULL, 'DeviceList', 1, 0, 'C', '0', '0', 'device:list:list', 'switching', 'admin', NOW(), '', NULL, '设备列表页面'),
('设备拓扑', (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='设备管理' LIMIT 1) AS t), 2, 'topology', 'device/topology/index', NULL, 'DeviceTopology', 1, 0, 'C', '0', '0', 'device:topology:view', 'tree', 'admin', NOW(), '', NULL, '设备拓扑页面'),
('心跳日志', (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='设备管理' LIMIT 1) AS t), 3, 'heartbeat', 'device/heartbeat/index', NULL, 'DeviceHeartbeat', 1, 0, 'C', '0', '0', 'device:heartbeat:list', 'log', 'admin', NOW(), '', NULL, '心跳检测日志页面');

-- 11. 新增访客管理菜单
INSERT IGNORE INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `query`, `route_name`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
('访客管理', 0, 5, 'visitor', NULL, NULL, NULL, 1, 0, 'M', '0', '0', NULL, 'people', 'admin', NOW(), '', NULL, '访客管理目录'),
('访客预约', (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='访客管理' LIMIT 1) AS t), 1, 'appointment', 'visitor/appointment/index', NULL, 'VisitorAppointment', 1, 0, 'C', '0', '0', 'visitor:appointment:list', 'edit', 'admin', NOW(), '', NULL, '访客预约页面'),
('访客审批', (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='访客管理' LIMIT 1) AS t), 2, 'approval', 'visitor/approval/index', NULL, 'VisitorApproval', 1, 0, 'C', '0', '0', 'visitor:approval:list', 'check', 'admin', NOW(), '', NULL, '访客审批页面'),
('现场登记', (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='访客管理' LIMIT 1) AS t), 3, 'register', 'visitor/register/index', NULL, 'VisitorRegister', 1, 0, 'C', '0', '0', 'visitor:register:add', 'form', 'admin', NOW(), '', NULL, '现场访客登记页面'),
('来访记录', (SELECT menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name='访客管理' LIMIT 1) AS t), 4, 'log', 'visitor/log/index', NULL, 'VisitorLog', 1, 0, 'C', '0', '0', 'visitor:log:list', 'list', 'admin', NOW(), '', NULL, '来访记录页面');

-- 12. 新增角色权限
INSERT IGNORE INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `status`, `del_flag`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
(3, '设备管理员', 'deviceAdmin', 3, '1', '0', '0', 'admin', NOW(), '', NULL, '设备管理权限角色'),
(4, '访客管理员', 'visitorAdmin', 4, '1', '0', '0', 'admin', NOW(), '', NULL, '访客审批权限角色');

-- 13. 短信告警配置参数
INSERT IGNORE INTO `sys_config` (`config_name`, `config_key`, `config_value`, `config_type`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`) VALUES
('短信API地址', 'sms.api.url', 'http://localhost:8080/api/sms/send', 'Y', 'admin', NOW(), '', NULL, '模拟短信API地址'),
('短信启用开关', 'sms.enabled', 'true', 'Y', 'admin', NOW(), '', NULL, '是否启用短信通知'),
('Ping检测间隔(秒)', 'device.ping.interval', '60', 'Y', 'admin', NOW(), '', NULL, '设备心跳Ping检测间隔'),
('SNMP超时时间(毫秒)', 'snmp.timeout', '3000', 'Y', 'admin', NOW(), '', NULL, 'SNMP请求超时时间');
