-- ============================================================
-- 部门访客登记二维码 - 建表 + 菜单权限初始化
-- 功能：为每个部门生成独立登记二维码，扫码仅可预约该部门(含子部门)人员
-- 执行目标: ry101 数据库
-- ============================================================

-- 1. 建表
CREATE TABLE IF NOT EXISTS visitor_dept_qrcode (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  dept_id       BIGINT       NOT NULL COMMENT '部门ID',
  register_code VARCHAR(64)  NOT NULL COMMENT '登记码(URL标识,唯一)',
  enabled       CHAR(1)      NOT NULL DEFAULT '1' COMMENT '是否启用(0停用 1启用)',
  create_by     VARCHAR(64)  DEFAULT '' COMMENT '创建者',
  create_time   DATETIME     DEFAULT NULL COMMENT '创建时间',
  update_by     VARCHAR(64)  DEFAULT '' COMMENT '更新者',
  update_time   DATETIME     DEFAULT NULL COMMENT '更新时间',
  del_flag      CHAR(1)      NOT NULL DEFAULT '0' COMMENT '删除标志(0存在 2删除)',
  remark        VARCHAR(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dept_id (dept_id),
  UNIQUE KEY uk_register_code (register_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门访客登记二维码配置表';

-- 2. 访客管理(2004)下新增菜单：部门登记二维码 (C型)
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('部门登记二维码', 2004, 7, 'deptQrcode', 'visitor/qrcode/index', 1, 0, 'C', '0', '0', 'visitor:qrcode:list', 'qr-code', 'admin', NOW());

-- 3. 子按钮权限 (F型)
SET @qrcode_menu_id = LAST_INSERT_ID();
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time) VALUES
('二维码查询', @qrcode_menu_id, 1, '', '', 1, 0, 'F', '0', '0', 'visitor:qrcode:query', '#', 'admin', NOW()),
('二维码新增', @qrcode_menu_id, 2, '', '', 1, 0, 'F', '0', '0', 'visitor:qrcode:add', '#', 'admin', NOW()),
('二维码修改', @qrcode_menu_id, 3, '', '', 1, 0, 'F', '0', '0', 'visitor:qrcode:edit', '#', 'admin', NOW()),
('二维码删除', @qrcode_menu_id, 4, '', '', 1, 0, 'F', '0', '0', 'visitor:qrcode:remove', '#', 'admin', NOW());
