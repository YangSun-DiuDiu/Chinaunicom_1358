-- 镭诺电子（宁波）有限公司组织重建（花名册 2026.10）
USE `ry101-test`;
START TRANSACTION;

-- 1) admin 迁移到新根部门
UPDATE sys_user SET dept_id=1 WHERE user_name='admin' AND del_flag='0';

-- 2) smartparkAdmin 角色部门绑定改到新根
UPDATE sys_role_dept SET dept_id=1 WHERE role_id=5;

-- 3) 旧部门二维码全部清理（100-104/205-214/215 均为旧测试部门, 重建后可按需重新批量初始化）
DELETE FROM visitor_dept_qrcode;

-- 4) 删除旧测试部门树 (100 智慧园区及子部门 101-104)
DELETE FROM sys_dept WHERE dept_id IN (101,102,103,104,100);

-- 2) 新部门树
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (1, 0, '0', '镭诺电子（宁波）有限公司', 0, '刘健朝', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (2, 1, '0,1', '慈溪总部', 1, NULL, '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (3, 1, '0,1', '北京分公司', 2, NULL, '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (4, 1, '0,1', '深圳分公司', 3, NULL, '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (5, 1, '0,1', '慈鸿', 4, NULL, '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (10, 2, '0,1,2', '公司领导', 1, '刘健朝', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (11, 2, '0,1,2', '综合管理部', 2, '岑梦婷', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (12, 2, '0,1,2', '工程技术部', 3, '王欢', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (13, 2, '0,1,2', '市场营销部', 4, '徐少尉', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (14, 2, '0,1,2', '项目管理部', 5, '陈寅', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (15, 2, '0,1,2', '厂务办公室', 6, '高伟强', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (16, 2, '0,1,2', '厂长办公室', 7, '施保球', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (20, 3, '0,1,3', '总经办', 1, '岑烨', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (21, 3, '0,1,3', '项目管理部', 2, '郭清梅', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (30, 4, '0,1,4', '市场营销部', 1, '刘振州', '0', '0', 'admin', NOW());
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, status, del_flag, create_by, create_time) VALUES (40, 5, '0,1,5', '工地管理部', 1, '陈曙光', '0', '0', 'admin', NOW());

-- 3) 48 名员工用户 (角色=4 visitorAdmin, 密码=admin123)
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (2, 10, 'liujianchao', '刘健朝', '', '18600669024', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (3, 20, 'cenye', '岑烨', '', '15210685008', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (4, 11, 'cenmengting', '岑梦婷', '', '18217689986', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (5, 11, 'cenchaochao', '岑超超', '', '15158322891', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (6, 11, 'zhongchuqi', '钟楚琪', '', '15968025921', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (7, 11, 'songxincheng', '宋鑫诚', '', '13685875881', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (8, 11, 'liangwenna', '梁文娜', '', '18715135326', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (9, 11, 'sujiafa', '苏家发', '', '13857804682', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (10, 11, 'cenbaiqing', '岑柏庆', '', '15958827656', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (11, 11, 'cenjianxue', '岑建学', '', '18094559052', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (12, 11, 'zhujianli', '诸建立', '', '15658243913', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (13, 11, 'wangjiong', '王炯', '', '13685733718', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (14, 12, 'wanghuan', '王欢', '', '15560230695', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (15, 12, 'zhoumengjiao', '周梦姣', '', '13685813690', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (16, 12, 'sunbin', '孙斌', '', '13042998627', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (17, 12, 'shiyali', '时雅丽', '', '19712528621', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (18, 12, 'chendongyan', '陈董嫣', '', '18868020417', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (19, 12, 'zhaoguangwei', '赵广威', '', '15058285046', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (20, 12, 'zhanglingmin', '张灵敏', '', '18656847260', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (21, 12, 'hujiaojiao', '胡娇娇', '', '15155444815', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (22, 12, 'tongkejin', '童可金', '', '18358454581', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (23, 12, 'zhangli', '张莉', '', '18790319091', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (24, 12, 'zhangyutao', '张宇涛', '', '13677444730', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (25, 12, 'zhangjiahong', '张佳红', '', '15258281145', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (26, 12, 'diaojixiang', '刁吉祥', '', '17816404683', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (27, 12, 'huangjiazhou', '黄嘉洲', '', '18758346061', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (28, 12, 'lijiahao', '李嘉豪', '', '15195860817', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (29, 13, 'xushaowei', '徐少尉', '', '13777883940', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (30, 13, 'wangshuai', '王帅', '', '13023195288', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (31, 13, 'zhangyin', '张引', '', '13567499438', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (32, 13, 'chenwenbo', '陈文博', '', '13065896060', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (33, 13, 'chenyuwei', '陈榆威', '', '13857456603', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (34, 13, 'xutianle', '徐天乐', '', '13967892214', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (35, 30, 'liuzhenzhou', '刘振州', '', '19806699419', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (36, 30, 'wangzhiyou', '王志有', '', '13410022243', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (37, 30, 'guyuquan', '古玉全', '', '13040830374', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (38, 30, 'fanwenlu', '范文禄', '', '13584940805', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (39, 30, 'chencongqiong', '陈聪琼', '', '18620806038', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (40, 30, 'wangnana', '王娜娜', '', '13502883189', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (41, 21, 'guoqingmei', '郭清梅', '', '18613816308', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (42, 21, 'guojiaqi', '郭家其', '', '18749302939', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (43, 14, 'chenyin', '陈寅', '', '13095966710', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (44, 14, 'dingqipei', '丁齐佩', '', '13732103775', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (45, 15, 'gaoweiqiang', '高伟强', '', '18931069208', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (46, 16, 'shibaoqiu', '施保球', '', '13418540069', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (47, 16, 'chendingjian', '陈丁健', '', '18220392666', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (48, 16, 'youzhengtian', '游政天', '', '16624771645', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');
INSERT INTO sys_user (user_id, dept_id, user_name, nick_name, email, phonenumber, sex, avatar, password, status, del_flag, create_by, create_time, update_by, update_time, remark) VALUES (49, 40, 'chenshuguang', '陈曙光', '', '15705846603', '0', '', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 'admin', NOW(), 'admin', NOW(), '花名册2026.10');

-- 4) 角色绑定: 全部 visitorAdmin(4)
INSERT INTO sys_user_role (user_id, role_id) VALUES (2, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (3, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (4, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (5, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (6, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (7, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (8, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (9, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (10, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (11, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (12, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (13, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (14, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (15, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (16, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (17, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (18, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (19, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (20, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (21, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (22, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (23, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (24, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (25, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (26, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (27, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (28, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (29, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (30, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (31, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (32, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (33, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (34, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (35, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (36, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (37, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (38, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (39, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (40, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (41, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (42, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (43, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (44, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (45, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (46, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (47, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (48, 4);
INSERT INTO sys_user_role (user_id, role_id) VALUES (49, 4);

COMMIT;
