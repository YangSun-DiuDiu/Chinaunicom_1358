-- 测试数据批量插入脚本 - 批次4: 视频监控 + 配件 + 配件使用 + 人员车辆绑定
USE ry101;

-- 视频监控设备 (当前1条, 补19条)
INSERT INTO iot_video_device (device_name, device_brand, ip_address, rtsp_port, rtsp_url, username, password, channel, stream_type, location, map_x, map_y, status, ping_interval, create_by, create_time, remark) VALUES
('办公楼大厅球机-01','HIKVISION','192.168.30.11',554,'rtsp://192.168.30.11:554/stream1','admin','hik12345',1,'MAIN','办公楼一楼大厅',120.5,80.3,'ONLINE',3,'admin',NOW(),''),
('办公楼走廊枪机-01','HIKVISION','192.168.30.12',554,'rtsp://192.168.30.12:554/stream1','admin','hik12345',1,'MAIN','办公楼二楼走廊',125.0,82.0,'ONLINE',3,'admin',NOW(),''),
('办公楼走廊枪机-02','HIKVISION','192.168.30.13',554,'rtsp://192.168.30.13:554/stream1','admin','hik12345',1,'MAIN','办公楼三楼走廊',125.0,84.0,'ONLINE',3,'admin',NOW(),''),
('研发楼大厅球机-01','DAHUA','192.168.30.21',554,'rtsp://192.168.30.21:554/stream1','admin','dahua123',1,'MAIN','研发楼一楼大厅',200.0,80.0,'ONLINE',3,'admin',NOW(),''),
('研发楼走廊枪机-01','DAHUA','192.168.30.22',554,'rtsp://192.168.30.22:554/stream1','admin','dahua123',1,'MAIN','研发楼二楼走廊',200.0,82.0,'ONLINE',3,'admin',NOW(),''),
('园区南门全景-01','HIKVISION','192.168.30.31',554,'rtsp://192.168.30.31:554/stream1','admin','hik12345',1,'MAIN','园区南门',50.0,20.0,'ONLINE',3,'admin',NOW(),''),
('园区北门全景-01','HIKVISION','192.168.30.32',554,'rtsp://192.168.30.32:554/stream1','admin','hik12345',1,'MAIN','园区北门',50.0,180.0,'ONLINE',3,'admin',NOW(),''),
('园区西门全景-01','HIKVISION','192.168.30.33',554,'rtsp://192.168.30.33:554/stream1','admin','hik12345',1,'MAIN','园区西门',10.0,100.0,'OFFLINE',3,'admin',NOW(),'正在维修'),
('停车场入口枪机-01','DAHUA','192.168.30.41',554,'rtsp://192.168.30.41:554/stream1','admin','dahua123',1,'MAIN','地下停车场B1入口',150.0,50.0,'ONLINE',3,'admin',NOW(),''),
('停车场内部枪机-01','DAHUA','192.168.30.42',554,'rtsp://192.168.30.42:554/stream1','admin','dahua123',1,'SUB','地下停车场B1-A区',160.0,60.0,'ONLINE',3,'admin',NOW(),''),
('停车场内部枪机-02','DAHUA','192.168.30.43',554,'rtsp://192.168.30.43:554/stream1','admin','dahua123',1,'SUB','地下停车场B1-B区',170.0,70.0,'ONLINE',3,'admin',NOW(),''),
('食堂全景球机-01','HIKVISION','192.168.30.51',554,'rtsp://192.168.30.51:554/stream1','admin','hik12345',1,'MAIN','员工食堂大厅',100.0,100.0,'ONLINE',3,'admin',NOW(),''),
('宿舍楼入口枪机-01','DAHUA','192.168.30.61',554,'rtsp://192.168.30.61:554/stream1','admin','dahua123',1,'MAIN','宿舍楼A栋入口',80.0,150.0,'ONLINE',3,'admin',NOW(),''),
('宿舍楼入口枪机-02','DAHUA','192.168.30.62',554,'rtsp://192.168.30.62:554/stream1','admin','dahua123',1,'MAIN','宿舍楼B栋入口',90.0,155.0,'ONLINE',3,'admin',NOW(),''),
('数据中心球机-01','HIKVISION','192.168.30.71',554,'rtsp://192.168.30.71:554/stream1','admin','hik12345',1,'MAIN','数据中心机房-机柜区',130.0,90.0,'ONLINE',3,'admin',NOW(),'高安全区域'),
('数据中心球机-02','HIKVISION','192.168.30.72',554,'rtsp://192.168.30.72:554/stream1','admin','hik12345',1,'MAIN','数据中心机房-配电区',135.0,95.0,'ONLINE',3,'admin',NOW(),'高安全区域'),
('园区围墙周界-01','HIKVISION','192.168.30.81',554,'rtsp://192.168.30.81:554/stream1','admin','hik12345',1,'MAIN','园区东围墙',180.0,30.0,'ONLINE',3,'admin',NOW(),'周界防范'),
('园区围墙周界-02','HIKVISION','192.168.30.82',554,'rtsp://192.168.30.82:554/stream1','admin','hik12345',1,'MAIN','园区西围墙',0.0,80.0,'ONLINE',3,'admin',NOW(),'周界防范'),
('仓库内部球机-01','DAHUA','192.168.30.91',554,'rtsp://192.168.30.91:554/stream1','admin','dahua123',1,'MAIN','物资仓库内',70.0,170.0,'ONLINE',3,'admin',NOW(),'');

