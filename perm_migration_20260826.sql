-- ============================================================
-- 权限模型重构迁移 SQL (生产 ry101 / 1.94.26.126)
-- 日期: 2026-08-26
-- 前置备份: bak_sys_role_20260826 / bak_sys_role_menu_20260826 / bak_sys_user_role_20260826
-- 约束: 所有业务账号必须挂固定角色; 非admin禁止授予 admin(1)/园区管理员(100) 角色
-- ============================================================

-- 1. 设备管理员(3): 追加 用户/部门管理只读 (system:user:list/query + system:dept:list/query)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 100), (3, 1000), (3, 103), (3, 1016);

-- 2. 访客管理员(4): 追加 二维码管理; 移除 角色/菜单/岗位/参数管理 + 用户写权限 + 部门写权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(4, 2181), (4, 2182), (4, 2183), (4, 2184), (4, 2185);
DELETE FROM sys_role_menu WHERE role_id = 4 AND menu_id IN (
  101, 102, 104, 106,
  1001, 1002, 1003, 1004, 1005, 1006,
  1007, 1008, 1009, 1010, 1011,
  1012, 1013, 1014, 1015,
  1017, 1018, 1019,
  1020, 1021, 1022, 1023, 1024,
  1030, 1031, 1032, 1033, 1034
);

-- 3. 园区管理员(100): 移除 角色管理(101)+角色按钮(1007-1011)+用户导入(1005)+部门写权限(1017-1019)
DELETE FROM sys_role_menu WHERE role_id = 100 AND menu_id IN (
  101, 1005,
  1007, 1008, 1009, 1010, 1011,
  1017, 1018, 1019
);

-- 4. 新增 会议管理员 角色 (role_id 5)
INSERT INTO sys_role (role_id, role_name, role_key, role_sort, data_scope, status, del_flag, create_by, create_time, remark)
VALUES (5, '会议管理员', 'meetingAdmin', 5, '1', '0', '0', 'admin', NOW(), '会议模块全量+用户/部门只读');
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(5, 100), (5, 1000), (5, 103), (5, 1016),
(5, 2031), (5, 2032), (5, 2034), (5, 2035), (5, 2036), (5, 2180),
(5, 2099), (5, 2100), (5, 2101), (5, 2102), (5, 2103), (5, 2104),
(5, 2105), (5, 2106), (5, 2107), (5, 2108), (5, 2109);

-- 5. 账号角色迁移
-- zhaojg(10): 生产部管理员(10) -> 设备管理员(3)+会议管理员(5)
DELETE FROM sys_user_role WHERE user_id = 10 AND role_id = 10;
INSERT INTO sys_user_role (user_id, role_id) VALUES (10, 3), (10, 5);
-- zhouzq(13): 工程部管理员(13) -> 设备管理员(3)
DELETE FROM sys_user_role WHERE user_id = 13 AND role_id = 13;
INSERT INTO sys_user_role (user_id, role_id) VALUES (13, 3);
-- sunlh(12): 行政部管理员(12) -> 访客管理员(4)
DELETE FROM sys_user_role WHERE user_id = 12 AND role_id = 12;
INSERT INTO sys_user_role (user_id, role_id) VALUES (12, 4);

-- 6. 删除部门管理员角色 10/11/12/13 (成员已清空)
DELETE FROM sys_role_menu WHERE role_id IN (10, 11, 12, 13);
DELETE FROM sys_role WHERE role_id IN (10, 11, 12, 13);

-- 7. 校验查询(执行后人工确认)
SELECT r.role_id, r.role_name, r.role_key, COUNT(rm.menu_id) AS menu_cnt
FROM sys_role r
LEFT JOIN sys_role_menu rm ON r.role_id = rm.role_id
WHERE r.del_flag = '0'
GROUP BY r.role_id, r.role_name, r.role_key
ORDER BY r.role_id;

SELECT ur.user_id, u.user_name, r.role_key
FROM sys_user_role ur
LEFT JOIN sys_user u ON u.user_id = ur.user_id
LEFT JOIN sys_role r ON r.role_id = ur.role_id
WHERE u.user_name IN ('zhaojg', 'zhouzq', 'sunlh', 'chejin')
ORDER BY u.user_name;
