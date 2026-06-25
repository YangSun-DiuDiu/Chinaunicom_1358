-- ============================================================
-- 菜单管理重构 - 批次2: 智能化管理 + 考勤管理 + 短信管理
-- ============================================================
USE ry101;

-- ============================
-- 3. 智能化管理 (新的一级菜单, sort=6)
-- ============================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('智能化管理', 0, 6, 'smart', NULL, 1, 0, 'M', '0', '0', NULL, 'cpu', 'admin', NOW());

SET @smart_id = LAST_INSERT_ID();

-- ============ 3.1 人员通行管理 (二级菜单) ============
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('人员通行管理', @smart_id, 1, 'person-access', NULL, 1, 0, 'M', '0', '0', NULL, 'people', 'admin', NOW());

SET @person_id = LAST_INSERT_ID();

-- 人员通行设备
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('人员通行设备', @person_id, 1, 'personDevice', 'smart/person-access/device/index', 1, 0, 'C', '0', '0', 'smart:personDevice:list', 'monitor', 'admin', NOW());

SET @person_dev_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('设备查询', @person_dev_id, 1, '', '', 1, 0, 'F', '0', '0', 'smart:personDevice:query', '#', 'admin', NOW()),
('设备新增', @person_dev_id, 2, '', '', 1, 0, 'F', '0', '0', 'smart:personDevice:add', '#', 'admin', NOW()),
('设备修改', @person_dev_id, 3, '', '', 1, 0, 'F', '0', '0', 'smart:personDevice:edit', '#', 'admin', NOW()),
('设备删除', @person_dev_id, 4, '', '', 1, 0, 'F', '0', '0', 'smart:personDevice:remove', '#', 'admin', NOW());

-- 通行权限管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('通行权限管理', @person_id, 2, 'personPerm', 'smart/person-access/permission/index', 1, 0, 'C', '0', '0', 'smart:personPerm:list', 'lock', 'admin', NOW());

SET @person_perm_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('权限查询', @person_perm_id, 1, '', '', 1, 0, 'F', '0', '0', 'smart:personPerm:query', '#', 'admin', NOW()),
('权限新增', @person_perm_id, 2, '', '', 1, 0, 'F', '0', '0', 'smart:personPerm:add', '#', 'admin', NOW()),
('权限修改', @person_perm_id, 3, '', '', 1, 0, 'F', '0', '0', 'smart:personPerm:edit', '#', 'admin', NOW()),
('权限删除', @person_perm_id, 4, '', '', 1, 0, 'F', '0', '0', 'smart:personPerm:remove', '#', 'admin', NOW());

-- 人员车辆绑定
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('人员车辆绑定', @person_id, 3, 'personVehicle', 'smart/person-access/vehicle/index', 1, 0, 'C', '0', '0', 'smart:personVehicle:list', 'car', 'admin', NOW());

SET @person_veh_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('绑定查询', @person_veh_id, 1, '', '', 1, 0, 'F', '0', '0', 'smart:personVehicle:query', '#', 'admin', NOW()),
('绑定新增', @person_veh_id, 2, '', '', 1, 0, 'F', '0', '0', 'smart:personVehicle:add', '#', 'admin', NOW()),
('绑定修改', @person_veh_id, 3, '', '', 1, 0, 'F', '0', '0', 'smart:personVehicle:edit', '#', 'admin', NOW()),
('绑定删除', @person_veh_id, 4, '', '', 1, 0, 'F', '0', '0', 'smart:personVehicle:remove', '#', 'admin', NOW());

-- ============ 3.2 车辆通行管理 (二级菜单) ============
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('车辆通行管理', @smart_id, 2, 'vehicle-access', NULL, 1, 0, 'M', '0', '0', NULL, 'car', 'admin', NOW());

SET @veh_id = LAST_INSERT_ID();

-- 车辆通行设备
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('车辆通行设备', @veh_id, 1, 'vehicleDevice', 'smart/vehicle-access/device/index', 1, 0, 'C', '0', '0', 'smart:vehicleDevice:list', 'monitor', 'admin', NOW());

SET @veh_dev_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('设备查询', @veh_dev_id, 1, '', '', 1, 0, 'F', '0', '0', 'smart:vehicleDevice:query', '#', 'admin', NOW()),
('设备新增', @veh_dev_id, 2, '', '', 1, 0, 'F', '0', '0', 'smart:vehicleDevice:add', '#', 'admin', NOW()),
('设备修改', @veh_dev_id, 3, '', '', 1, 0, 'F', '0', '0', 'smart:vehicleDevice:edit', '#', 'admin', NOW()),
('设备删除', @veh_dev_id, 4, '', '', 1, 0, 'F', '0', '0', 'smart:vehicleDevice:remove', '#', 'admin', NOW());

-- 车辆通行权限
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('车辆通行权限', @veh_id, 2, 'vehiclePerm', 'smart/vehicle-access/permission/index', 1, 0, 'C', '0', '0', 'smart:vehiclePerm:list', 'lock', 'admin', NOW());

SET @veh_perm_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('权限查询', @veh_perm_id, 1, '', '', 1, 0, 'F', '0', '0', 'smart:vehiclePerm:query', '#', 'admin', NOW()),
('权限新增', @veh_perm_id, 2, '', '', 1, 0, 'F', '0', '0', 'smart:vehiclePerm:add', '#', 'admin', NOW()),
('权限修改', @veh_perm_id, 3, '', '', 1, 0, 'F', '0', '0', 'smart:vehiclePerm:edit', '#', 'admin', NOW()),
('权限删除', @veh_perm_id, 4, '', '', 1, 0, 'F', '0', '0', 'smart:vehiclePerm:remove', '#', 'admin', NOW());

