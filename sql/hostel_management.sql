-- 公寓房源管理 - 建表 + 菜单
CREATE TABLE apartment_info (
    apartment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    apartment_name VARCHAR(100) NOT NULL COMMENT '公寓名称',
    address VARCHAR(200) COMMENT '地址',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    area_desc VARCHAR(500) COMMENT '区域描述',
    dept_id BIGINT COMMENT '所属组织',
    create_by VARCHAR(64), create_time DATETIME,
    update_by VARCHAR(64), update_time DATETIME,
    remark VARCHAR(500),
    INDEX idx_apt_dept (dept_id)
) COMMENT='公寓信息';

CREATE TABLE room_info (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_code VARCHAR(20) NOT NULL COMMENT '房间编号',
    apartment_id BIGINT COMMENT '公寓ID',
    building VARCHAR(50) COMMENT '楼栋',
    floor VARCHAR(50) COMMENT '楼层',
    unit_type VARCHAR(50) COMMENT '户型',
    area DECIMAL(10,2) COMMENT '面积(m²)',
    rent_start DATE COMMENT '租期开始',
    rent_end DATE COMMENT '租期结束',
    tenant_count INT DEFAULT 0 COMMENT '合租人数',
    door_type VARCHAR(20) DEFAULT 'NFC' COMMENT 'ONLINE/NFC',
    device_status VARCHAR(20) DEFAULT 'UNBOUND' COMMENT 'BOUND/UNBOUND',
    status VARCHAR(20) DEFAULT 'GREEN' COMMENT 'GREEN/BLUE/YELLOW/ORANGE/RED/GRAY',
    dept_id BIGINT COMMENT '所属组织',
    create_by VARCHAR(64), create_time DATETIME,
    update_by VARCHAR(64), update_time DATETIME,
    remark VARCHAR(500),
    INDEX idx_room_apt (apartment_id),
    INDEX idx_room_status (status),
    INDEX idx_room_dept (dept_id)
) COMMENT='房间信息';

-- 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('公寓房源管理', 0, 10, 'hostel', NULL, 'M', '0', '0', NULL, 'admin', NOW());
SET @hostel_parent = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('公寓信息', @hostel_parent, 1, 'apartment', 'hostel/apartment/index', 'C', '0', '0', 'hostel:apartment:list', 'admin', NOW());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('房间管理-表格', @hostel_parent, 2, 'room', 'hostel/room/index', 'C', '0', '0', 'hostel:room:list', 'admin', NOW());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('房间卡片视图', @hostel_parent, 3, 'room/card', 'hostel/room/card', 'C', '0', '0', 'hostel:room:card', 'admin', NOW());

-- 租客信息表
CREATE TABLE tenant_info (
    tenant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_name VARCHAR(64) NOT NULL COMMENT '姓名',
    id_card VARCHAR(18) COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '手机号',
    gender CHAR(1) DEFAULT 'M' COMMENT 'M/F',
    room_id BIGINT COMMENT 'FK→room_info',
    check_in_date DATE COMMENT '入住日期',
    check_out_date DATE COMMENT '退租日期',
    rent_start DATE COMMENT '租期开始',
    rent_end DATE COMMENT '租期结束',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL/CHECKED_OUT',
    openid VARCHAR(64) COMMENT '微信openid',
    push_enabled CHAR(1) DEFAULT '1' COMMENT '推送开关',
    dept_id BIGINT, create_by VARCHAR(64), create_time DATETIME,
    update_by VARCHAR(64), update_time DATETIME, remark VARCHAR(500)
) COMMENT='租客信息';

-- 租客管理菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('租客管理', 0, 11, 'tenant', NULL, 'M', '0', '0', NULL, 'admin', NOW());
SET @tparent = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('租客档案', @tparent, 1, 'list', 'hostel/tenant/index', 'C', '0', '0', 'hostel:tenant:list', 'admin', NOW());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('入住办理', @tparent, 2, 'checkin', 'hostel/tenant/checkin', 'C', '0', '0', 'hostel:tenant:checkin', 'admin', NOW());
