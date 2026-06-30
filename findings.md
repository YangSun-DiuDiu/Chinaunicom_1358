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

## 访客管理模块流程（2026-06-29 梳理）

### 数据模型
- **visitor_appointment**：预约主表，status 六态流转，pass_code 8位UUID
- **visitor_log**：来访记录表，register_type 区分 APPOINTMENT/WALKIN

### 三条业务流程
1. **后台预约→审批（APPOINTMENT）**：管理员创建→审批人通过/拒绝→短信通知→通行码验证→完成离开
2. **H5自助登记（WALKIN）**：访客扫码→公开接口提交→自动创建预约+记录→仍需审批
3. **现场登记（WALKIN）**：保安前台→登记页面→自动创建预约+记录→立即进入

### 状态流转
PENDING → APPROVED/REJECTED → (VISITING,实际跳过) → COMPLETED
PENDING/APPROVED → CANCELLED

### 公开接口（无需登录）
- `GET /pass/{passCode}` — 通行码验证
- `POST /visitor/h5/submit` — H5自助登记
- `GET /visitor/h5/hosts` — 被访人列表

### 发现的问题（已于 2026-06-29~30 全部修复）
- ✅ 短信双重发送 → 删除 Controller 层重复，统一由 ServiceImpl 单次发送
- ✅ 物理删除 → XML 改为逻辑删除 update set del_flag='1'
- ✅ VISITING 状态未用 → VisitorPassController 验证通行码时自动更新
- ✅ 审批人无指定 → insertAppointment 自动匹配 hostName→sys_user
- ✅ Controller 直接注入 Mapper → VisitorPassController 改为注入 Service
- ✅ Controller 包含业务逻辑 → 全部下沉到 ServiceImpl
- ✅ 非管理员看不到待审批 → 3 层根因：dept_id NULL 被 DataScope 过滤 + tryAssignApprover 未设 dept_id + selectUserList 不支持 nickName

### 关键调试发现（2026-06-30）
- **BaseEntity 无 delFlag**：项目定制的 BaseEntity 只有 searchValue/createBy/createTime/updateBy/updateTime/remark/deptId/params，没有 delFlag
- **MyBatis 类型别名陷阱**：`typeAliasesPackage: com.ruoyi.**.domain` 不覆盖 `entity` 包，`resultType="SysUser"` 需用完整类名或改用 Map
- **selectUserList 不支持 nickName 过滤**：SysUserMapper.xml 的 selectUserList 只过滤 userId/userName/status/phonenumber/deptId，nickName 被忽略
- **DeptScope 全局拦截**：DeptScopeInterceptor 对非 admin 自动注入 `dept_id = 当前用户部门`，NULL 记录被排除
- **H5 无登录上下文**：`SecurityUtils.getDeptId()` 在公开接口中抛异常，需兜底

### 关键调试发现（2026-07-01）
- **DeptFillAspect 误写 SysDept 主键**：`DeptFillAspect` 拦截所有 Controller add* 方法，无条件把当前用户的 `deptId` 写入 `BaseEntity.deptId`。但 `SysDept.deptId` 是部门**主键**，不是数据范围字段。admin 的 deptId=100 被写入新部门的 dept_id → INSERT 主键冲突。修复：`if (arg instanceof SysDept) continue;`

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
