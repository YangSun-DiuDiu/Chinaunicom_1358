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
