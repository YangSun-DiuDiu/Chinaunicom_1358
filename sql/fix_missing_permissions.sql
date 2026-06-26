-- 补全缺失的后端 Controller 权限到 sys_menu
-- 所有 backend @PreAuthorize 中用到但 DB 中缺失的权限

-- === 考勤管理 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('考勤数据新增', 2021, 3, '', '', 'F', '0', '0', 'attendance:data:add', 'admin', NOW()),
('考勤数据导出', 2021, 4, '', '', 'F', '0', '0', 'attendance:data:export', 'admin', NOW()),
('考勤数据删除', 2021, 5, '', '', 'F', '0', '0', 'attendance:data:remove', 'admin', NOW()),
('数据对接新增', 2021, 6, '', '', 'F', '0', '0', 'attendance:import:add', 'admin', NOW()),
('数据对接编辑', 2021, 7, '', '', 'F', '0', '0', 'attendance:import:edit', 'admin', NOW()),
('数据对接删除', 2021, 8, '', '', 'F', '0', '0', 'attendance:import:remove', 'admin', NOW());

-- === 设备管理 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('设备新增', 2000, 9, '', '', 'F', '0', '0', 'device:add', 'admin', NOW()),
('设备修改', 2000, 10, '', '', 'F', '0', '0', 'device:edit', 'admin', NOW()),
('设备删除', 2000, 11, '', '', 'F', '0', '0', 'device:remove', 'admin', NOW()),
('设备查询', 2000, 12, '', '', 'F', '0', '0', 'device:query', 'admin', NOW()),
('设备导出', 2000, 13, '', '', 'F', '0', '0', 'device:export', 'admin', NOW()),
('设备拓扑查询', 2000, 14, '', '', 'F', '0', '0', 'device:topology:query', 'admin', NOW()),
('设备报修', 2000, 15, '', '', 'F', '0', '0', 'device:repair', 'admin', NOW()),
('故障报修', 2024, 4, '', '', 'F', '0', '0', 'device:fault:repair', 'admin', NOW()),
('心跳查询', 2003, 3, '', '', 'F', '0', '0', 'device:heartbeat:query', 'admin', NOW()),
('心跳导出', 2003, 4, '', '', 'F', '0', '0', 'device:heartbeat:export', 'admin', NOW()),
('设备状态日志', 2000, 16, '', '', 'F', '0', '0', 'device:statuslog:list', 'admin', NOW()),
('状态日志查询', 2000, 17, '', '', 'F', '0', '0', 'device:statuslog:query', 'admin', NOW());

-- === 配件管理 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('配件新增', 2020, 3, '', '', 'F', '0', '0', 'device:parts:add', 'admin', NOW()),
('配件修改', 2020, 4, '', '', 'F', '0', '0', 'device:parts:edit', 'admin', NOW()),
('配件删除', 2020, 5, '', '', 'F', '0', '0', 'device:parts:remove', 'admin', NOW()),
('配件查询', 2020, 6, '', '', 'F', '0', '0', 'device:parts:query', 'admin', NOW());

-- === 维修工单 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('工单查询', 2026, 3, '', '', 'F', '0', '0', 'device:repair:query', 'admin', NOW()),
('工单转派', 2026, 4, '', '', 'F', '0', '0', 'device:repair:transfer', 'admin', NOW()),
('工单接单', 2026, 5, '', '', 'F', '0', '0', 'device:repair:accept', 'admin', NOW()),
('工单拒绝', 2026, 6, '', '', 'F', '0', '0', 'device:repair:reject', 'admin', NOW());

-- === 设备端口 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('端口管理', 2000, 18, '', '', 'F', '0', '0', 'device:port:list', 'admin', NOW()),
('端口新增', 2000, 19, '', '', 'F', '0', '0', 'device:port:add', 'admin', NOW()),
('端口修改', 2000, 20, '', '', 'F', '0', '0', 'device:port:edit', 'admin', NOW()),
('端口删除', 2000, 21, '', '', 'F', '0', '0', 'device:port:remove', 'admin', NOW()),
('端口查询', 2000, 22, '', '', 'F', '0', '0', 'device:port:query', 'admin', NOW());

-- === 仪表盘 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('设备仪表盘查询', 2027, 3, '', '', 'F', '0', '0', 'dashboard:device:query', 'admin', NOW()),
('访客仪表盘查询', 2027, 4, '', '', 'F', '0', '0', 'dashboard:visitor:query', 'admin', NOW());

