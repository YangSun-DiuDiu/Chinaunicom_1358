-- ============================================================
-- 镭诺电子（宁波）有限公司 - 组织架构与账号权限配置
-- 数据库: ry-vue100
-- 说明: 创建4个部门、部门管理员账号，配置功能权限与数据隔离
-- ============================================================

-- ============================================================
-- 1. 更新公司根部门名称
-- ============================================================
UPDATE `sys_dept` SET `dept_name` = '镭诺电子（宁波）有限公司', `leader` = '系统管理员', `update_by` = 'admin', `update_time` = NOW() WHERE `dept_id` = 100;

-- 清除原有演示子部门
DELETE FROM `sys_dept` WHERE `dept_id` > 100;

-- ============================================================
-- 2. 创建4个部门（ID: 101~104）
-- ============================================================
INSERT INTO `sys_dept` (`dept_id`, `parent_id`, `ancestors`, `dept_name`, `order_num`, `leader`, `status`, `del_flag`, `create_by`, `create_time`) VALUES
(101, 100, '0,100', '生产部', 1, '', '0', '0', 'admin', NOW()),
(102, 100, '0,100', '财务部', 2, '', '0', '0', 'admin', NOW()),
(103, 100, '0,100', '行政部', 3, '', '0', '0', 'admin', NOW()),
(104, 100, '0,100', '工程部', 4, '', '0', '0', 'admin', NOW());

-- ============================================================
-- 3. 更新主账号 admin 所属部门（挂到根部门下）
-- ============================================================
UPDATE `sys_user` SET `dept_id` = 100 WHERE `user_id` = 1;

-- ============================================================
-- 4. 创建部门管理员角色（data_scope=3 本部门数据权限）
-- ============================================================
INSERT INTO `sys_role` (`role_id`, `role_name`, `role_key`, `role_sort`, `data_scope`, `menu_check_strictly`, `dept_check_strictly`, `status`, `del_flag`, `create_by`, `create_time`, `remark`) VALUES
(10, '生产部管理员', 'prodAdmin', 10, '3', 1, 1, '0', '0', 'admin', NOW(), '生产部管理角色'),
(11, '财务部管理员', 'finAdmin',  11, '3', 1, 1, '0', '0', 'admin', NOW(), '财务部管理角色 - 设备管理 + 访客审批'),
(12, '行政部管理员', 'hrAdmin',   12, '3', 1, 1, '0', '0', 'admin', NOW(), '行政部管理角色 - 访客审批'),
(13, '工程部管理员', 'engAdmin',  13, '3', 1, 1, '0', '0', 'admin', NOW(), '工程部管理角色 - 设备管理');

-- ============================================================
-- 5. 创建部门管理员账号（初始密码: admin123）
--    bcrypt: $2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2
-- ============================================================
INSERT INTO `sys_user` (`user_id`, `dept_id`, `user_name`, `nick_name`, `user_type`, `password`, `status`, `del_flag`, `create_by`, `create_time`, `remark`) VALUES
(10, 101, 'zhaojg', '赵建国', '00', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '生产部管理员'),
(11, 102, 'qianxm', '钱晓明', '00', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '财务部管理员'),
(12, 103, 'sunlh',  '孙丽华', '00', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '行政部管理员'),
(13, 104, 'zhouzq', '周志强', '00', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), '工程部管理员');

-- ============================================================
-- 6. 用户-角色关联（每个部门管理员绑定自己的角色）
-- ============================================================
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(10, 10),  -- 赵建国 → 生产部管理员
(11, 11),  -- 钱晓明 → 财务部管理员
(12, 12),  -- 孙丽华 → 行政部管理员
(13, 13);  -- 周志强 → 工程部管理员

-- ============================================================
-- 7. 角色-部门关联（数据权限范围：仅本部门）
-- ============================================================
INSERT INTO `sys_role_dept` (`role_id`, `dept_id`) VALUES
(10, 101),  -- 生产部管理员 → 生产部
(11, 102),  -- 财务部管理员 → 财务部
(12, 103),  -- 行政部管理员 → 行政部
(13, 104);  -- 工程部管理员 → 工程部

-- ============================================================
-- 8. 角色-菜单权限分配
--    使用 INSERT ... SELECT 子查询动态获取菜单ID
-- ============================================================

