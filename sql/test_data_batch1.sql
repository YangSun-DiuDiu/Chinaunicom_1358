-- 测试数据批量插入脚本 - 批次1
USE ry101;

-- 设备端口 (当前0条, 插入20条)
INSERT INTO iot_device_port (device_id, port_name, port_type, port_status, connected_device_id, remark, create_by, create_time) VALUES
(1,'GE0/0/1','GE','UP',2,'上联核心交换机','admin',NOW()),
(1,'GE0/0/2','GE','UP',3,'连接门禁控制器','admin',NOW()),
(1,'GE0/0/3','GE','UP',NULL,'备用端口','admin',NOW()),
(1,'GE0/0/4','GE','DOWN',NULL,'未使用','admin',NOW()),
(2,'GE0/0/1','GE','UP',1,'上联接入交换机','admin',NOW()),
(2,'GE0/0/2','GE','UP',NULL,'连接办公区AP','admin',NOW()),
(2,'GE0/0/3','GE','UP',NULL,'连接监控网络','admin',NOW()),
(2,'GE0/0/4','GE','DOWN',NULL,'预留端口','admin',NOW()),
(3,'Port-01','RS485','UP',NULL,'门禁读头1','admin',NOW()),
(3,'Port-02','RS485','UP',NULL,'门禁读头2','admin',NOW()),
(3,'Port-03','Wiegand','UP',NULL,'指纹识别器','admin',NOW()),
(3,'Port-04','RELAY','UP',NULL,'电锁控制','admin',NOW()),
(4,'Port-01','RS485','UP',NULL,'门禁读头1','admin',NOW()),
(4,'Port-02','RS485','UP',NULL,'门禁读头2','admin',NOW()),
(4,'Port-03','Wiegand','UP',NULL,'人脸识别终端','admin',NOW()),
(4,'Port-04','RELAY','UP',NULL,'电锁控制','admin',NOW()),
(5,'COM1','RS232','UP',NULL,'地感线圈','admin',NOW()),
(5,'COM2','RS485','UP',NULL,'车牌识别','admin',NOW()),
(6,'COM1','RS232','UP',NULL,'地感线圈','admin',NOW()),
(6,'COM2','RS485','UP',NULL,'车牌识别','admin',NOW());

-- 维修工单 (当前15条, 补5条)
INSERT INTO iot_device_repair (repair_no, device_id, device_name, device_ip, fault_description, original_responsible, original_phone, current_responsible, current_phone, status, repair_result, repair_time, has_parts, parts_desc, create_by, create_time, remark) VALUES
('WO20260617001',1,'核心交换机-01','192.168.1.1','设备端口GE0/0/4异常离线','张建国','13900001001','张建国','13900001001','PENDING',NULL,NULL,'0','','admin',NOW(),'紧急处理'),
('WO20260617002',3,'办公楼门禁控制器','192.168.2.10','门禁刷卡无响应，指示灯闪烁异常','钱小明','13900002001','钱小明','13900002001','IN_PROGRESS',NULL,NULL,'1','需更换电源模块','admin',DATE_SUB(NOW(),INTERVAL 1 DAY),''),
('WO20260617003',5,'园区入口道闸','192.168.3.5','道闸起落速度变慢','孙立华','13900003001','孙立华','13900003001','COMPLETED','更换道闸电机和弹簧，已恢复正常',NOW(),'1','道闸电机x1,弹簧x2','admin',DATE_SUB(NOW(),INTERVAL 3 DAY),'已完成维修'),
('WO20260617004',2,'接入交换机-02','192.168.1.2','设备温度过高告警，风扇转速异常','张建国','13900001001','张建国','13900001001','TRANSFERRED',NULL,NULL,'0','','admin',DATE_SUB(NOW(),INTERVAL 2 DAY),'转交运维厂商处理'),
('WO20260617005',4,'研发楼门禁控制器','192.168.2.11','人脸识别终端频繁重启','钱小明','13900002001','周志强','13900004001','IN_PROGRESS',NULL,NULL,'1','需更换人脸识别终端','admin',DATE_SUB(NOW(),INTERVAL 1 DAY),'厂家已派人');
