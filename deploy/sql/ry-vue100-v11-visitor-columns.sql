-- ============================================================
-- V11 访客管理表补充缺失字段
-- 执行目标: ry101 数据库 (1.94.26.126:3306)
-- 执行时间: 2026-06-17
-- ============================================================

-- 1. visitor_appointment 补充字段
ALTER TABLE `visitor_appointment`
  ADD COLUMN `pass_code`  varchar(20)  DEFAULT ''  COMMENT '通行码'     AFTER `sms_content`,
  ADD COLUMN `has_car`    char(1)      DEFAULT '0' COMMENT '是否开车(0=否 1=是)' AFTER `pass_code`,
  ADD COLUMN `car_plate`  varchar(20)  DEFAULT ''  COMMENT '车牌号'     AFTER `has_car`,
  ADD COLUMN `has_goods`  char(1)      DEFAULT '0' COMMENT '是否携带物资(0=否 1=是)' AFTER `car_plate`,
  ADD COLUMN `goods_desc` varchar(200) DEFAULT ''  COMMENT '携带物资说明' AFTER `has_goods`;

-- 2. visitor_log 补充字段
ALTER TABLE `visitor_log`
  ADD COLUMN `pass_code`  varchar(20)  DEFAULT ''  COMMENT '通行码'     AFTER `register_type`,
  ADD COLUMN `has_car`    char(1)      DEFAULT '0' COMMENT '是否开车(0=否 1=是)' AFTER `pass_code`,
  ADD COLUMN `car_plate`  varchar(20)  DEFAULT ''  COMMENT '车牌号'     AFTER `has_car`,
  ADD COLUMN `has_goods`  char(1)      DEFAULT '0' COMMENT '是否携带物资(0=否 1=是)' AFTER `car_plate`,
  ADD COLUMN `goods_desc` varchar(200) DEFAULT ''  COMMENT '携带物资说明' AFTER `has_goods`;