-- 配件管理 (当前10条, 补10条)
INSERT INTO iot_parts (part_name, part_code, part_model, unit, quantity, alert_quantity, price, create_by, create_time, remark) VALUES
('光纤跳线-SC','PARTS-FIBER-SC','SC-SC单模3米','根',50,10,25.00,'admin',NOW(),'网络配件'),
('水晶头-RJ45','PARTS-RJ45','超五类屏蔽RJ45','盒',30,5,80.00,'admin',NOW(),'网络配件'),
('无线AP面板','PARTS-AP','TP-LINK AP1900GI','个',8,3,350.00,'admin',NOW(),'网络设备'),
('道闸弹簧','PARTS-SPRING','JSK-600弹簧套件','套',5,2,120.00,'admin',NOW(),'道闸配件'),
('道闸电机','PARTS-MOTOR','DC24V直流电机','个',3,1,850.00,'admin',NOW(),'道闸配件'),
('门禁电源模块','PARTS-POWER','12V5A门禁电源','个',15,5,65.00,'admin',NOW(),'门禁配件'),
('人脸识别终端','PARTS-FACE-TERM','DS-K1T607MFW','台',2,1,2800.00,'admin',NOW(),'门禁配件'),
('监控电源适配器','PARTS-CAM-PWR','12V3A监控电源','个',20,5,35.00,'admin',NOW(),'监控配件'),
('网线-超五类','PARTS-CAT5E','超五类非屏蔽网线305米','箱',10,3,580.00,'admin',NOW(),'综合布线'),
('机柜散热风扇','PARTS-FAN','12cm机柜散热风扇','个',12,3,45.00,'admin',NOW(),'机房配件');

