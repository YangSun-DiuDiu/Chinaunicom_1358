-- 测试数据批量插入脚本 - 批次2: 访客管理
USE ry101;

-- 访客预约 (当前1条, 补19条)
INSERT INTO visitor_appointment (visitor_name, visitor_phone, visitor_id_card, visitor_company, visit_reason, host_name, host_dept, host_phone, visit_time, leave_time, status, approver_id, approve_time, approve_remark, sms_sent, pass_code, has_car, car_plate, has_goods, goods_desc, create_by, create_time, remark) VALUES
('李明','13810001001','110101199001011234','华为技术有限公司','技术交流与合作洽谈','张建国','技术部','13900001001',DATE_ADD(NOW(),INTERVAL 1 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','A1B2C3D4','1','京A12345','0','','admin',NOW(),''),
('王芳','13810001002','110101199105052345','阿里巴巴集团','商务合作洽谈','钱小明','销售部','13900002001',DATE_ADD(NOW(),INTERVAL 2 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','B2C3D4E5','0','','1','演示设备一套','admin',NOW(),''),
('赵强','13810001003','320501198812123456','中兴通讯','设备维修服务','孙立华','运维部','13900003001',DATE_ADD(NOW(),INTERVAL 1 DAY),NULL,'APPROVED',1,NOW(),'同意来访','Y','C3D4E5F6','1','苏B67890','0','','admin',NOW(),''),
('陈静','13810001004','440101199203034567','腾讯科技','参观园区','周志强','工程部','13900004001',DATE_ADD(NOW(),INTERVAL 3 DAY),NULL,'REJECTED',1,NOW(),'本周园区施工不便接待','Y','D4E5F6G7','0','','0','','admin',NOW(),''),
('刘伟','13810001005','500101199308081234','百度公司','AI技术交流','张建国','技术部','13900001001',DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,'VISITING',1,DATE_SUB(NOW(),INTERVAL 1 DAY),'欢迎来访','Y','E5F6G7H8','1','渝C11111','0','','admin',DATE_SUB(NOW(),INTERVAL 1 DAY),''),
('孙悦','13810001006','120101199410102345','京东物流','物流合作洽谈','钱小明','销售部','13900002001',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),'COMPLETED',1,DATE_SUB(NOW(),INTERVAL 2 DAY),'已完成来访','Y','F6G7H8I9','1','津D22222','0','','admin',DATE_SUB(NOW(),INTERVAL 2 DAY),''),
('周明','13810001007','610101199501013456','华为技术有限公司','产品演示','张建国','技术部','13900001001',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),'COMPLETED',1,DATE_SUB(NOW(),INTERVAL 3 DAY),'已通过','Y','G7H8I9J0','1','陕A33333','0','','admin',DATE_SUB(NOW(),INTERVAL 3 DAY),''),
('吴婷','13810001008','330101199602024567','网易科技','系统集成方案讨论','孙立华','运维部','13900003001',DATE_ADD(NOW(),INTERVAL 4 DAY),NULL,'CANCELLED',NULL,NULL,NULL,'N','H8I9J0K1','0','','0','','admin',NOW(),'访客自行取消'),
('郑磊','13910001001','420101198501015678','中国移动','5G基站选址勘察','周志强','工程部','13900004001',DATE_ADD(NOW(),INTERVAL 1 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','I9J0K1L2','1','鄂A44444','1','测量仪器设备','admin',NOW(),''),
('林小红','13910001002','510101199803036789','美团点评','智慧餐饮合作','钱小明','销售部','13900002001',DATE_ADD(NOW(),INTERVAL 2 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','J0K1L2M3','0','','0','','admin',NOW(),''),
('冯刚','13910001003','450101199004047890','大疆创新','无人机巡检方案','张建国','技术部','13900001001',DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),'COMPLETED',1,DATE_SUB(NOW(),INTERVAL 5 DAY),'已完成','Y','K1L2M3N4','1','桂B55555','1','无人机设备箱','admin',DATE_SUB(NOW(),INTERVAL 5 DAY),'已演示'),
('陈小红','13910001004','340101199105058901','海康威视','安防设备巡检','孙立华','运维部','13900003001',DATE_ADD(NOW(),INTERVAL 5 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','L2M3N4O5','0','','0','','admin',NOW(),''),
('何涛','13910001005','210101199206069012','东软集团','软件开发合作','张建国','技术部','13900001001',DATE_ADD(NOW(),INTERVAL 1 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','M3N4O5P6','1','辽A66666','0','','admin',NOW(),''),
('谢芳','13910001006','370101199307071234','浪潮集团','服务器采购洽谈','钱小明','销售部','13900002001',DATE_ADD(NOW(),INTERVAL 3 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','N4O5P6Q7','1','鲁B77777','0','','admin',NOW(),''),
('韩磊','13910001007','410101199408089012','郑州宇通','车辆管理系统考察','周志强','工程部','13900004001',DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),'COMPLETED',1,DATE_SUB(NOW(),INTERVAL 6 DAY),'完成考察','Y','O5P6Q7R8','1','豫A88888','0','','admin',DATE_SUB(NOW(),INTERVAL 6 DAY),''),
('唐敏','13910001008','430101199509093456','三一重工','智能制造参观','张建国','技术部','13900001001',DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),'COMPLETED',1,DATE_SUB(NOW(),INTERVAL 4 DAY),'参观圆满结束','Y','P6Q7R8S9','1','湘A99999','0','','admin',DATE_SUB(NOW(),INTERVAL 4 DAY),''),
('曹阳','13910001009','230101199610104567','中科曙光','超算中心建设方案','张建国','技术部','13900001001',DATE_ADD(NOW(),INTERVAL 6 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','Q7R8S9T0','1','黑A00001','1','方案演示设备','admin',NOW(),''),
('邓丽','13910001010','350101199711115678','宁德时代','储能项目考察','周志强','工程部','13900004001',DATE_ADD(NOW(),INTERVAL 2 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','R8S9T0U1','1','闽B00002','0','','admin',NOW(),''),
('彭杰','13910001011','530101199812127890','云南白药','智慧园区参观学习','钱小明','销售部','13900002001',DATE_ADD(NOW(),INTERVAL 4 DAY),NULL,'PENDING',NULL,NULL,NULL,'N','S9T0U1V2','0','','0','','admin',NOW(),'');

-- 访客记录 (当前1条, 补19条)
INSERT INTO visitor_log (appointment_id, visitor_name, visitor_phone, visitor_id_card, visitor_company, visit_reason, host_name, host_dept, entry_time, exit_time, register_type, pass_code, has_car, car_plate, has_goods, goods_desc, create_by, create_time, remark) VALUES
(NULL,'王磊','13600001001','360101198801011234','本地访客','临时来访','张建国','技术部',DATE_SUB(NOW(),INTERVAL 1 HOUR),NULL,'WALKIN','WALKIN001','0','','0','','admin',NOW(),'现场登记'),
(NULL,'刘洋','13600001002','360101198902022345','本地访客','快递配送','前台','行政部',DATE_SUB(NOW(),INTERVAL 2 HOUR),DATE_SUB(NOW(),INTERVAL 1 HOUR),'WALKIN','WALKIN002','1','本地A001','1','包裹3件','admin',DATE_SUB(NOW(),INTERVAL 2 HOUR),'已完成离开'),
(3,'赵强','13810001003','320501198812123456','中兴通讯','设备维修服务','孙立华','运维部',DATE_ADD(NOW(),INTERVAL 1 DAY),NULL,'APPOINTMENT','C3D4E5F6','1','苏B67890','0','','admin',NOW(),''),
(5,'刘伟','13810001005','500101199308081234','百度公司','AI技术交流','张建国','技术部',DATE_SUB(NOW(),INTERVAL 1 DAY),NULL,'APPOINTMENT','E5F6G7H8','1','渝C11111','0','','admin',DATE_SUB(NOW(),INTERVAL 1 DAY),'已进入'),
(6,'孙悦','13810001006','120101199410102345','京东物流','物流合作洽谈','钱小明','销售部',DATE_SUB(NOW(),INTERVAL 2 DAY),DATE_SUB(NOW(),INTERVAL 2 DAY),'APPOINTMENT','F6G7H8I9','1','津D22222','0','','admin',DATE_SUB(NOW(),INTERVAL 2 DAY),'已离开'),
(7,'周明','13810001007','610101199501013456','华为技术有限公司','产品演示','张建国','技术部',DATE_SUB(NOW(),INTERVAL 3 DAY),DATE_SUB(NOW(),INTERVAL 3 DAY),'APPOINTMENT','G7H8I9J0','1','陕A33333','0','','admin',DATE_SUB(NOW(),INTERVAL 3 DAY),'已离开'),
(11,'冯刚','13910001003','450101199004047890','大疆创新','无人机巡检方案','张建国','技术部',DATE_SUB(NOW(),INTERVAL 5 DAY),DATE_SUB(NOW(),INTERVAL 5 DAY),'APPOINTMENT','K1L2M3N4','1','桂B55555','1','无人机设备箱','admin',DATE_SUB(NOW(),INTERVAL 5 DAY),'已离开'),
(15,'韩磊','13910001007','410101199408089012','郑州宇通','车辆管理系统考察','周志强','工程部',DATE_SUB(NOW(),INTERVAL 6 DAY),DATE_SUB(NOW(),INTERVAL 6 DAY),'APPOINTMENT','O5P6Q7R8','1','豫A88888','0','','admin',DATE_SUB(NOW(),INTERVAL 6 DAY),'已离开'),
(16,'唐敏','13910001008','430101199509093456','三一重工','智能制造参观','张建国','技术部',DATE_SUB(NOW(),INTERVAL 4 DAY),DATE_SUB(NOW(),INTERVAL 4 DAY),'APPOINTMENT','P6Q7R8S9','1','湘A99999','0','','admin',DATE_SUB(NOW(),INTERVAL 4 DAY),'已离开'),
(NULL,'黄建国','13600002001',NULL,'','送水','后勤','行政部',DATE_SUB(NOW(),INTERVAL 30 MINUTE),NULL,'WALKIN','WALKIN003','0','','1','桶装水10桶','admin',DATE_SUB(NOW(),INTERVAL 30 MINUTE),'现场登记'),
(NULL,'杨芳','13600002002','520101199303031234','来访客户','业务咨询','钱小明','销售部',DATE_SUB(NOW(),INTERVAL 5 HOUR),DATE_SUB(NOW(),INTERVAL 3 HOUR),'WALKIN','WALKIN004','0','','0','','admin',DATE_SUB(NOW(),INTERVAL 5 HOUR),'已离开'),
(NULL,'马超','13600002003','460101199404041234','','设备维修','孙立华','运维部',DATE_SUB(NOW(),INTERVAL 1 HOUR),NULL,'WALKIN','WALKIN005','1','琼E12345','1','工具箱','admin',DATE_SUB(NOW(),INTERVAL 1 HOUR),'现场登记维修'),
(NULL,'罗杰','13600002004','130101199505051234','顺丰快递','送文件','前台','行政部',DATE_SUB(NOW(),INTERVAL 40 MINUTE),DATE_SUB(NOW(),INTERVAL 30 MINUTE),'WALKIN','WALKIN006','0','','0','','admin',DATE_SUB(NOW(),INTERVAL 40 MINUTE),'已签收离开'),
(NULL,'胡丽华','13600002005','140101199606061234','来访客户','项目验收','周志强','工程部',DATE_SUB(NOW(),INTERVAL 2 HOUR),NULL,'WALKIN','WALKIN007','1','晋F67890','0','','admin',DATE_SUB(NOW(),INTERVAL 2 HOUR),'现场登记'),
(NULL,'段鹏','13600002006','150101199707071234','','空调维修','孙立华','运维部',DATE_SUB(NOW(),INTERVAL 3 HOUR),DATE_SUB(NOW(),INTERVAL 2 HOUR),'WALKIN','WALKIN008','1','蒙G11111','1','维修工具及配件','admin',DATE_SUB(NOW(),INTERVAL 3 HOUR),'已离开'),
(NULL,'田蜜','13600002007','220101199808081234','来访客户','访友','张建国','技术部',DATE_SUB(NOW(),INTERVAL 1 HOUR),NULL,'WALKIN','WALKIN009','0','','0','','admin',DATE_SUB(NOW(),INTERVAL 1 HOUR),'现场登记'),
(NULL,'沈浪','13600002008','310101199909091234','上海建工','施工方案确认','周志强','工程部',DATE_SUB(NOW(),INTERVAL 4 HOUR),DATE_SUB(NOW(),INTERVAL 3 HOUR),'WALKIN','WALKIN010','1','沪H22222','1','施工图纸','admin',DATE_SUB(NOW(),INTERVAL 4 HOUR),'已离开'),
(NULL,'汪琳','13600002009','630101200010101234','来访客户','面试','HR','人事部',DATE_SUB(NOW(),INTERVAL 2 HOUR),DATE_SUB(NOW(),INTERVAL 1 HOUR),'WALKIN','WALKIN011','0','','0','','admin',DATE_SUB(NOW(),INTERVAL 2 HOUR),'面试结束已离开'),
(NULL,'蒋涛','13600003001','540101200101011234','本地访客','设备巡检','孙立华','运维部',DATE_SUB(NOW(),INTERVAL 6 HOUR),DATE_SUB(NOW(),INTERVAL 4 HOUR),'WALKIN','WALKIN012','1','藏G12345','1','巡检仪器','admin',DATE_SUB(NOW(),INTERVAL 6 HOUR),'已离开');
