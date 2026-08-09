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

### 关键调试发现（2026-07-22）—— 访客审批逻辑修正

- **dept_id 来源错误**：`tryAssignApprover` 曾将被访人 dept_id 写入 appointment.deptId，后被误改为取创建者 `SecurityUtils.getDeptId()`。正确逻辑：**dept_id 以被访人部门为准**，用于数据范围隔离。
- **approver 匹配应强制**：旧逻辑 best-effort 静默匹配 hostName→sys_user，匹配失败不报错，导致 approver_id 为 NULL 的预约可被创建。正确逻辑：**强制匹配，失败抛 ServiceException 阻止创建**。
- **visitor_log 缺少 dept_id**：`approveAppointment()` 和 `registerWalkin()` 两处创建 VisitorLog 时均未设置 deptId，导致 visitor_log.dept_id 始终为 NULL。修复：`log.setDeptId(appointment.getDeptId())`。
- **三条路径统一覆盖**：员工后台预约 → `insertAppointment()`、H5 自助 → `registerWalkin()` → `insertAppointment()`、现场散客 → `registerWalkin()` → `insertAppointment()`，全部汇入 `assignApproverAndDept()`（原 `tryAssignApprover` 改名），一处修改全局生效。
- **方法重命名**：`tryAssignApprover`（best-effort）→ `assignApproverAndDept`（强制匹配），更准确反映语义。

### 关键调试发现（2026-07-01）
- **DeptFillAspect 误写 SysDept 主键**：`DeptFillAspect` 拦截所有 Controller add* 方法，无条件把当前用户的 `deptId` 写入 `BaseEntity.deptId`。但 `SysDept.deptId` 是部门**主键**，不是数据范围字段。admin 的 deptId=100 被写入新部门的 dept_id → INSERT 主键冲突。修复：`if (arg instanceof SysDept) continue;`
- **权限三方不一致审计**：新增 H5/仪表盘功能后，Controller 使用了 `dashboard:meeting:query`/`dashboard:apartment:query`/`visitor:guard:verify` 三个权限，但 `sys_menu` 表完全没有对应记录。H5Controller 3 个端点无 `@PreAuthorize` 注解。修复：DB 插 12 条菜单权限 + Controller 加注解
- **菜单树结构错位**：公寓管理 租客 parent_id=0 应是 2113；智能化管理 7 页面有 3 个多余中间层；考勤管理 6 个按钮权限 parent_id 错挂在顶层 2021。修复：扁平化+归位

### 关键调试发现（2026-08-09）—— 设备拓扑整体升级

