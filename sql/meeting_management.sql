-- 会议室表
CREATE TABLE meeting_room (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL COMMENT '会议室名称',
    location VARCHAR(200) COMMENT '位置',
    capacity INT DEFAULT 10 COMMENT '容纳人数',
    equipment VARCHAR(500) COMMENT '设备清单',
    status CHAR(1) DEFAULT '0' COMMENT '状态(0启用 1停用)',
    dept_id BIGINT COMMENT '所属组织',
    create_by VARCHAR(64),
    create_time DATETIME,
    update_by VARCHAR(64),
    update_time DATETIME,
    remark VARCHAR(500),
    INDEX idx_meeting_room_dept (dept_id)
) COMMENT='会议室';

-- 会议预约表
CREATE TABLE meeting_booking (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL COMMENT '会议室ID',
    title VARCHAR(200) NOT NULL COMMENT '会议主题',
    host_name VARCHAR(64) COMMENT '主持人',
    host_phone VARCHAR(20) COMMENT '联系电话',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    attendees INT DEFAULT 1 COMMENT '参会人数',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    dept_id BIGINT COMMENT '所属组织',
    create_by VARCHAR(64),
    create_time DATETIME,
    update_by VARCHAR(64),
    update_time DATETIME,
    remark VARCHAR(500),
    INDEX idx_booking_room (room_id),
    INDEX idx_booking_time (room_id, start_time, end_time),
    INDEX idx_booking_dept (dept_id)
) COMMENT='会议预约';

-- 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('会议管理', 0, 9, 'meeting', NULL, 'M', '0', '0', NULL, 'el-icon-s-order', 'admin', NOW());
SET @meeting_parent = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('会议室管理', @meeting_parent, 1, 'room', 'meeting/room/index', 'C', '0', '0', 'meeting:room:list', '#', 'admin', NOW());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('会议预约', @meeting_parent, 2, 'booking', 'meeting/booking/index', 'C', '0', '0', 'meeting:booking:list', '#', 'admin', NOW());
