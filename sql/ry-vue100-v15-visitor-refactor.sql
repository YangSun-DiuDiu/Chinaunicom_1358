-- ============================================================
-- V15 访客管理模块重构 DDL
-- 执行目标: ry101 数据库 (1.94.26.126:3306)
-- 执行时间: 2026-06-29
-- 变更说明:
--   1. visitor_appointment 添加 del_flag（逻辑删除）
--   2. 现有行的 del_flag 设为 '0'（正常）
-- ============================================================

-- 1. visitor_appointment 添加 del_flag 列
ALTER TABLE `visitor_appointment`
  ADD COLUMN `del_flag` char(1) DEFAULT '0' COMMENT '删除标志（0存在 2删除）' AFTER `remark`;

-- 2. 确保所有现有数据 del_flag = '0'
UPDATE `visitor_appointment` SET `del_flag` = '0' WHERE `del_flag` IS NULL;
