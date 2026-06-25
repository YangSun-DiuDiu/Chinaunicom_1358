-- ============================================
-- V10 人员管理增强：支持钉钉/企业微信组织架构导入
-- ============================================

-- sys_user 增加外部系统关联字段
ALTER TABLE `sys_user`
    ADD COLUMN `external_user_id` VARCHAR(100) DEFAULT '' COMMENT '外部系统用户ID(钉钉userId/企业微信UserId)' AFTER `user_name`,
    ADD COLUMN `external_source`   VARCHAR(20)  DEFAULT '' COMMENT '外部来源(DINGTALK/WECHAT)' AFTER `external_user_id`,
    ADD COLUMN `external_dept_id`  VARCHAR(100) DEFAULT '' COMMENT '外部系统部门ID' AFTER `dept_id`,
    ADD COLUMN `position`          VARCHAR(100) DEFAULT '' COMMENT '职位/岗位名称' AFTER `email`,
    ADD COLUMN `hire_date`         DATE         DEFAULT NULL COMMENT '入职日期' AFTER `position`;

-- sys_dept 增加外部系统关联字段
ALTER TABLE `sys_dept`
    ADD COLUMN `external_dept_id`   VARCHAR(100) DEFAULT '' COMMENT '外部系统部门ID(钉钉dept_id/企业微信party_id)' AFTER `dept_name`,
    ADD COLUMN `external_source`    VARCHAR(20)  DEFAULT '' COMMENT '外部来源(DINGTALK/WECHAT)' AFTER `external_dept_id`,
    ADD COLUMN `external_parent_id` VARCHAR(100) DEFAULT '' COMMENT '外部系统父部门ID' AFTER `external_source`;
