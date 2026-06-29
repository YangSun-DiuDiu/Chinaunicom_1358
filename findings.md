# 研究发现

## 架构发现

- 全静态路由，前端 `meta.permissions` 过滤
- 租户=Dept，DeptScopeInterceptor 全局自动过滤，需检测 JOIN 别名
- Quartz RAMJobStore，启动时从 DB 加载任务
- SMS 新架构：SmsUtil 统一入口 → biz_code 路由 → 三驱动(阿里云/腾讯云/HTTP)
- 短信参数应从业务代码传入变量JSON，模板内容在云平台维护

## 常见问题模式

| 问题 | 根因 | 修复方式 |
|------|------|---------|
| 403 无权限 | Controller/路由/菜单权限不一致 | 三方统一 |
| dept_id ambiguous | SQL JOIN 两表有dept_id | 拦截器检测别名 |
| 短信日志未生成 | bizCode 不匹配 sys_sms_biz | 统一使用注册的 biz_code |
| 阿里云短信失败 | sign_name 有前导空格 | TRIM() |
| 离线发2条SMS | HeartbeatTask+DeviceServiceImpl 重复 | 由 ServiceImpl 统一发送 |
| 自动工单无 repairNo | createRepairOrder 没生成 | 注入JdbcTemplate生成 |

## 房间状态规则

| 条件 | 状态 | 颜色 |
|------|------|------|
| actual=0 | GREEN | 绿色 |
| 0<actual<capacity | CYAN | 青色 |
| actual>=capacity | BLUE | 蓝色 |
| 手动设置 | GRAY | 灰色(维修) |

## 短信 biz_code 映射

| biz_code | 业务场景 | 模板变量 |
|----------|---------|---------|
| device_repair | 手动报修 | device_name, token |
| device_repair_transfer | 转派 | device_name, token |
| device_fault_repair | 故障报修 | device_name, token |
| device_repair_alert | 报修按钮 | device_name |
| device_offline_alert | 离线告警 | device_name, token |
| device_online_notify | 上线通知 | device_name |
| smart_device_repair | 智能设备报修 | device_name, token |
| visitor_approval | 访客审批 | visitor_name, host_name, pass_code |

## 技术债务（代码审查发现，暂不修复）

| 严重度 | 问题 | 位置 |
|--------|------|------|
| CRITICAL | Controller 直接使用 JdbcTemplate | TenantController, RoomController |
| HIGH | 物理 DELETE 未用 delFlag | hostel Mapper XML |
| HIGH | ServiceImpl 缺少 @DataScope | hostel ServiceImpl |
| MEDIUM | cardList N+1 查询 | RoomController |

## 关键配置

| 配置项 | 值 |
|--------|-----|
| 后端端口 | 8090 |
| 前端端口 | 80 (Nginx) |
| 部署目录 | /root/zhihuiyuanqu20260625/ |
| 上传目录 | /home/ruoyi/uploadPath/ |
| 数据库 | 1.94.26.126:3306/ry101 (sadmin) |
| GitHub | https://github.com/YangSun-DiuDiu/Chinaunicom_1358 |