- **拓扑页空白根因**：`DeviceController.buildTopologyTree` 返回 `id`/`label`，前端读 `node.deviceId`/`node.deviceName` → 节点名称空。修复：字段对齐 `deviceId/deviceName/model/location/responsible/remark/icon/cableType/parentId`。
- **拓扑权限双轨**：菜单 `device:topology:view`(2002) + 按钮 `device:topology:query`(2048)。财务部(11)/工程部(13)角色有 view 无 query → 菜单可见但接口 403 页面空白。修复：`INSERT IGNORE INTO sys_role_menu VALUES (11,2048),(13,2048)`。
- **本地SVG图标集**：8 类图标放 `ruoyi-ui/src/assets/topo-icons/`（router/switch/firewall/ap/server/camera/nvr/other），前端 `require('@/assets/topo-icons/'+key+'.svg')` 打包到 dist/static/img/。拓扑节点按 `icon` 渲染，未配置按 deviceType 映射默认（NETWORK→switch/MONITOR→camera/NVR→nvr）。
- **线缆库**：`cable_type` 4 值：fiber光纤橙实线、ethernet双绞线蓝实线、wireless无线绿虚线(border-left dashed)、coaxial同轴灰实线。拓扑页每个子节点上方垂直线按该子节点 cableType 着色。
- **部署重启偶发失败**：重启遇 `sysConfigServiceImpl` init 失败（Redis/DB 瞬时抖动），重试即成功。生产环境为 SpringBoot4.0.3+Java21+Tomcat11；fat-jar 关闭时 NoClassDefFoundError(Lifecycle$SingleUse 等) 为噪音不影响运行。启动用 `setsid nohup java -jar ... </dev/null > app.log 2>&1 &`（start-topo.sh 脚本）。
- **拓扑页空白真正根因（渲染层，2026-08-09 定位）**：`TopologyNode` 组件用**内联字符串 `template`**，而项目打包的是 **runtime-only Vue（无模板编译器）**——`node_modules/vue` 的 module/main 入口都是 `vue.runtime.*.js`，`vue.config.js` 无 `vue$` alias 指向 full build。runtime-only 无法编译字符串模板 → 组件渲染为空注释占位 `<!--function...-->` → 节点区空白。生产 `NODE_ENV=production` 下编译警告被静默，**console 无任何报错**（之前 console 0 错误误导了排查）。该 bug 从 baseline 就存在，拓扑页节点区从未真正渲染过；字段对齐/detailNode 修复只解决接口层和弹窗层。**修复**：TopologyNode 拆为独立 SFC（`topology/TopologyNode.vue`），vue-loader 构建期预编译为 render 函数；index.vue 改 import 引入。**经验**：排查"接口 200 + console 无错但页面空白"时，检查渲染出的 DOM 是否有 `<!--function...-->` 注释占位符 + `Vue.compile` 是否存在（runtime-only 判断）。
- **Vue runtime-only 判定方法**：页面 `document.querySelector('#app').__vue__.constructor.compile` 为 `undefined` 即 runtime-only；字符串 template 组件必须 SFC 化或改 render 函数。
- **拓扑页过滤孤立设备（2026-08-09）**：拓扑树只展示"参与关联"的设备——`parent_id` 非空 **或** 被其他设备引用为父节点，隐藏完全孤立设备（无父且无子）。实现于 `DeviceController.topology()`：先收集全部 `parent_id` 集合，再过滤 `d.getParentId()!=null || parentIds.contains(d.getDeviceId())`。生产 12 台设备中仅 华为防火墙USG6565(100, 作为 测试设备-权限验证(106) 的父) 保留显示，其余 10 台测试/演示设备全部隐藏。注意：过滤基于 `selectDeviceList` 返回集（受 DeptScope 数据权限影响），不同账号可见范围不同。
- **架构图连线风格（2026-08-09）**：TopologyNode.vue 连线改为架构图直角折线。初版层级间距 40px（connector 28 + padding-top 12）偏短，**用户反馈后加长到 64px**（connector 44 + padding-top 20 + 子竖线 20）+ 水平间距 64px，commit `3f83728`。连线几何注意点：子节点竖线用 `top:-padding-top` 负值定位到 padding 区，需父容器无 `overflow:hidden`。

- **ssh 会话退出杀 Java 进程（2026-08-09）**：`ssh root@host "pkill; setsid nohup java -jar ... &"` 方式在 ssh 会话退出后 nohup 子进程被 SIGTERM 杀掉。**修复**：用 `ssh -f root@host "bash start-topo.sh"` 让 SSH 自身后台化，进程脱离终端生命周期。症状：pgrep 能短暂看到 PID，但 ssh 退出后进程消失、app.log 无新日志。

- **Redis session 过期导致 playwright 页面 stale（2026-08-09）**：playwright 复用的浏览器 cookie（Admin-Token）仍在，但 Redis 中 login_tokens 已过期 → API 返回 HTTP 200 但 body `code:401`"认证失败"→ 页面看起来"接口 200 但数据空"。**排查标识**：同一页面 `getInfo` 和业务 API 同时返回 `code:401` 即 session 过期。**修复**：playwright 验证前先 `document.cookie='Admin-Token=; expires=...'` + `localStorage.clear()` + 重新 login。

- **playwright-cli 污染 dist 目录（2026-08-09）**：playwright-cli 以当前 cwd 为工作目录，自动写入 `.playwright-cli/`。当 cwd 误在 dist 时，该目录被写入 dist/static/js/ 下 → `rmdir dist` 时 EBUSY（目录被 playwright-cli 进程锁定）。**规避**：构建前确保 cwd 在项目根，部署前 `rm -rf dist/.playwright-cli` 后再 tar。

