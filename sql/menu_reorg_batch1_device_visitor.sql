-- ============================================================
-- 菜单管理重构 - 与业务模块一一对应
-- 执行目标: ry101 数据库 (192.168.11.128)
-- ============================================================
USE ry101;

-- 先清理旧的智能化/考勤菜单(如果存在)
DELETE FROM sys_menu WHERE menu_name IN ('智能化管理','考勤管理','短信管理','数据对接','考勤数据') AND parent_id=0;
DELETE FROM sys_menu WHERE parent_id IN (SELECT t.menu_id FROM (SELECT menu_id FROM sys_menu WHERE menu_name IN ('智能化管理','考勤管理','短信管理')) t);
DELETE FROM sys_menu WHERE menu_name IN ('配件管理','状态日志','维修工单','地图模式','视频预览','监控设备管理','车辆通行权限','车辆通行管理','通行权限管理','人员通行管理','视频监控管理') AND parent_id IN (2000,2004);

-- ============================
-- 1. 设备管理扩展 (2000下)
-- ============================

-- 设备列表子按钮(F类型权限)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('设备查询', 2001, 1, '', '', 1, 0, 'F', '0', '0', 'device:list:query', '#', 'admin', NOW()),
('设备新增', 2001, 2, '', '', 1, 0, 'F', '0', '0', 'device:list:add', '#', 'admin', NOW()),
('设备修改', 2001, 3, '', '', 1, 0, 'F', '0', '0', 'device:list:edit', '#', 'admin', NOW()),
('设备删除', 2001, 4, '', '', 1, 0, 'F', '0', '0', 'device:list:remove', '#', 'admin', NOW()),
('设备导出', 2001, 5, '', '', 1, 0, 'F', '0', '0', 'device:list:export', '#', 'admin', NOW());

-- 设备拓扑子按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('拓扑查询', 2002, 1, '', '', 1, 0, 'F', '0', '0', 'device:topology:query', '#', 'admin', NOW());

-- 心跳日志子按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('心跳查询', 2003, 1, '', '', 1, 0, 'F', '0', '0', 'device:heartbeat:query', '#', 'admin', NOW()),
('心跳导出', 2003, 2, '', '', 1, 0, 'F', '0', '0', 'device:heartbeat:export', '#', 'admin', NOW());

-- 设备状态日志 (C型菜单)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('状态日志', 2000, 4, 'statusLog', 'device/statusLog/index', 1, 0, 'C', '0', '0', 'device:statusLog:list', 'log', 'admin', NOW());

SET @status_log_id = (SELECT menu_id FROM sys_menu WHERE perms='device:statusLog:list' AND parent_id=2000 LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('状态查询', @status_log_id, 1, '', '', 1, 0, 'F', '0', '0', 'device:statusLog:query', '#', 'admin', NOW()),
('状态导出', @status_log_id, 2, '', '', 1, 0, 'F', '0', '0', 'device:statusLog:export', '#', 'admin', NOW());

-- 维修工单 (C型菜单)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('维修工单', 2000, 5, 'repair', 'device/repair/index', 1, 0, 'C', '0', '0', 'device:repair:list', 'tool', 'admin', NOW());

SET @repair_id = (SELECT menu_id FROM sys_menu WHERE perms='device:repair:list' AND parent_id=2000 LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('工单查询', @repair_id, 1, '', '', 1, 0, 'F', '0', '0', 'device:repair:query', '#', 'admin', NOW()),
('工单新增', @repair_id, 2, '', '', 1, 0, 'F', '0', '0', 'device:repair:add', '#', 'admin', NOW()),
('工单修改', @repair_id, 3, '', '', 1, 0, 'F', '0', '0', 'device:repair:edit', '#', 'admin', NOW()),
('工单删除', @repair_id, 4, '', '', 1, 0, 'F', '0', '0', 'device:repair:remove', '#', 'admin', NOW()),
('工单转交', @repair_id, 5, '', '', 1, 0, 'F', '0', '0', 'device:repair:transfer', '#', 'admin', NOW()),
('工单完成', @repair_id, 6, '', '', 1, 0, 'F', '0', '0', 'device:repair:complete', '#', 'admin', NOW());

-- 配件管理 (移到设备管理下)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('配件管理', 2000, 6, 'parts', 'device/parts/index', 1, 0, 'C', '0', '0', 'device:parts:list', 'component', 'admin', NOW());

SET @parts_id = (SELECT menu_id FROM sys_menu WHERE perms='device:parts:list' AND parent_id=2000 LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('配件查询', @parts_id, 1, '', '', 1, 0, 'F', '0', '0', 'device:parts:query', '#', 'admin', NOW()),
('配件新增', @parts_id, 2, '', '', 1, 0, 'F', '0', '0', 'device:parts:add', '#', 'admin', NOW()),
('配件修改', @parts_id, 3, '', '', 1, 0, 'F', '0', '0', 'device:parts:edit', '#', 'admin', NOW()),
('配件删除', @parts_id, 4, '', '', 1, 0, 'F', '0', '0', 'device:parts:remove', '#', 'admin', NOW());

-- SNMP工具 (C型菜单)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('SNMP工具', 2000, 7, 'snmp', 'device/snmp/index', 1, 0, 'C', '0', '0', 'device:snmp:list', 'monitor', 'admin', NOW());

SET @snmp_id = (SELECT menu_id FROM sys_menu WHERE perms='device:snmp:list' AND parent_id=2000 LIMIT 1);
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('SNMP查询', @snmp_id, 1, '', '', 1, 0, 'F', '0', '0', 'device:snmp:query', '#', 'admin', NOW());

-- ============================
-- 2. 访客管理扩展 (2004下)
-- ============================

-- 访客预约子按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('预约查询', 2005, 1, '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:query', '#', 'admin', NOW()),
('预约新增', 2005, 2, '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:add', '#', 'admin', NOW()),
('预约修改', 2005, 3, '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:edit', '#', 'admin', NOW()),
('预约删除', 2005, 4, '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:remove', '#', 'admin', NOW()),
('预约导出', 2005, 5, '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:export', '#', 'admin', NOW()),
('预约取消', 2005, 6, '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:cancel', '#', 'admin', NOW()),
('完成来访', 2005, 7, '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:complete', '#', 'admin', NOW());

-- 访客审批子按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('审批查询', 2006, 1, '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:pending', '#', 'admin', NOW()),
('审批通过', 2006, 2, '', '', 1, 0, 'F', '0', '0', 'visitor:appointment:approve', '#', 'admin', NOW());

-- 现场登记子按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('登记新增', 2007, 1, '', '', 1, 0, 'F', '0', '0', 'visitor:log:add', '#', 'admin', NOW());

-- 来访记录子按钮
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('记录查询', 2008, 1, '', '', 1, 0, 'F', '0', '0', 'visitor:log:query', '#', 'admin', NOW()),
('记录导出', 2008, 2, '', '', 1, 0, 'F', '0', '0', 'visitor:log:export', '#', 'admin', NOW()),
('记录离开', 2008, 3, '', '', 1, 0, 'F', '0', '0', 'visitor:log:exit', '#', 'admin', NOW());

-- H5登记二维码 (C型菜单)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('H5自助登记', 2004, 5, 'h5register', 'visitor/h5/register', 1, 0, 'C', '0', '0', 'visitor:h5:register', 'mobile', 'admin', NOW());

-- 通行码查询页 (C型菜单)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('通行码查询', 2004, 6, 'pass', 'visitor/pass/index', 1, 0, 'C', '0', '0', 'visitor:pass:list', 'qr-code', 'admin', NOW());