-- === 智能化 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('人员设备新增', 2011, 3, '', '', 'F', '0', '0', 'smart:personAccess:add', 'admin', NOW()),
('人员设备修改', 2011, 4, '', '', 'F', '0', '0', 'smart:personAccess:edit', 'admin', NOW()),
('人员设备删除', 2011, 5, '', '', 'F', '0', '0', 'smart:personAccess:remove', 'admin', NOW()),
('人员设备查询', 2011, 6, '', '', 'F', '0', '0', 'smart:personAccess:query', 'admin', NOW()),
('人员权限新增', 2012, 3, '', '', 'F', '0', '0', 'smart:personPerm:add', 'admin', NOW()),
('人员权限删除', 2012, 4, '', '', 'F', '0', '0', 'smart:personPerm:remove', 'admin', NOW()),
('车辆设备新增', 2014, 3, '', '', 'F', '0', '0', 'smart:vehicleAccess:add', 'admin', NOW()),
('车辆设备修改', 2014, 4, '', '', 'F', '0', '0', 'smart:vehicleAccess:edit', 'admin', NOW()),
('车辆设备删除', 2014, 5, '', '', 'F', '0', '0', 'smart:vehicleAccess:remove', 'admin', NOW()),
('车辆设备查询', 2014, 6, '', '', 'F', '0', '0', 'smart:vehicleAccess:query', 'admin', NOW()),
('车辆权限新增', 2015, 3, '', '', 'F', '0', '0', 'smart:vehiclePerm:add', 'admin', NOW()),
('车辆权限删除', 2015, 4, '', '', 'F', '0', '0', 'smart:vehiclePerm:remove', 'admin', NOW()),
('视频设备新增', 2017, 3, '', '', 'F', '0', '0', 'smart:videoDevice:add', 'admin', NOW()),
('视频设备修改', 2017, 4, '', '', 'F', '0', '0', 'smart:videoDevice:edit', 'admin', NOW()),
('视频设备删除', 2017, 5, '', '', 'F', '0', '0', 'smart:videoDevice:remove', 'admin', NOW()),
('视频设备查询', 2017, 6, '', '', 'F', '0', '0', 'smart:videoDevice:query', 'admin', NOW());

-- === 短信管理 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('短信日志查询', 2042, 3, '', '', 'F', '0', '0', 'sms:log:query', 'admin', NOW());

-- === 访客管理 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('访客新增', 2005, 4, '', '', 'F', '0', '0', 'visitor:appointment:add', 'admin', NOW()),
('访客修改', 2005, 5, '', '', 'F', '0', '0', 'visitor:appointment:edit', 'admin', NOW()),
('访客删除', 2005, 6, '', '', 'F', '0', '0', 'visitor:appointment:remove', 'admin', NOW()),
('访客查询', 2005, 7, '', '', 'F', '0', '0', 'visitor:appointment:query', 'admin', NOW()),
('访客审批', 2005, 8, '', '', 'F', '0', '0', 'visitor:appointment:approve', 'admin', NOW()),
('访客取消', 2005, 9, '', '', 'F', '0', '0', 'visitor:appointment:cancel', 'admin', NOW()),
('访客完成', 2005, 10, '', '', 'F', '0', '0', 'visitor:appointment:complete', 'admin', NOW()),
('访客导出', 2005, 11, '', '', 'F', '0', '0', 'visitor:appointment:export', 'admin', NOW()),
('访客记录新增', 2008, 3, '', '', 'F', '0', '0', 'visitor:log:add', 'admin', NOW()),
('访客记录修改', 2008, 4, '', '', 'F', '0', '0', 'visitor:log:edit', 'admin', NOW()),
('访客记录导出', 2008, 5, '', '', 'F', '0', '0', 'visitor:log:export', 'admin', NOW()),
('访客记录查询', 2008, 6, '', '', 'F', '0', '0', 'visitor:log:query', 'admin', NOW());

-- === 会议管理 ===
INSERT IGNORE INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time) VALUES
('会议室新增', 2032, 4, '', '', 'F', '0', '0', 'meeting:room:add', 'admin', NOW()),
('会议室修改', 2032, 5, '', '', 'F', '0', '0', 'meeting:room:edit', 'admin', NOW()),
('会议室删除', 2032, 6, '', '', 'F', '0', '0', 'meeting:room:remove', 'admin', NOW()),
('会议室查询', 2032, 7, '', '', 'F', '0', '0', 'meeting:room:query', 'admin', NOW()),
('预约新增', 2035, 4, '', '', 'F', '0', '0', 'meeting:booking:add', 'admin', NOW()),
('预约修改', 2035, 5, '', '', 'F', '0', '0', 'meeting:booking:edit', 'admin', NOW()),
('预约删除', 2035, 6, '', '', 'F', '0', '0', 'meeting:booking:remove', 'admin', NOW()),
('预约查询', 2035, 7, '', '', 'F', '0', '0', 'meeting:booking:query', 'admin', NOW()),
('预约审批', 2035, 8, '', '', 'F', '0', '0', 'meeting:booking:approve', 'admin', NOW()),
('预约拒绝', 2035, 9, '', '', 'F', '0', '0', 'meeting:booking:reject', 'admin', NOW()),
('预约取消', 2035, 10, '', '', 'F', '0', '0', 'meeting:booking:cancel', 'admin', NOW());

-- 给admin角色(1)授权所有新菜单
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) SELECT 1, menu_id FROM sys_menu WHERE perms IS NOT NULL AND perms != '' AND menu_id NOT IN (SELECT menu_id FROM sys_role_menu WHERE role_id = 1);
