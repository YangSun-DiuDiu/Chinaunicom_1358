-- 测试数据批量插入脚本 - 批次3: 考勤 + 人员通行设备 + 车辆通行设备
USE ry101;

-- 考勤记录 (当前0条, 插20条)
INSERT INTO attendance_record (user_name, dept_name, attendance_date, check_in_time, check_out_time, status, source, create_by, create_time, remark) VALUES
('张建国','技术部',CURDATE(),CONCAT(CURDATE(),' 08:25:00'),CONCAT(CURDATE(),' 17:35:00'),'NORMAL','MANUAL','admin',NOW(),'正常出勤'),
('钱小明','销售部',CURDATE(),CONCAT(CURDATE(),' 08:15:00'),CONCAT(CURDATE(),' 18:10:00'),'OVERTIME','MANUAL','admin',NOW(),'加班1小时'),
('孙立华','运维部',CURDATE(),CONCAT(CURDATE(),' 08:30:00'),CONCAT(CURDATE(),' 17:30:00'),'NORMAL','MANUAL','admin',NOW(),'正常出勤'),
('周志强','工程部',CURDATE(),CONCAT(CURDATE(),' 09:15:00'),CONCAT(CURDATE(),' 17:00:00'),'LATE','MANUAL','admin',NOW(),'迟到15分钟'),
('丁磊','人事部',CURDATE(),CONCAT(CURDATE(),' 08:20:00'),NULL,'NORMAL','MANUAL','admin',NOW(),'暂未签退'),
('张建国','技术部',DATE_SUB(CURDATE(),INTERVAL 1 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 1 DAY),' 08:28:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 1 DAY),' 17:32:00'),'NORMAL','MANUAL','admin',NOW(),''),
('钱小明','销售部',DATE_SUB(CURDATE(),INTERVAL 1 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 1 DAY),' 07:50:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 1 DAY),' 20:00:00'),'OVERTIME','MANUAL','admin',NOW(),'加班3小时'),
('孙立华','运维部',DATE_SUB(CURDATE(),INTERVAL 1 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 1 DAY),' 08:32:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 1 DAY),' 17:28:00'),'NORMAL','MANUAL','admin',NOW(),''),
('周志强','工程部',DATE_SUB(CURDATE(),INTERVAL 1 DAY),NULL,NULL,'ABSENT','MANUAL','admin',NOW(),'全天缺勤'),
('张建国','技术部',DATE_SUB(CURDATE(),INTERVAL 2 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 2 DAY),' 08:30:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 2 DAY),' 17:30:00'),'NORMAL','MANUAL','admin',NOW(),''),
('钱小明','销售部',DATE_SUB(CURDATE(),INTERVAL 2 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 2 DAY),' 08:10:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 2 DAY),' 17:40:00'),'NORMAL','MANUAL','admin',NOW(),''),
('孙立华','运维部',DATE_SUB(CURDATE(),INTERVAL 2 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 2 DAY),' 08:45:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 2 DAY),' 17:15:00'),'LATE','MANUAL','admin',NOW(),'迟到45分钟'),
('周志强','工程部',DATE_SUB(CURDATE(),INTERVAL 2 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 2 DAY),' 07:30:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 2 DAY),' 16:50:00'),'EARLY','MANUAL','admin',NOW(),'早退40分钟'),
('张建国','技术部',DATE_SUB(CURDATE(),INTERVAL 3 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 3 DAY),' 08:22:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 3 DAY),' 17:38:00'),'NORMAL','MANUAL','admin',NOW(),''),
('钱小明','销售部',DATE_SUB(CURDATE(),INTERVAL 3 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 3 DAY),' 08:18:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 3 DAY),' 17:45:00'),'NORMAL','MANUAL','admin',NOW(),''),
('孙立华','运维部',DATE_SUB(CURDATE(),INTERVAL 3 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 3 DAY),' 08:30:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 3 DAY),' 17:30:00'),'NORMAL','MANUAL','admin',NOW(),''),
('周志强','工程部',DATE_SUB(CURDATE(),INTERVAL 3 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 3 DAY),' 08:10:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 3 DAY),' 18:30:00'),'OVERTIME','MANUAL','admin',NOW(),'加班1.5小时'),
('丁磊','人事部',DATE_SUB(CURDATE(),INTERVAL 4 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 4 DAY),' 08:25:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 4 DAY),' 17:35:00'),'NORMAL','MANUAL','admin',NOW(),''),
('郝思嘉','行政部',DATE_SUB(CURDATE(),INTERVAL 4 DAY),CONCAT(DATE_SUB(CURDATE(),INTERVAL 4 DAY),' 08:50:00'),CONCAT(DATE_SUB(CURDATE(),INTERVAL 4 DAY),' 17:00:00'),'LATE','MANUAL','admin',NOW(),'迟到50分钟'),
('吕良','技术部',DATE_SUB(CURDATE(),INTERVAL 5 DAY),NULL,NULL,'ABSENT','MANUAL','admin',NOW(),'请假一天');

-- 人员通行设备 (当前0条, 插20条)
INSERT INTO iot_person_access_device (device_name, device_brand, ip_address, port, username, password, location, status, ping_interval, create_by, create_time, remark) VALUES
('办公楼人脸门禁-01','HIKVISION','192.168.10.11',80,'admin','hik12345','办公楼一楼东门','ONLINE',3,'admin',NOW(),''),
('办公楼人脸门禁-02','HIKVISION','192.168.10.12',80,'admin','hik12345','办公楼一楼西门','ONLINE',3,'admin',NOW(),''),
('办公楼刷卡门禁-01','HIKVISION','192.168.10.13',80,'admin','hik12345','办公楼二楼东门','ONLINE',3,'admin',NOW(),''),
('办公楼刷卡门禁-02','HIKVISION','192.168.10.14',80,'admin','hik12345','办公楼二楼西门','ONLINE',3,'admin',NOW(),''),
('研发楼人脸门禁-01','DAHUA','192.168.10.21',80,'admin','dahua123','研发楼一楼大厅','ONLINE',3,'admin',NOW(),''),
('研发楼人脸门禁-02','DAHUA','192.168.10.22',80,'admin','dahua123','研发楼二楼东','ONLINE',3,'admin',NOW(),''),
('研发楼刷卡门禁-01','DAHUA','192.168.10.23',80,'admin','dahua123','研发楼二楼西','OFFLINE',3,'admin',NOW(),'正在维修'),
('食堂人脸门禁-01','HIKVISION','192.168.10.31',80,'admin','hik12345','员工食堂入口','ONLINE',3,'admin',NOW(),''),
('食堂人脸门禁-02','HIKVISION','192.168.10.32',80,'admin','hik12345','员工食堂出口','ONLINE',3,'admin',NOW(),''),
('宿舍楼人脸门禁-01','DAHUA','192.168.10.41',80,'admin','dahua123','宿舍楼A栋入口','ONLINE',3,'admin',NOW(),''),
('宿舍楼人脸门禁-02','DAHUA','192.168.10.42',80,'admin','dahua123','宿舍楼B栋入口','ONLINE',3,'admin',NOW(),''),
('数据中心门禁-01','HIKVISION','192.168.10.51',80,'admin','hik12345','数据中心机房入口','ONLINE',3,'admin',NOW(),'高安全区域'),
('数据中心门禁-02','HIKVISION','192.168.10.52',80,'admin','hik12345','数据中心机房出口','ONLINE',3,'admin',NOW(),'高安全区域'),
('行政楼指纹门禁-01','OTHER','192.168.10.61',80,'admin','other123','行政楼一楼','ONLINE',3,'admin',NOW(),'指纹识别'),
('行政楼指纹门禁-02','OTHER','192.168.10.62',80,'admin','other123','行政楼二楼','ONLINE',3,'admin',NOW(),'指纹识别'),
('西门岗亭门禁','HIKVISION','192.168.10.71',80,'admin','hik12345','园区西门保安亭','ONLINE',3,'admin',NOW(),''),
('南门岗亭门禁','HIKVISION','192.168.10.72',80,'admin','hik12345','园区南门保安亭','ONLINE',3,'admin',NOW(),''),
('北门岗亭门禁','HIKVISION','192.168.10.73',80,'admin','hik12345','园区北门保安亭','OFFLINE',3,'admin',NOW(),'网络故障待修'),
('仓库人脸门禁-01','DAHUA','192.168.10.81',80,'admin','dahua123','物资仓库入口','ONLINE',3,'admin',NOW(),''),
('停车场人脸门禁-01','HIKVISION','192.168.10.91',80,'admin','hik12345','地下停车场入口','ONLINE',3,'admin',NOW(),'');

-- 车辆通行设备 (当前1条, 补19条)
INSERT INTO iot_vehicle_access_device (device_name, device_brand, ip_address, port, username, password, location, status, ping_interval, create_by, create_time, remark) VALUES
('南门入口车牌识别-01','HIKVISION','192.168.20.11',80,'admin','hik12345','园区南门入口-1号车道','ONLINE',3,'admin',NOW(),''),
('南门入口车牌识别-02','HIKVISION','192.168.20.12',80,'admin','hik12345','园区南门入口-2号车道','ONLINE',3,'admin',NOW(),''),
('南门出口车牌识别-01','HIKVISION','192.168.20.13',80,'admin','hik12345','园区南门出口-1号车道','ONLINE',3,'admin',NOW(),''),
('南门出口车牌识别-02','HIKVISION','192.168.20.14',80,'admin','hik12345','园区南门出口-2号车道','ONLINE',3,'admin',NOW(),''),
('北门入口车牌识别','DAHUA','192.168.20.21',80,'admin','dahua123','园区北门入口','ONLINE',3,'admin',NOW(),''),
('北门出口车牌识别','DAHUA','192.168.20.22',80,'admin','dahua123','园区北门出口','ONLINE',3,'admin',NOW(),''),
('西门入口车牌识别','HIKVISION','192.168.20.31',80,'admin','hik12345','园区西门入口','ONLINE',3,'admin',NOW(),''),
('西门出口车牌识别','HIKVISION','192.168.20.32',80,'admin','hik12345','园区西门出口','ONLINE',3,'admin',NOW(),''),
('地下车库入口车牌识别','DAHUA','192.168.20.41',80,'admin','dahua123','地下车库B1入口','ONLINE',3,'admin',NOW(),''),
('地下车库出口车牌识别','DAHUA','192.168.20.42',80,'admin','dahua123','地下车库B1出口','ONLINE',3,'admin',NOW(),''),
('VIP入口车牌识别','HIKVISION','192.168.20.51',80,'admin','hik12345','VIP停车场入口','ONLINE',3,'admin',NOW(),'VIP专用'),
('VIP出口车牌识别','HIKVISION','192.168.20.52',80,'admin','hik12345','VIP停车场出口','ONLINE',3,'admin',NOW(),'VIP专用'),
('员工通道入口车牌识别','DAHUA','192.168.20.61',80,'admin','dahua123','员工专用通道入口','ONLINE',3,'admin',NOW(),''),
('员工通道出口车牌识别','DAHUA','192.168.20.62',80,'admin','dahua123','员工专用通道出口','ONLINE',3,'admin',NOW(),''),
('货车通道入口车牌识别','HIKVISION','192.168.20.71',80,'admin','hik12345','物流通道入口','OFFLINE',3,'admin',NOW(),'设备检修中'),
('货车通道出口车牌识别','HIKVISION','192.168.20.72',80,'admin','hik12345','物流通道出口','ONLINE',3,'admin',NOW(),''),
('临时出入口车牌识别','OTHER','192.168.20.81',80,'admin','other123','临时停车场入口','ONLINE',3,'admin',NOW(),''),
('东门辅道车牌识别','HIKVISION','192.168.20.91',80,'admin','hik12345','园区东门辅道','ONLINE',3,'admin',NOW(),''),
('西南角车牌识别','DAHUA','192.168.20.101',80,'admin','dahua123','园区西南角','ONLINE',3,'admin',NOW(),'备用通道');