-- -----------------------------------------------------------
-- 8a. 工程部管理员（engAdmin, role_id=13）：设备管理全部菜单
--     perms: device:list:list, device:topology:view, device:heartbeat:list
-- -----------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 13, `menu_id` FROM `sys_menu` WHERE `perms` = 'device:list:list'
UNION ALL
SELECT 13, `menu_id` FROM `sys_menu` WHERE `perms` = 'device:topology:view'
UNION ALL
SELECT 13, `menu_id` FROM `sys_menu` WHERE `perms` = 'device:heartbeat:list';

-- -----------------------------------------------------------
-- 8b. 行政部管理员（hrAdmin, role_id=12）：访客管理全部菜单
--     perms: visitor:appointment:list, visitor:approval:list,
--            visitor:register:add, visitor:log:list
-- -----------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 12, `menu_id` FROM `sys_menu` WHERE `perms` = 'visitor:appointment:list'
UNION ALL
SELECT 12, `menu_id` FROM `sys_menu` WHERE `perms` = 'visitor:approval:list'
UNION ALL
SELECT 12, `menu_id` FROM `sys_menu` WHERE `perms` = 'visitor:register:add'
UNION ALL
SELECT 12, `menu_id` FROM `sys_menu` WHERE `perms` = 'visitor:log:list';

-- -----------------------------------------------------------
-- 8c. 财务部管理员（finAdmin, role_id=11）：设备管理 + 访客管理
-- -----------------------------------------------------------
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`)
SELECT 11, `menu_id` FROM `sys_menu` WHERE `perms` = 'device:list:list'
UNION ALL
SELECT 11, `menu_id` FROM `sys_menu` WHERE `perms` = 'device:topology:view'
UNION ALL
SELECT 11, `menu_id` FROM `sys_menu` WHERE `perms` = 'device:heartbeat:list'
UNION ALL
SELECT 11, `menu_id` FROM `sys_menu` WHERE `perms` = 'visitor:appointment:list'
UNION ALL
SELECT 11, `menu_id` FROM `sys_menu` WHERE `perms` = 'visitor:approval:list'
UNION ALL
SELECT 11, `menu_id` FROM `sys_menu` WHERE `perms` = 'visitor:register:add'
UNION ALL
SELECT 11, `menu_id` FROM `sys_menu` WHERE `perms` = 'visitor:log:list';

-- -----------------------------------------------------------
-- 8d. 生产部管理员（prodAdmin, role_id=10）：仅首页，无额外业务菜单
--     无需分配（Dashboard 首页不需要权限即可访问）
-- -----------------------------------------------------------

-- ============================================================
-- 9. 清理原有演示数据
-- ============================================================
-- 删除原演示用户
DELETE FROM `sys_user` WHERE `user_id` = 2;
DELETE FROM `sys_user_role` WHERE `user_id` = 2;
DELETE FROM `sys_user_post` WHERE `user_id` = 2;

-- 删除原普通角色
DELETE FROM `sys_role` WHERE `role_id` = 2;
DELETE FROM `sys_role_menu` WHERE `role_id` = 2;
DELETE FROM `sys_role_dept` WHERE `role_id` = 2;

-- ============================================================
-- 配置完成！账户汇总
-- ============================================================
-- ┌──────────┬──────────┬──────────┬──────────────┬─────────────────────┐
-- │ 用户名   │ 密码     │ 昵称     │ 所属部门     │ 权限                │
-- ├──────────┼──────────┼──────────┼──────────────┼─────────────────────┤
-- │ admin    │ admin123 │ 超级管理 │ 镭诺电子     │ 全部权限（超级管理）│
-- │ zhaojg   │ admin123 │ 赵建国   │ 生产部       │ 首页（无业务权限）  │
-- │ qianxm   │ admin123 │ 钱晓明   │ 财务部       │ 设备管理 + 访客审批 │
-- │ sunlh    │ admin123 │ 孙丽华   │ 行政部       │ 访客审批            │
-- │ zhouzq   │ admin123 │ 周志强   │ 工程部       │ 设备管理            │
-- └──────────┴──────────┴──────────┴──────────────┴─────────────────────┘
--
-- 数据隔离规则:
--   除 admin 外，各账号 data_scope=3（本部门数据权限）
--   仅能查看/操作本部门数据，无法操作其他部门数据
-- ============================================================