-- ============ 3.3 视频监控管理 (二级菜单) ============
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('视频监控管理', @smart_id, 3, 'video', NULL, 1, 0, 'M', '0', '0', NULL, 'video', 'admin', NOW());

SET @video_id = LAST_INSERT_ID();

-- 监控设备管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('监控设备管理', @video_id, 1, 'videoDevice', 'smart/video/device/index', 1, 0, 'C', '0', '0', 'smart:videoDevice:list', 'camera', 'admin', NOW());

SET @video_dev_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('设备查询', @video_dev_id, 1, '', '', 1, 0, 'F', '0', '0', 'smart:videoDevice:query', '#', 'admin', NOW()),
('设备新增', @video_dev_id, 2, '', '', 1, 0, 'F', '0', '0', 'smart:videoDevice:add', '#', 'admin', NOW()),
('设备修改', @video_dev_id, 3, '', '', 1, 0, 'F', '0', '0', 'smart:videoDevice:edit', '#', 'admin', NOW()),
('设备删除', @video_dev_id, 4, '', '', 1, 0, 'F', '0', '0', 'smart:videoDevice:remove', '#', 'admin', NOW());

-- 视频预览
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('视频预览', @video_id, 2, 'videoPreview', 'smart/video/preview/index', 1, 0, 'C', '0', '0', 'smart:videoPreview:list', 'play', 'admin', NOW());

-- 地图模式
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('地图模式', @video_id, 3, 'videoMap', 'smart/video/map/index', 1, 0, 'C', '0', '0', 'smart:videoMap:list', 'map', 'admin', NOW());

-- 底图管理
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('底图管理', @video_id, 4, 'mapBase', 'smart/video/mapBase/index', 1, 0, 'C', '0', '0', 'smart:mapBase:list', 'image', 'admin', NOW());

SET @map_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('底图查询', @map_id, 1, '', '', 1, 0, 'F', '0', '0', 'smart:mapBase:query', '#', 'admin', NOW()),
('底图新增', @map_id, 2, '', '', 1, 0, 'F', '0', '0', 'smart:mapBase:add', '#', 'admin', NOW()),
('底图修改', @map_id, 3, '', '', 1, 0, 'F', '0', '0', 'smart:mapBase:edit', '#', 'admin', NOW()),
('底图删除', @map_id, 4, '', '', 1, 0, 'F', '0', '0', 'smart:mapBase:remove', '#', 'admin', NOW());

-- ============================
-- 4. 考勤管理 (一级菜单, sort=7)
-- ============================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('考勤管理', 0, 7, 'attendance', NULL, 1, 0, 'M', '0', '0', NULL, 'date', 'admin', NOW());

SET @att_id = LAST_INSERT_ID();

-- 考勤数据
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('考勤数据', @att_id, 1, 'attendanceData', 'attendance/data/index', 1, 0, 'C', '0', '0', 'attendance:data:list', 'chart', 'admin', NOW());

SET @att_data_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('数据查询', @att_data_id, 1, '', '', 1, 0, 'F', '0', '0', 'attendance:data:query', '#', 'admin', NOW()),
('数据新增', @att_data_id, 2, '', '', 1, 0, 'F', '0', '0', 'attendance:data:add', '#', 'admin', NOW()),
('数据修改', @att_data_id, 3, '', '', 1, 0, 'F', '0', '0', 'attendance:data:edit', '#', 'admin', NOW()),
('数据删除', @att_data_id, 4, '', '', 1, 0, 'F', '0', '0', 'attendance:data:remove', '#', 'admin', NOW()),
('数据导出', @att_data_id, 5, '', '', 1, 0, 'F', '0', '0', 'attendance:data:export', '#', 'admin', NOW());

-- 钉钉对接
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('钉钉对接', @att_id, 2, 'dingtalk', 'attendance/dingtalk/index', 1, 0, 'C', '0', '0', 'attendance:dingtalk:list', 'link', 'admin', NOW());

SET @ding_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('对接查询', @ding_id, 1, '', '', 1, 0, 'F', '0', '0', 'attendance:dingtalk:query', '#', 'admin', NOW()),
('同步执行', @ding_id, 2, '', '', 1, 0, 'F', '0', '0', 'attendance:dingtalk:sync', '#', 'admin', NOW());

-- ============================
-- 5. 短信管理 (一级菜单, sort=8)
-- ============================
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('短信管理', 0, 8, 'sms', NULL, 1, 0, 'M', '0', '0', NULL, 'message', 'admin', NOW());

SET @sms_id = LAST_INSERT_ID();

-- 短信日志
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('短信日志', @sms_id, 1, 'smsLog', 'sms/log/index', 1, 0, 'C', '0', '0', 'sms:log:list', 'list', 'admin', NOW());

SET @sms_log_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('日志查询', @sms_log_id, 1, '', '', 1, 0, 'F', '0', '0', 'sms:log:query', '#', 'admin', NOW()),
('日志导出', @sms_log_id, 2, '', '', 1, 0, 'F', '0', '0', 'sms:log:export', '#', 'admin', NOW());

-- 短信配置
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('短信配置', @sms_id, 2, 'smsConfig', 'sms/config/index', 1, 0, 'C', '0', '0', 'sms:config:list', 'edit', 'admin', NOW());

SET @sms_cfg_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('配置查询', @sms_cfg_id, 1, '', '', 1, 0, 'F', '0', '0', 'sms:config:query', '#', 'admin', NOW()),
('配置修改', @sms_cfg_id, 2, '', '', 1, 0, 'F', '0', '0', 'sms:config:edit', '#', 'admin', NOW());