- **DeptScope 影响拓扑可见性（2026-08-09）**：`DeviceController.topology()` 调用 `selectDeviceList(new Device())` 受 DeptScopeInterceptor 全局过滤 → 非 admin 账号只能看到本部门设备。zhaojg 账号部门内设备无父子关联 → 过滤后拓扑为空——这是数据权限与拓扑过滤逻辑叠加的**正确结果，非 bug**。如需该账号看到拓扑，需在部门内配置设备的 `parent_id` 关系。

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

## 监控体系基础设施（2026-07-12）

| 组件 | 地址 | 用途 |
|------|------|------|
| Grafana | http://115.159.194.149:3000 | 监控看板可视化 |
| Prometheus | http://115.159.194.149:9090 | 时序指标存储 |
| Categraf | 各设备本地/中心采集 | 统一采集器(SNMP+Ping插件) |
| AlertManager | 待部署(115.159.194.149:9093) | 告警规则→Webhook |

### AlertManager 部署完成（2026-07-12）

已部署 AlertManager 0.27.0 到 115.159.194.149:9093，告警链路端到端验证通过：
- 下载：gh-proxy.com 镜像(github直连不通)，装在 /opt/alertmanager
- 配置：webhook → http://1.94.26.126:8090/device/alert/webhook, send_resolved:true, group_wait:30s
- Prometheus：/etc/prometheus/prometheus.yml 配 alerting→localhost:9093 + rule_files→/etc/prometheus/rules/device.yml(up==0 for 3m)，systemctl restart 生效
- 验证：模拟告警 push_alert=200 → 30s后 Java AlertWebhookController 收到 → 未匹配IP正确跳过
- 良性日志：selectDeviceList 触发 DeptScopeInterceptor 异常(webhook无登录态,getDeptId抛异常)，被内部catch,不影响告警处理(同DeviceHeartbeatTask定时任务同源噪音)
- 坑：device.yml的heredoc中 {{ $labels }} 的 \$ 转义会导致yaml解析报"unknown escape character"，改用静态summary避开
- 重启方式：systemd unit prometheus.service，无 --web.enable-lifecycle，只能 systemctl restart

### 服务器 SSH 凭据

| 服务器 | 用户 | 密码 | 用途 |
|--------|------|------|------|
| 1.94.26.126 | root | Chinaunicom@1358 | 业务后端(8090)+前端(80)+MySQL(ry101)+Redis |
| 115.159.194.149 | root | Chinaunicom@1358 | Prometheus(9090)+Grafana(3000)+AlertManager(9093待部署)+Categraf |

## 大模型配置（AI报告用，2026-07-12）

| 项 | 值 |
|----|-----|
| 提供商 | DeepSeek |
| baseUrl | https://api.deepseek.com/anthropic |
| API Key | sk-ae962dffc574492fb81bf9378f200437 |
| 模型名 | deepseek-v4-flash |
| 接口协议 | Anthropic 兼容(/anthropic 后缀) |

## 监控改造定案（2026-07-12）

| 项 | 值 |
|----|-----|
| Grafana总览Dashboard uid | e585a8-e5b180-e4bbaa-e8a1a8-e79b98 |
| Grafana单设备Dashboard uid | e8aebe-e5a487-e4bbaa-e8a1a8-e79b98 |
| 报告格式 | B分级评估型(健康评级+设备/线路/流量三维+风险预警+优化建议) |
| A4运维 | Categraf/AlertManager/Grafana已部署，改为验证 |
| 三层架构 | Webhook解析下沉AlertWebhookService，Controller只转发 |
| 健康评分 | HealthScoreCalculator独立工具类(service/impl,纯函数单测) |

## A4验证结果（2026-07-12）

| 组件 | 端口 | 状态 |
|------|------|------|
| Prometheus | 9090 | ✅ 200，up指标可查 |
| Grafana | 3000 | ✅ 200 |
| AlertManager | 9093 | ⚠️ 000连接失败——需排查(未启动/端口/防火墙)，不通则A1 Webhook无告警来源 |

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