-- 配件使用记录 (当前0条, 插20条)
INSERT INTO iot_parts_usage (repair_id, part_id, part_name, quantity, used_by, used_time, create_by, create_time, remark) VALUES
(3,4,'道闸弹簧',2,'孙立华',NOW(),'admin',NOW(),'维修道闸使用'),
(3,5,'道闸电机',1,'孙立华',NOW(),'admin',NOW(),'维修道闸更换电机'),
(1,6,'门禁电源模块',1,'钱小明',DATE_SUB(NOW(),INTERVAL 1 DAY),'admin',DATE_SUB(NOW(),INTERVAL 1 DAY),'门禁维修'),
(2,7,'人脸识别终端',1,'周志强',DATE_SUB(NOW(),INTERVAL 1 DAY),'admin',DATE_SUB(NOW(),INTERVAL 1 DAY),'更换人脸设备'),
(1,1,'光纤跳线-SC',2,'张建国',DATE_SUB(NOW(),INTERVAL 2 DAY),'admin',DATE_SUB(NOW(),INTERVAL 2 DAY),''),
(2,8,'监控电源适配器',1,'钱小明',DATE_SUB(NOW(),INTERVAL 2 DAY),'admin',DATE_SUB(NOW(),INTERVAL 2 DAY),''),
(2,2,'水晶头-RJ45',10,'张建国',DATE_SUB(NOW(),INTERVAL 3 DAY),'admin',DATE_SUB(NOW(),INTERVAL 3 DAY),'大批量更换'),
(3,6,'门禁电源模块',1,'孙立华',DATE_SUB(NOW(),INTERVAL 3 DAY),'admin',DATE_SUB(NOW(),INTERVAL 3 DAY),''),
(1,3,'无线AP面板',1,'张建国',DATE_SUB(NOW(),INTERVAL 4 DAY),'admin',DATE_SUB(NOW(),INTERVAL 4 DAY),'更换故障AP'),
(2,9,'网线-超五类',1,'周志强',DATE_SUB(NOW(),INTERVAL 4 DAY),'admin',DATE_SUB(NOW(),INTERVAL 4 DAY),'重新布线'),
(3,10,'机柜散热风扇',2,'张建国',DATE_SUB(NOW(),INTERVAL 5 DAY),'admin',DATE_SUB(NOW(),INTERVAL 5 DAY),'数据中心更换风扇'),
(1,8,'监控电源适配器',2,'钱小明',DATE_SUB(NOW(),INTERVAL 5 DAY),'admin',DATE_SUB(NOW(),INTERVAL 5 DAY),''),
(2,5,'道闸电机',1,'孙立华',DATE_SUB(NOW(),INTERVAL 6 DAY),'admin',DATE_SUB(NOW(),INTERVAL 6 DAY),'南门道闸维修'),
(3,4,'道闸弹簧',1,'孙立华',DATE_SUB(NOW(),INTERVAL 6 DAY),'admin',DATE_SUB(NOW(),INTERVAL 6 DAY),''),
(1,2,'水晶头-RJ45',5,'周志强',DATE_SUB(NOW(),INTERVAL 7 DAY),'admin',DATE_SUB(NOW(),INTERVAL 7 DAY),''),
(1,6,'门禁电源模块',1,'钱小明',DATE_SUB(NOW(),INTERVAL 7 DAY),'admin',DATE_SUB(NOW(),INTERVAL 7 DAY),''),
(2,1,'光纤跳线-SC',3,'张建国',DATE_SUB(NOW(),INTERVAL 8 DAY),'admin',DATE_SUB(NOW(),INTERVAL 8 DAY),'机房跳线更换'),
(3,9,'网线-超五类',1,'周志强',DATE_SUB(NOW(),INTERVAL 8 DAY),'admin',DATE_SUB(NOW(),INTERVAL 8 DAY),''),
(2,10,'机柜散热风扇',1,'张建国',DATE_SUB(NOW(),INTERVAL 9 DAY),'admin',DATE_SUB(NOW(),INTERVAL 9 DAY),''),
(1,4,'道闸弹簧',2,'孙立华',DATE_SUB(NOW(),INTERVAL 10 DAY),'admin',DATE_SUB(NOW(),INTERVAL 10 DAY),'出口道闸弹簧更换');

-- 人员车辆绑定 (当前0条, 插20条)
INSERT INTO iot_person_vehicle (user_id, user_name, dept_id, dept_name, vehicle_plate, vehicle_brand, vehicle_color, create_by, create_time, remark) VALUES
(1,'admin',100,'总公司','京A00001','宝马X5','黑色','admin',NOW(),''),
(10,'张建国',101,'技术部','京A12345','奥迪A6L','白色','admin',NOW(),''),
(11,'钱小明',102,'销售部','京B67890','奔驰E300','黑色','admin',NOW(),''),
(12,'孙立华',103,'运维部','京C11111','大众帕萨特','银色','admin',NOW(),''),
(13,'周志强',104,'工程部','京D22222','丰田汉兰达','白色','admin',NOW(),''),
(1,'admin',100,'总公司','京A00002','特斯拉Model Y','蓝色','admin',NOW(),'新能源车'),
(10,'张建国',101,'技术部','京E33333','本田雅阁','黑色','admin',NOW(),'家庭第二辆车'),
(11,'钱小明',102,'销售部','京F44444','别克GL8','金色','admin',NOW(),'公司商务车'),
(12,'孙立华',103,'运维部','京G55555','比亚迪汉','红色','admin',NOW(),'新能源车'),
(13,'周志强',104,'工程部','京H66666','日产奇骏','灰色','admin',NOW(),''),
(10,'张建国',101,'技术部','京I77777','理想ONE','绿色','admin',NOW(),'新能源增程'),
(11,'钱小明',102,'销售部','京J88888','宝马3系','白色','admin',NOW(),''),
(12,'孙立华',103,'运维部','京K99999','大众途观L','棕色','admin',NOW(),''),
(13,'周志强',104,'工程部','京L11110','雷克萨斯ES','银色','admin',NOW(),''),
(1,'admin',100,'总公司','京M22220','奥迪Q5','黑色','admin',NOW(),''),
(10,'张建国',101,'技术部','京N33330','福特蒙迪欧','白色','admin',NOW(),''),
(11,'钱小明',102,'销售部','京O44440','吉利星越L','蓝色','admin',NOW(),''),
(12,'孙立华',103,'运维部','京P55550','长城坦克300','橙色','admin',NOW(),''),
(13,'周志强',104,'工程部','京Q66660','传祺M8','黑色','admin',NOW(),''),
(10,'张建国',101,'技术部','京R77770','小鹏P7','灰色','admin',NOW(),'新能源车');
