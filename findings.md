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
| @PreAuthorize求值500 | hasAnyPermi/hasAnyRoles写成多参数 `('a','b')` | 改逗号分隔单参数 `('a,b')` |

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
- **权限三方不一致审计**：新增 H5/仪表盘功能后，Controller 使用了 `dashboard:meeting:query`/`dashboard:apartment:query`/`visitor:guard:verify` 三个权限，但 `sys_menu` 表完全没有对应记录。H5Controller 3 个端点无 `@PreAuthorize` 注解。修复：DB 插 12 条菜单权限 + Controller 加注解
- **菜单树结构错位**：公寓管理 租客 parent_id=0 应是 2113；智能化管理 7 页面有 3 个多余中间层；考勤管理 6 个按钮权限 parent_id 错挂在顶层 2021。修复：扁平化+归位

## 待处理需求（暂缓）

- **废弃 getRouter，精简路由工作流**：当前添加新页面需改前端路由+DB菜单+后端注解三处。方案：sys_menu 只存权限字符串，前端 router 全权负责菜单结构，砍掉 DB 树形关系。新增页面只需改 router + Controller 两处。讨论日期：2026-07-02。

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

## 驾驶舱 Dashboard 设备饼图 API 499 排查（2026-07-11）

### 问题现象
- 驾驶舱4个仪表盘 API：visitor/meeting/apartment 正常 200，device **100% 失败**
- Chrome DevTools: `net::ERR_ABORTED`
- Nginx access log: `HTTP 499`（客户端断开连接）
- 设备饼图区域始终空白

### 排查过程

| 步骤 | 检查项 | 结果 |
|------|--------|------|
| 1 | 直接 curl 后端 `/dashboard/device` | 200，0.005s，后端不慢 |
| 2 | 通过 Nginx curl | 200，0.001s，代理不慢 |
| 3 | 数据库 `sys_menu` 权限 | admin 有 `*:*:*`，非权限问题 |
| 4 | 部署 jar 中的 Controller 注解 | 4 个 `@PreAuthorize` 都存在 |
| 5 | 时间规律 | device 请求与其他3个间隔恰好 10s（axios timeout=10000） |
| 6 | 部署目录 | **14 个旧 chunk 残留**，新旧 JS 混存 |

### 根因结论

**Nginx 499 = 客户端在收到服务端响应前主动断开连接。**

最可能原因（3 个，按概率排序）：

1. **部署目录旧 chunk 残留**：多次部署积累了 14 个含 `dashboard/device` 的 chunk 文件，浏览器可能加载了旧版 JS（无 `await` 串行化），4 个并发请求同时发出 → Chrome 连接池竞争 → 第一个请求被丢弃 → 10s 超时 → ERR_ABORTED

2. **mounted() 资源竞争**：`loadFonts()`（Google Fonts `<link>` 注入）和 `loadAll()`（4 个 API）同步触发，浏览器资源加载风暴阻塞了首请求

3. **Cookie/Token 读取延迟**：若 `getToken()` 在首请求发出时 cookie 尚未就绪 → 后端 401 → 响应拦截器弹窗 → 取消请求

### 修复方案

| 方案 | 操作 | 效果 |
|------|------|------|
| A（推荐） | `setTimeout(() => loadAll(), 100)` + 清空部署目录 | 避免资源竞争 + 保证 JS 版本一致 |
| B | loadAll 内加 retry 逻辑 | 兜底，API 失败后重试一次 |
| C | SecurityConfig permitAll `/dashboard/**` | 不推荐（降低安全性） |

方案 A+B 组合预计 95% 解决。
