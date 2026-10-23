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
| 非管理员看到全部数据(越权) | DeptScopeInterceptor白名单按`originalSql.contains(table)`判定,业务表JOIN系统表(sd)时被误判为系统表查询跳过dept过滤 | 白名单改**主表判定**(extractMainTable),非管理员强制追加`(dept_id=X OR dept_id IN(SELECT t.dept_id FROM sys_dept WHERE find_in_set(X,t.ancestors)))` |
| 前端传空串导致Long反序列化失败 | 父设备清空时前端传`''`被Jackson转Long报400 | submitForm统一归一化为`null` |

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
| 后端端口 | 8080（生产 1.94.26.126） |
| 前端端口 | 80 (Nginx, 1.94.26.126) |
| 后端部署目录 | /opt/leinuo/（jar=ruoyi-admin.jar, 启动脚本 start-topo.sh, 日志 app.log） |
| 前端部署目录 | /usr/share/nginx/leinuo/ |
| Nginx 站点配置 | /etc/nginx/conf.d/leinuo.conf（80 端口, /prod-api/→127.0.0.1:8080, /profile/→127.0.0.1:8080, try_files 回退 index.html） |
| 上传目录 | /opt/leinuo/uploadPath/ |
| 数据库（生产） | 1.94.26.126:3306/leinuo-fangke (leinuo/Chinaunicom@1358)，22 表（基础14+访客3+短信5），已从 1.55/ry101-test 全量迁移（2026-10-23，用户50/部门17/角色4/预约77/日志76） |
| Redis（生产） | 1.94.26.126:6379 |
| 中间件收口 | MySQL(3306+33060) 与 Redis(6379) 均已 bind 127.0.0.1，仅本机可访问（2026-10-23 收口；配置备份 mysqld.cnf.bak-20261023 / redis.conf.bak-20261023） |
| JDK（生产） | 系统 Java（Ubuntu 20.04 apt 安装，路径见 `which java`） |
| GitHub | https://github.com/YangSun-DiuDiu/Chinaunicom_1358（master 分支；裁剪前备份 tag=backup-pre-filter-20261021, commit=4bfb6b5；裁剪后 commit=8f4f697） |

### 生产环境迁移（2026-10-23，commit 待提交）

> 生产环境整体迁移到 1.94.26.126：应用 + MySQL/Redis 同机，中间件全部收口 127.0.0.1。旧生产 192.168.1.60(应用)/192.168.1.55(数据库/Redis) 已退役，数据已全量迁移，findings 早期章节中 1.94.26.126:8090、115.159.194.149 等为历史记录。

- **数据库**：1.55/ry101-test（sadmin）全量导出（本地 mysqldump --result-file，219573B）→ 1.94 建库 `leinuo-fangke`（utf8mb4）+ 专用账号 `leinuo`@localhost/127.0.0.1（Chinaunicom@1358，仅该库权限）→ 导入，22 表数据完整。
- **部署**：后端 /opt/leinuo/ruoyi-admin.jar（start-topo.sh，-Xms256m -Xmx512m，--spring.profiles.active=druid，HTTP 200 PID 2704935）；前端 /usr/share/nginx/leinuo/（Nginx 80，leinuo.conf 含 /prod-api/ 与 /profile/ 反代）。
- **配置变更**：application.yml（redis host 127.0.0.1、ruoyi.profile /opt/leinuo/uploadPath）；application-druid.yml（jdbc 127.0.0.1:3306/leinuo-fangke、username leinuo）；deploy.json production → 1.94.26.126。
- **遗留注意**：Redis 无 systemd 自启（手动 redis-server /etc/redis/redis.conf 启动，daemonize yes），服务器重启后需手动拉起；MySQL 内存紧张（1.7G，Swap 2.3G）。
- **验证**：HTTP 200（首页）+ /prod-api/login 代理通（验证码逻辑正常）+ 浏览器登录成功（截图 `截图/194_login.png`、`截图/194_index.png`）。

### 短信参数更新（2026-10-23，宁波镭诺生产）

> 生产环境短信配置切换为新阿里云账号（数据库 sys_sms_channel/sys_sms_sign_template 更新，改库后重启后端清驱动缓存生效，HTTP 200 PID 2705894）。

| 项 | 旧值 | 新值 |
|----|------|------|
| AccessKey ID | LTAI5t63X****（历史，已失效） | LTAI5tA3S9t****（完整值见生产库 sys_sms_channel，勿写入文档） |
| AccessKey Secret | QHezX9rB****（历史，已失效） | i25Ao2Xvay****（完整值见生产库 sys_sms_channel，勿写入文档） |
| 签名 | 慈溪思刊信息技术有限公司 | 镭诺电子宁波 |
| 审批环节-通知被访人模板（st_id=1，无变量） | SMS_512631808 | SMS_512631873 |
| 审批通过-通知访客模板（st_id=2，变量 passcode） | SMS_512676790 | SMS_512676855 |

- 备注：密钥明文存储于 sys_sms_channel（SmsResolver 直读，无加密）；驱动按 channel_type 单例缓存，改 AK/SK 后必须重启后端（`bash /opt/leinuo/start-topo.sh`）生效。
- **签名调整（2026-10-23 二次修正）**：签名「宁波镭诺」→「镭诺电子宁波」（阿里云账号下原签名不存在，报 isv.SMS_SIGNATURE_ILLEGAL；改为已审核通过的「镭诺电子宁波」后全链路打通）。已更新 sys_sms_sign_template st_id=1/2 并重启后端（PID 2707218）。**验证通过**：真实链路（预约 9087 孙扬/15657477316→岑梦婷）审批后两条短信均 SUCCESS——log 153 visitor_pending_notify→18217689986(SMS_512631873)、log 154 visitor_approval→15657477316(SMS_512676855)，签名均显示「镭诺电子宁波」。
- **投递诊断（2026-10-23）**：用户反馈手机未收到，用 AK 直调 QuerySendDetails 查回执（当前AK/SK 完整值见生产库 sys_sms_channel）：系统链路 100% 正常（阿里云接口 code=OK、bizId 有效、内容渲染正确），但**两个测试号码均被运营商侧拦截未送达**——15657477316 ErrCode=`MOBILE_IN_BLACK`（阿里云黑名单，需控制台「系统设置-黑名单管理」移除或客服申诉）；18217689986 ErrCode=`PORT_NOT_REGISTERED`（运营商端口未注册，常见携号转网/虚拟号段）。后续短信功能在生产验证时应换正常在用号码，或先清黑名单。
- **换号重测通过（2026-10-23）**：访客号码换为 15669267932（预约 9088 孙扬→岑梦婷，passCode=129C7A19），审批通过短信 **SendStatus=1 真正送达**（内容「【镭诺电子宁波】您的访客申请已审批通过，预约号为129C7A19」）。确认：短信链路全通，生产可用；此前 15657477316 黑名单、18217689986 端口未注册为号码侧问题。

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
| 1.94.26.126 | root | Chinaunicom@1358 | 生产：访客系统后端(8080,/opt/leinuo)+Nginx80+MySQL(leinuo-fangke)+Redis(均127.0.0.1收口) |
| 115.159.194.149 | root | Chinaunicom@1358 | Prometheus(9090)+Grafana(3000)+AlertManager(9093待部署)+Categraf |

## 大模型配置（AI报告用，2026-07-12）

| 项 | 值 |
|----|-----|
| 提供商 | DeepSeek |
| baseUrl | https://api.deepseek.com/anthropic |
| API Key | （已脱敏，见本地配置文件） |
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

## 设备管理父设备/拓扑状态修复（2026-08-16）

- **父设备无法修改/删除**：`DeviceMapper.xml` 的 `updateDevice` 用 `<if test="parentId != null">` 包住 `parent_id`，无法清空（清空=null 时被忽略）；删除有子设备的父设备时外键约束失败。修复：`parent_id = #{parentId},` 改为无条件更新。
- **前端空串坑**：清空父设备时 `<el-select v-model="form.parentId">` 传空串 `''`，Jackson 反序列化 Long 报 400。修复：submitForm 里 `''`/`undefined` 归一化为 `null`。
- **修改后状态不同步到拓扑页**：`list/index.vue` 与 `topology/index.vue` 之间通过 keep-alive 缓存切换，topology 页 `created()` 只在首次创建执行，缓存复用不触发 → 修改父级后拓扑页不刷新。修复：`created → activated()`（keep-alive 下每次进入组件都会执行 activated）。
- **父设备下拉防循环**：`loadParentDevices` 只排除自身（excludeId），若 A 的父是 B、B 的父又是 A 会死循环/无限层级。修复：迭代收集全部后代（父被排除则其子也排除）再过滤。

## zhaojg 部门数据隔离越权修复（2026-08-16）

### 根因
`DeptScopeInterceptor`（ruoyi-framework，MyBatis StatementHandler 拦截器）是项目唯一部门隔离机制。原白名单判定为 `originalSql.contains(白名单表名)` —— `selectDeviceList` `LEFT JOIN sys_dept sd` 取部门名时，SQL 文本含 `sys_dept` 被误判为系统表查询 → **跳过 dept_id 过滤** → 非管理员（zhaojg/dept 101）可见全部 12 台设备。

### 次要漏洞（一并修复）
1. 原逻辑 `WHERE 已含 dept_id → 跳过` 的检查可被带 deptId 参数的查询绕过。
2. `DeviceMapper.xml` line 68-70 的 `<if test="deptId != null and deptId != 0">` 允许前端传 deptId 参数绕过。

### 修复（路线甲：拦截器为主）
- **白名单改主表判定**：`extractMainTable(sqlLower)` 取 `FROM` 后第一个表，仅当主表为系统/全局共享表（sys_*、meeting_room）才跳过。业务表即使 JOIN 系统表也会被过滤。
- **非管理员强制附加**：删除「WHERE 含 dept_id → 跳过」后门，非管理员无条件追加 `(dept_id = X OR dept_id IN (SELECT t.dept_id FROM sys_dept t WHERE find_in_set(X, t.ancestors)))` —— 本部门 + 全部子部门。
- **DeviceMapper.xml 保留 deptId 条件**（用户决策路线甲）：XML 过滤与拦截器叠加，非重复定义问题，双保险。
- **用户约束**：非管理员看本部门+子部门；历史数据不改（用户自行处理）。

### 验证（生产 1.94.26.126）
- zhaojg（dept 101, role prodAdmin）登录 `/device/list` → 仅 2 台（106/114），全部 dept 101
- admin → 12 台（豁免正常）
- 部署 jar 校验：内嵌 `ruoyi-framework-3.9.2.jar` 的 `DeptScopeInterceptor.class` 含 `extractMainTable` 方法标记（字节码 grep 确认修复代码已上线）
- dept 101 无子部门（ancestors 无 find_in_set(101)），故仅本部门 2 台

### 经验
- **MyBatis 拦截器部署验证**：修改拦截器后，仅验证接口层不够，需确认 fat-jar 内嵌 framework 模块 jar 的 class 确实是新版。方法：`unzip -p ruoyi-admin.jar BOOT-INF/lib/ruoyi-framework-*.jar > /tmp/fw.jar`，再 `unzip -p /tmp/fw.jar com/ruoyi/framework/interceptor/DeptScopeInterceptor.class | grep 方法名`。
- **登录验证统一密码**：系统所有业务账号密码均为 `admin123`（见 CLAUDE.md「系统登录账号」），可用 curl 直接登录调 API 做端到端验证。

## 会议室组织架构树 + 部门隔离（2026-08-23）

- **业务页面部门树复用模式**：`/system/dept/treeselect` 本项目已改为仅 admin 可调（SysDeptController），业务角色页面必须新增**业务级 deptTree 端点**（设备用 `/device/deptTree` 权限 `device:list`；会议室新增 `/meeting/room/deptTree` 权限 `meeting:room:list`），内部统一委托 `deptService.selectDeptTreeList(new SysDept())`，返回 List<TreeSelect>(id/label/disabled/children)。
- **部门树与 sys_dept 实时同步**：页面 `created()` 每次进入重新拉取 deptTree（前端路由未设 keepAlive），TreePanel 自带刷新按钮 `@refresh="getDeptTree"`，无需缓存/定时任务。
- **meeting_room 移出白名单启用隔离**：DeptScopeInterceptor 白名单移除 `meeting_room` 后，非管理员列表查询被强制追加 `(dept_id=X OR dept_id IN(SELECT t.dept_id FROM sys_dept t WHERE find_in_set(X,t.ancestors)))`。主表写别名 `mr` 时拦截器自动用 `mr.dept_id`（detect 到 "meeting_room mr"），LEFT JOIN sys_dept sd 无歧义。**重要**：移除白名单前必须确认存量数据 dept_id 已填充，否则 NULL 部门数据对非管理员不可见（本项目 3 室已全部填充，回填 no-op）。
- **列表 SQL 别名化坑**：给查询加 LEFT JOIN 后，若主表别名，则 selectMeetingRoomById 等所有复用同一 `<include>` 的 SQL 的 where 条件也要带别名（`where mr.room_id = #{roomId}`），否则 MySQL 报 unknown column。
- **update 不加 dept_id（自动归属模式）**：DeptFillAspect 仅在 deptId==null 时注入当前用户/切换上下文部门。若 update SQL 加 dept_id 且前端编辑表单不带 deptId，编辑时会被 DeptFillAspect 误改为当前用户部门。决策：归属仅在创建时决定，update SQL 不含 dept_id 列。
- **DeptFillAspect 是存量 dept_id 的来源**：本项目 3 个会议室 dept_id 已填充（admin→100，zhaojg→101）正是阶段16 DeptFillAspect 生效后创建的。
- **Vue2 组件未注册的隐蔽症状**：`import TreePanel` 但漏 `components:{TreePanel}` → Vue 把 `<tree-panel>` 当**未知自定义元素**原样渲染（DOM 里是空标签 `<tree-panel title="组织架构"...></tree-panel>`），页面只显示 title 属性文本「组织架构」，**树节点完全不渲染**。API 验证全过（deptTree 200、deptOptions 有数据）但 UI 空白。排查要点：①对比正常页（设备列表 `components:{TreePanel,Treeselect}`）②`document.querySelector('.tree-sidebar')` 为 null 即未渲染 ③`document.querySelector('tree-panel').innerHTML===''` 且 childNodes=0 即组件未挂载。连锁坑：`this.$refs.xxxRef.setCurrentKey()` 报 `setCurrentKey is not a function`（$refs 拿到的是空 DOM 元素而非组件实例），与未注册同根因，修注册一处都好。
- **playwright eval 的 bash `$` 展开坑**：eval 里写 `vm.$store`/`vm.$refs` 时，bash 双引号会把 `$store` 展开成空 → JS 变 `vm..store` 报 `Unexpected token '.'`（位置 38 左右）。规避：整个 eval 用**单引号**包裹 bash 参数，或避免 `$` 属性访问。

## 访客部门登记二维码（2026-08-26）

### 调试坑 1：Vue2 项目误用 Element Plus 组件 `el-tree-select`
- **症状**：新增弹窗部门树区域空白，无法选择任何部门；API 正常（`/system/dept/treeselect` 返回完整树）。
- **根因**：`el-tree-select` 是 **Element Plus（Vue 3）** 组件，本项目 Vue 2.6.12 + Element UI 2.15.14 无此组件 → Vue 模板编译成未知自定义元素**渲染为空**（与 37a 的 `<tree-panel>` 未注册同族症状：API 通但 UI 空）。
- **修复**：改用项目标准 `@riophae/vue-treeselect`（package.json 已依赖 0.4.0），`import Treeselect` + css + `components:{Treeselect}`，`:options` 用 `enabledDeptOptions`（`filterDisabledDept` 递归过滤 `dept.disabled`），对齐 system/user、system/dept、device 写法。
- **教训**：Vue2 + Element UI 项目写模板组件前先确认组件所属框架。Element UI 2.x 没有 `el-tree-select`/`el-cascader-panel` 等 Element Plus 独有组件，未知元素在 Vue 2 静默渲染为空（无报错，仅 console warning）。

### 调试坑 2：Mapper XML 引用 remark 但建表漏列
- **症状**：批量初始化接口报 `java.sql.SQLSyntaxErrorException: Unknown column 'v.remark' in 'field list'`。
- **根因**：`VisitorDeptQrcodeMapper.xml` 按若依标准模板写了 `v.remark`（resultMap/selectVo/insert/update 均引用），但手写建表 SQL 漏了 `remark` 列。
- **修复**：`ALTER TABLE visitor_dept_qrcode ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT '备注' AFTER del_flag;`，本地 SQL 文件同步补齐。
- **教训**：自定义表的建表 SQL 字段清单必须与 Mapper XML 的 resultMap/column 清单逐一核对；MyBatis `selectVo` 中显式列出的每个字段都要在表里存在。

### 业务要点
- **部门树选人含子部门**：`sys_dept.ancestors LIKE CONCAT('%,', #{deptId}, ',%')` 查本部门+全部子部门在职员工（`sys_user u LEFT JOIN sys_dept d`，u.del_flag='0' AND u.status='0' AND d.del_flag='0' AND d.status='0'）。
- **URL 用 register_code 不用 deptId**：16 位大写 UUID 防暴露内部部门 ID；H5 匿名 `/info/{code}` 解析部门，`/submit` 用 `validateDeptScope` 后端强校验被访人必须属于扫码部门子树，防绕过。
- **旧码兼容**：无 code 参数时 `/hosts` 退回原「全园区历史被访人」模式，`registerWalkin` 2 参重载委托 3 参传 null 跳过部门校验。

## 权限模型重构（2026-08-26）

### 调试坑 1：checkRoleDataScope 部门过滤阻塞园区管理员分配角色
- **症状**：园区管理员(100)给本部门用户分配任意模块角色(3/4/5)都报「没有权限访问角色数据！」；空角色报 NPE「Cannot read the array length because <local2> is null」。
- **根因**：`SysRoleServiceImpl.checkRoleDataScope` 原实现遍历 roleIds，每个用 `selectRoleList(role)` 校验，而 `selectRoleList` 带 `@DataScope(deptAlias="d")` → DataScopeAspect 按 `sys_role_dept` 过滤；固定角色模型下模块角色在本部门(dept216)无成员 → 查空 → 误判「无权限」。NPE 则是 for-each 遍历 null roleIds。
- **修复**：放宽为「仅校验角色存在」——`if (!isAdmin && roleIds != null) { for (Long roleId : roleIds) { if (roleId==null) continue; if (roleMapper.selectRoleById(roleId)==null) throw ... } }`。`selectRoleById` 不被部门过滤（`sys_role` 在 DeptScopeInterceptor 白名单）。admin/100 越权限制不在本方法，统一由 `SysUserServiceImpl.validateRoleAssignment` 兜底（否则会破坏园区管理员编辑自身时保留已持有角色 100 的路径）。
- **教训**：固定/全局角色的存在性校验不能用带 `@DataScope` 的列表查询；「角色是否可被当前用户操作」与「角色是否存在」是两个概念，数据权限过滤和存在性校验要分开。

### 调试坑 2：前端 updateAuthRole 用 query string 绑定 Long[] roleIds
- **症状**：分配角色接口 `PUT /system/user/authRole` 传 JSON body `{userId, roleIds:[3]}` 时后端 roleIds=null。
- **根因**：`api/system/user.js` 的 `updateAuthRole(data)` 用 `request({ url, method:'put', params: data })` —— **params 走 query string**，Spring 对 `Long[] roleIds` 用 `@RequestParam` 从 query string 按 `roleIds=3,5` 逗号分隔绑定；JSON body（`data: data`）绑不到 `Long[]`（若依 insertUserAuth 的 roleIds 是 `@RequestBody SysUser` 里的字段，但 authRole 接口签名是 `(Long userId, Long[] roleIds)` 裸参数，只能 query string 绑定）。
- **教训**：若依「授权角色」接口是裸参数绑定（非 @RequestBody），调用/测试必须用 query string（`?userId=X&roleIds=3,5`），不能发 JSON body。

### 调试坑 3：paramiko Transport 直连认证失败，SSHClient 正常
- **症状**：`paramiko.Transport((host,22))` + `transport.connect(username,password)` 报 `AuthenticationException: Authentication failed`；但同一密码用 `SSHClient().connect()` 成功。
- **根因**：目标服务器认证协商下 `Transport` 直连与 `SSHClient` 走的安全认证路径有差异（具体为 server 多密码认证策略 + Transport 未触发相同的 auth 协商）。
- **修复**：统一改用 `SSHClient` 建立会话后用 `client.open_sftp()` 上传（复用已认证通道），不再单独 `Transport`。
- **教训**：生产 paramiko 上传/执行统一走 `SSHClient`，SFTP 用 `open_sftp()`；避免混用 `Transport` 直连。

### 关键机制：本项目是静态路由 + meta.permissions 过滤（非 DB 动态路由）
- `permission.js` 的 `GenerateRoutes` → `filterStaticRoutes`，按前端静态路由的 `meta.permissions` 与用户 `permissions` 集合过滤，**不是** `getRouters` 动态路由。
- `/system` 父级无 permissions，子菜单(用户100/部门103/角色/菜单管理)逐个按 `meta.permissions`(如 system:user:list)过滤，父菜单子项全被过滤则自动隐藏。
- DB `sys_role_menu` 只驱动后端 `@PreAuthorize` 权限字符串(`selectMenuPermsByUserId`)和前端 `v-hasPermi`，**不驱动路由/侧边栏**。侧边栏可见性由前端静态路由 meta 决定。
- 因此「收敛系统管理权限」= 改 `sys_role_menu`(后端接口 403) + 前端静态路由已用 `meta.permissions` 过滤(侧边栏自动隐藏)，无需改 router/index.js。

### 关键机制：多角色 data_scope 取并集 + 数据隔离职责分工
- **多角色 data_scope 合并规则**（`DataScopeAspect.dataScopeFilter`）：遍历用户所有角色，按 data_scope 类型去重后 **OR 拼接 = 取并集**；任一角色 data_scope=1(全部) 短路放行全量。不是「以某个角色为准」。
- **5 固定角色实际值**（生产 sys_role）：admin(1)=**1**(全部)；设备(3)/访客(4)/会议(5)/园区(100)=**4**(本部门及以下)。故多角色用户(如 zhaojg=3+5)去重后恒为「本部门及以下」，无冲突。
- **职责分工**：**业务表**(device/visitor/meeting/tenant) → `DeptScopeInterceptor`(不看 data_scope，非 admin 强制 `dept_id IN(本部门+子部门)`，按主表白名单判定)；**框架性数据**(组织架构 sys_dept、组织树 treeselect、系统表查询 selectRoleList/selectUserList) → `DataScopeAspect`(依赖 sys_role.data_scope)。两套范围计算一致(本部门+子部门)，自洽不打架。
- **约束**：新增角色 data_scope 一律保持 `4`，禁用 `2`(自定义)——否则需维护 sys_role_dept，且会被拦截器「本部门及以下」收窄，与固定角色模型冲突。

## 访客管理三需求（2026-08-28）

### 需求1：园区管理员看本部门及以下（数据范围）
- **隔离已有，只需放开**：`DeptScopeInterceptor` 已对 visitor_appointment/visitor_log 做「本部门+子部门」过滤，园区管理员(100)数据范围本来就是本部门及以下。真正卡住的是 **Controller 层的 approverId/hostName 硬限制**（非 admin 只能查自己审批/被访的）。修复：新增 `isParkAdmin()` = `SecurityUtils.hasRole("smartparkAdmin")`，`VisitorAppointmentController`/`VisitorLogController` 的 list/pending/export/getInfo 4 处 `!isAdmin(...)` 改为 `!isAdmin && !isParkAdmin`。
- **hasRole 匹配**：`SecurityUtils.hasRole("smartparkAdmin")` 按 `sys_role.role_key` 大小写敏感精确匹配（PatternMatchUtils），与若依 `@PreAuthorize("@ss.hasRole(...)")` 同机制，勿写错大小写。

### 需求2：登记二维码完整 URL
- **新端点**：`GET /visitor/deptQrcode/current`（权限 visitor:qrcode:list）返回当前用户所属部门的启用登记码，优先读 Redis `dept:context:<userId>`（切换上下文）再退 `SecurityUtils.getDeptId()`；无配置返回 null → 前端兜底 `origin + '/h5-register'`。
- **QrCode 组件相对路径**：编码时对以 `/` 开头的相对 URL 自动拼接 `window.location.origin`，否则扫码为相对路径无效。
- **坑：/current 首次返回已删除配置**：`selectDeptQrcodeByDeptId` mapper 无 del_flag 过滤 → 返回 delFlag=2 的旧配置。修复：mapper 补 `AND v.del_flag='0'` + 前端 `enabled==='1'` 双保险（注意 /current 与 selectByCode 不同——后者本就带 enabled+del_flag 过滤）。
- **部署一致性验证**：前端产物部署后 `md5sum` 比对本地 vs 生产 chunk，确保 Nginx 加载的就是新 bundle（本会话 chunk-576c223c.155ca453.js 两侧一致）。

### 需求3：H5 被访人脱敏（仅前端展示层）
- maskName：姓名只留首字 `charAt(0)`，其余 `*`.repeat；maskPhone：`*`.repeat(len-4) + `slice(-4)` 末4位（len≤4 全 `*`）；maskHost：`maskName +（部门）+ 手机号脱敏`，无手机号不拼手机号。
- 下拉选项/被访人电话框/成功页全用 mask 后值展示，提交仍传全量 hostName/hostPhone（后端校验不受影响）。
- H5 hosts 接口返回全量真实手机号（sunyang 15657477316），**脱敏纯前端**，接口层未动——若需接口层脱敏须另加字段控制（本次用户明确仅前端）。

### 测试账号创建经验
- 验证必须新建各权限账号（用户要求）。创建时手机号必须唯一：13977775555 冲突报「手机号码已存在」→ 换唯一号重试。园区管理员角色 role_key=smartparkAdmin(100)，访客管理员 visitorAdmin(4)。

## 园区管理员角色下拉修复（2026-09-07）

### 调试坑 1：selectRoleAll 走 AOP 代理触发 @DataScope 部门过滤 → 角色下拉空
- **症状**：cenmt(园区管理员 100, dept215) 用户管理**新增弹窗角色下拉为空**；**编辑弹窗角色下拉显示 raw id `100`** 而非角色名「园区管理员」。
- **根因**（两层）：
  1. **新增下拉空**：`SysRoleServiceImpl.selectRoleAll()` 原实现 `return SpringUtils.getAopProxy(this).selectRoleList(new SysRole());` —— 经 AOP 代理调用 `selectRoleList`，触发方法上 `@DataScope(deptAlias="d")`。DataScopeAspect 按 `sys_role.data_scope`(=4 本部门及以下)过滤，SQL 经 `selectRoleVo` 的 `LEFT JOIN sys_user_role/sys_user/sys_dept d` 注入 `d.dept_id ∈ 可见部门`。固定角色模型下普通角色 3/4/5 **在 dept215 无任何持有者** → 被 SQL 过滤掉 → 新增下拉只剩 100(甚至空)。
  2. **编辑显示裸 id**：编辑 cenmt 自身时 100 已在可见集合返回，但前端 `filterAssignableRoles` 把 1/100 一律滤掉 → roleId 有值、roleName 词条被滤 → `el-select` 找不到匹配 label 直接显示 `100`。
- **修复**（4 文件）：
  1. `SysRoleServiceImpl.selectRoleAll()` 改**直查 `roleMapper.selectRoleAll()`**（不再走 AOP 代理，去掉 @DataScope 副作用），删 `SpringUtils` import，补 Javadoc 说明。`selectRolesByUserId` 内部调 `selectRoleAll` 同样受益（授权页 authRole）。
  2. `SysRoleMapper.xml` `selectRoleAll` 补 `WHERE r.del_flag='0'`（直查全量时过滤逻辑删角色）。
  3. `SysUserServiceImpl.validateRoleAssignment` 追加**防自降级**：`isSelfEdit(targetUserId==当前用户) && existingRoleIds.contains(100) && 本次roleIds不含100 → 抛「不能移除自身的园区管理员角色」`。用 `Arrays.asList(roleIds).contains(100L)` 判断（需 import java.util.Arrays）。
  4. 前端 `user/index.vue` `filterAssignableRoles(roles, heldRoleIds)` 增加已持有参数：`roles.filter(r => (r.roleId!==1 && r.roleId!==100) || held.includes(r.roleId))`；`handleAdd` 传 `[]`、`handleUpdate` 传 `response.roleIds`（编辑必须保留已持有的 1/100 才不裸 id）。
- **权限边界不回归**：园区可授 3/4/5；授 100/1 仍被 `validateRoleAssignment` 首段校验拦截（已持有保留防误报）；admin 走 `SecurityUtils.isAdmin()` 短路不受限。admin 新增下拉不含 admin 角色 1 是 getInfo 原生过滤行为，未改动。
- **教训**：**角色下拉数据源（全局角色清单）不能带 @DataScope**——「角色可分配性」与「角色全量可见」是两回事。列表页部门过滤是特性，但下拉/树/字典这类**元数据源必须全量**；谁可分配由业务校验层统一兜底，勿让数据范围切面悄悄吞掉下拉选项。

### 调试坑 2：编辑弹窗 roleId 裸显示 vs 授权页 authRole 写法差异
- `authRole.vue` 早有 `|| item.flag` 保留已持有角色逻辑（授权页参考正确）；但 `user/index.vue` 的 `filterAssignableRoles` 漏了等价保留 → 编辑自己显示裸 id。**同一「保留已持有高权限角色」规则需在 index.vue handleUpdate 与 authRole 两处一致**。index.vue 场景经 handleUpdate 已有 `response.roleIds`（后端 getInfo 返回 `roleIds:[100]`），不必像 authRole 靠 item.flag 标记，直接以 roleIds 为 heldRoleIds 传入即可。

## 部署脚本固化进 Skill（2026-09-07）

### 经验：业务 Skill 可内嵌 scripts/ 子目录 + 可执行脚本（首个先例）
- 之前 7 个 zhihuiyuanqu-* skill 全是纯 SKILL.md 文档；本次为 zhihuiyuanqu-deploy 引入首个 `scripts/deploy.py`。Skill 的 SKILL.md 负责「何时用、怎么指挥、模式判断、验证、总结」，真正易出错的执行逻辑下沉到可复用脚本。**纯文档 skill 教的命令若与项目强制规范冲突（本场景：CLAUDE.md 要求 paramiko，SKILL.md 却写 scp/ssh），必然导致每次现场重写脚本——脚本必须与规范一致并随 skill 固话。**
- 脚本定位为「纯上传 + 远端执行」（用户确认），本地构建（mvn/npm）留在外部由 skill 引导先执行 → 脚本不耦合 Windows 构建环境，产物缺失时明确报错引导。脚本读 `deploy.json` 驱动 + `--project` 覆盖 → 跨若依项目复用，不硬编码 IP/密码/路径。

### 调试坑 1：Windows 控制台 GBK 无法打印非 ASCII 符号
- **症状**：deploy.py 全部部署成功（HTTP 200+PID 正常），但收尾 `log("部署完成 ✅")` 抛 `UnicodeEncodeError: 'gbk' codec can't encode character '✅'`（Windows stdout 默认 GBK）。
- **根因**：print 含 ✅ 等非 GBK 字符 → 控制台编码崩。`encode/decode` sanitize 是 no-op，真正解法是重配置 stdout。
- **修复**：脚本顶部 `sys.stdout.reconfigure(encoding="utf-8", errors="replace")`（try 包住，Python3.7+），此后中文/✅ 正常打印。同场景也常见于 mysql/curl 输出拉取（见会话多次 GBK 乱码），统一在打印前 reconfigure 或 `decode(...,'replace')`。

### 调试坑 2：curl `-w '%{http_code}'` 的 `%{}` 撞 python `%` 格式化
- **症状**：`"...%{http_code}...http://localhost:%s/" % port` 抛 `ValueError: unsupported format character '{'`。
- **根因**：curl 的格式占位 `%{http_code}` 里裸 `{`，被 python `%` 运算符当格式字符解析。
- **修复**：该字符串不用 `%` 格式化，改字符串拼接 `"...http://localhost:" + str(port) + "/..."`；或对要传给 python 的写 `%%{http_code}`（转义后 python 输出单 `%`）。注意验证 curl 只在 shell 层（远端命令字符串）则无碍。

### 调试坑 3：paramiko exec_command 后台进程阻塞 read（CLAUDE.md 已知坑，脚本内置防护）
- deploy.py 执行 `startCmd`（start-topo.sh 内 `setsid nohup java ... </dev/null > app.log 2>&1 &`）时，后台 java 持有 SSH 通道 fd → `stdout.read()` 阻塞。脚本用 `exec_command(cmd, timeout=10)` + 捕获 `socket.timeout` 视为「已启动不失败」+ 独立轮询 `curl localhost:8090` 与 `ps aux | grep jar` 验证（最多 30×2s）。实际生产验证第 10 次探测（~20s）才就绪 → 后端启动需 ~20s，轮询上限合理。

### 部署产物正确性校验
- 前端 dist 解压到 nginxDir 后顶层含 `board.html/.gz/html/pass/robots.txt/index.html.gz` 等——这些**都在本地 `ruoyi-ui/dist/` 中**（阶段 24/38 等手动放入的平板页/静态页），解压后结构与本地一致，非误清服务器文件。校验方法：对比本地 dist 顶层与远端解压后顶层。
- 后端上传前先 `cp jar jar.bak`（保留回滚底），full 流程 jar 与 .bak 同尺寸（106MB）确认生效。

### 调试坑 4：精简 SQL 建表漏列 → 接口逐层报 Unknown column（阶段45）
- **症状**：访客公开接口 `/visitor/h5/hosts` 报 `Unknown column 'a.dept_id'`；修复后完整登录报 `Unknown column 'u.job_number' in 'field list'`（SysUserMapper selectUserVo）。
- **根因**：精简 SQL 是按若依标准表 + 访客核心列手写的，但项目 Mapper XML 是定制版——引用了超出标准表的列：visitor_appointment 需 dept_id/pass_code/has_car/car_plate/has_goods/goods_desc/del_flag；visitor_log 需 dept_id/pass_code/has_car/car_plate/has_goods/goods_desc；sys_user 需 job_number(工号, varchar50)/photo(人员照片路径, varchar200)。
- **通用排查法**：写脚本解析全部 mapper XML 的 `alias.col` 引用（去掉 `<if>` 块避免噪声），与生产库 `information_schema.columns` 差集 → 一次性找全缺失列，避免逐个接口踩坑。
- **修复**：生产库 ALTER 补列 + 本地精简 SQL 三表 DDL/INSERT 同步修正（`sql/zhihuiyuanqu_visitor_only_init.sql`）。原表定义可从全量备份 `/root/db_backup/ry101-test-full-20261022_042236.sql` awk 提取。
- **经验**：验证码开启时登录脚本必须真实调用 `/captchaImage` 拿 uuid，再到 1.55 Redis `captcha_codes:{uuid}`（格式 `算式@答案`，取 `@` 后）取答案；否则 login 返回「验证码错误」无法区分是登录问题还是列缺失。

### 调试坑 5：getRouters 500「No static resource」——本项目无此接口，属正常
- 阶段45浏览器验证脚本误测 `/getRouters` 返回 500，一度怀疑裁剪删坏了登录链路。实查确认：本项目**前端为纯静态路由**（`permission.js` 的 `GenerateRoutes` 基于 `permissions` 过滤 `staticRoutes`，见 `store/modules/permission.js`），后端**本就没有** getRouters 接口（原版备份 4bfb6b5 也没有）——该 500 是验证脚本测了不存在的接口，非缺陷。
- 浏览器级验证的正确做法：playwright 真实登录（placeholder 定位「账号/密码/验证码」+ Redis 取答案）→ 登录后 URL `/index` → 本定制版是**顶部导航布局**（非若依默认左侧 sidebar）→ 首页文本含「系统管理 菜单·角色·用户·部门·岗位·参数 / 访客管理 预约·审批·登记·记录」即菜单正常 → 点击访客管理进入 `/visitor/appointment`。

## 现场登记页布局统一调整（2026-10-22）

- **问题**：`visitor/register/index.vue` 输入表单卡被限宽 `max-width:960px`，当日记录卡全宽——两卡宽度不统一，输入区与记录区占比不协调。
- **调整**（commit `72fc84e`，已部署 192.168.1.60，已 push GitHub master）：
  1. 两卡统一 `max-width:1280px` 居中、`margin:0 auto 16px` 统一间距、`border-radius:8px`；
  2. 表单卡 body padding 收紧（`20px 24px 8px`），记录卡 `12px 16px 16px`，按钮行贴齐表单底部；
  3. 表格容器圆角 + `overflow:hidden` 延续 Notion 浅色/hairline 风格。
- **验证**：playwright 截图（`https://aka.doubaocdn.com/s/fdicMsXavp`）确认两卡对齐、占比协调，用户确认满意后 commit+push。
- **注意**：`register/index.vue` 表单「联系电话」字段校验为 11 位手机号格式（座机号会被拦截，部门新增时同样如此），业务需座机号须改校验规则。
- 浏览器测试会话（Playwright 5 项 CRUD 测试 #25-#29）全部通过；测试数据已清理，现场登记保留记录 C935B53C（CRUD现场访客/13600001111/岑梦婷/综合管理部）。

## Logo 更换为方案四「迎客通道」图形标（2026-10-22）

- **背景**：用户对原 logo（🏢 emoji + 紫色渐变文字）不满意，要求简约、符合访客管理意境，经 doubao-creative-design 生成 6 个候选方案供挑选，用户选定**方案四**（两条相向弧线构成人形通道负空间、深青绿 `#2F5B50`、纯白底、扁平极简，见画布 `gfBfNJ6o-4NoDaNVPYg-0201`）。
- **衍生资产**：基于方案四核心图 `image_edit` 生成方形图形标（无文字、1:1、2048×2048，画布 item `LnG3gMDE-4Nr3TsBr2A-0201`），存 `ruoyi-ui/src/assets/logo/logo-icon.png`；横版组合图存 `logo-horizontal.png`。
- **改动文件**（commit 待本次提交）：
  1. `src/layout/components/Sidebar/Logo.vue`：emoji 🏢 → `img logo-icon.png`（展开 30px/折叠 34px、圆角 7px）；「智慧园区」文字渐变改深青绿 `#2F5B50→#5FA08F`；删除 led-pulse 紫色动画。
  2. `src/views/login.vue`：登录卡片顶部加横版 logo（170px，白底自然融合）。
  3. `src/views/index.vue`：门户头部加横版 logo（190px）。
  4. `public/favicon.ico`：由 logo-icon.png 经 Pillow 生成（64/32/16 多尺寸，4089B）。
- **验证**（playwright，线上 http://192.168.1.60，截图见 `截图/login_now.png`、`截图/logo_home.png`、`截图/logo_sidebar.png`、`截图/logo_collapse.png`）：
  - 登录页卡片顶部 logo ✅；首页门户 logo ✅；
  - 侧边栏展开态（图形标+智慧园区文字）✅；折叠态（34px 图形标）✅；
  - favicon.ico 线上 `GET /favicon.ico` 200 / 4089B ✅；`img.sidebar-logo-img` 打包资源 `static/img/logo-icon.a2c1128b.png` 加载 2048×2048 ✅。
- **部署**：`npm run build:prod`（exit 0）→ `deploy.py --mode frontend`（已生效）。
- **注意**：本次 commit 只含 logo 相关文件；Navbar 精简+个人信息页改造（任务#31）见下节，用户确认满意后随本记录一并提交。

## Navbar 图标精简 + 个人信息页 Notion 化改造（2026-10-22，任务#31）

- **背景**：界面右上角图标堆叠太"若依"化，个人信息维护页同理，用户要求精简并改造成本项目自己的风格（Notion 基准）。
- **改动文件**（commit 随本记录提交，已部署 192.168.1.60 生效）：
  1. `src/layout/components/Navbar.vue`：删除右上角搜索 Search、全屏 Screenfull、布局大小 SizeSelect 三个图标（含 import 与模板）；头像下拉只保留「个人中心 / 退出登录」；删除 setLayout/lockScreen/setting 死代码；
  2. `src/views/system/user/profile/index.vue`：重写为 Notion 卡片布局——左侧信息卡（96px 头像 + 昵称 + 紫色徽章 + 信息行），右侧「个人资料」下划线 tabs；
  3. `src/views/system/user/profile/userInfo.vue` / `resetPwd.vue`：表单统一 8px 圆角、紫色 2px focus 描边；
  4. `src/views/system/user/profile/userAvatar.vue`：头像 96px + hover 紫色光圈。
- **验证**：浏览器实测（playwright）截图 `截图/navbar_top.png`（首页右上角精简后仅剩「选择组织/系统管理员」）、`截图/profile_notion.png`（个人信息页新布局），已交付用户并获确认满意。

## 门户页去 logo + 移除「返回首页」入口（2026-10-22）

- **需求**：① /index 门户页的智慧园区横版 logo 不需要（登录后首页标题已足够）；② 左上角「返回首页」入口取消（与面包屑「首页」按钮功能重复）。
- **改动**（commit 待提交，已部署 192.168.1.60）：
  1. `src/views/index.vue`：删除 `portal-logo` img 与其样式，门户头部仅保留「智慧园区·综合管理平台」标题 + 英文副标题 + 紫色 accent 短线；
  2. `src/layout/components/Navbar.vue`：删除「返回首页」router-link（含 `showBackHome` computed 与 `.back-home-link` 样式），保留汉堡按钮 + 面包屑。
- **验证**：playwright 实测截图 `截图/portal_nologo.png`（门户无 logo）、`截图/navbar_noback.png`（访客页 navbar 无返回首页，面包屑「首页/访客管理/访客预约」可直达），已交付用户确认。

## 照片无法预览修复：Nginx 补 /profile/ 反代（2026-10-22）

- **症状**：人员照片上传成功（文件落盘 + `sys_user.photo` 写入 `http://192.168.1.60/profile/upload/...`），但「修改用户」弹窗内 60px 缩略图无法显示。
- **根因**：Nginx `zhihuiyuanqu.conf` 只有 `location / { try_files ... /index.html }` 和 `location /prod-api/`，**缺少 `/profile/` 反代** → `/profile/upload/...` 被 try_files 兜底返回 SPA 的 index.html（`HTTP 200 text/html`，非图片）。
- **修复**（192.168.1.60 `/etc/nginx/conf.d/zhihuiyuanqu.conf`，原配置已备份 `.bak-profilefix`）：
  ```
  location /profile/ {
      proxy_pass http://127.0.0.1:8080;   # 无尾斜杠：保留 /profile/ 前缀
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
      client_max_body_size 20m;
  }
  ```
  后端若依 ResourceConfig 映射 `/profile/** → /home/ruoyi/uploadPath/`，与上传目录一致。
- **验证**：`nginx -t` 通过 + reload；`GET /profile/upload/2026/10/22/page-...png` 由 text/html → `200 image/png`（124138B）；浏览器「修改用户」弹窗 img `complete=true`、naturalW/H=1249×1221（原图正常加载）；截图 `截图/photo_thumb.png`。
- **注意**：此修复在服务器 Nginx 上，不在代码仓库；后续后端若换端口需同步改此处。


## 架构审查落地：状态机 + pass 端点读写分离 + SmsUtil 收敛（2026-10-22，任务#35）
- **流程**：按 improve-codebase-architecture 技能完成架构走查（git 热点 = 前端 UI / 访客业务 / 多租户 / 短信），出 6 候选报告（临时目录 %TEMP%\architecture-review-20261022_191233.html，未入库），用户选定 Top recommendation（候选1+4+2，候选6并入候选1）。
- **候选1 预约状态机**（新文件 `ruoyi-system/.../domain/VisitorAppointmentState.java`）：PENDING/APPROVED/REJECTED/CANCELLED/VISITING/COMPLETED + Event(APPROVE/REJECT/CANCEL/CHECKIN/COMPLETE)，`can()/transition()` 唯一权威。Service 各方法删除手写状态判断（approve/cancel/complete/checkIn 全部改为状态机校验+转移）。状态转移矩阵：APPROVE/REJECT←PENDING；CANCEL←PENDING/APPROVED；CHECKIN←PENDING/APPROVED；COMPLETE←VISITING/APPROVED。
- **候选6 记录工厂**（新文件 `domain/VisitorLogFactory.java`）：`from(appointment, registerType, entryTime)` 统一 appointment→log 字段映射，消除 approve/registerWalkin 两处 20+ 字段手写拷贝。
- **候选4 pass 端点读写分离**（`VisitorPassController.java` 重写 + 新 `domain/vo/PassInfoDTO.java`）：
  - `GET /pass/{code}` 纯读（PassInfoDTO，两来源归一，不改变状态）；
  - `POST /pass/{code}/checkin` 显式到访确认（→VISITING）。
  - 前端：`h5/guard.vue` GET 成功后追加 POST checkin（扫码即确认行为不变）；`h5/visitor-pass.vue` 纯读 + created 支持 `?code=` 带参自动查询；`visitor/appointment/index.vue` openPassPage 由 `window.open('/pass/'+code)`（Nginx 无此反代，实为打开 SPA 空白页的既有缺陷）改为 `window.open('/h5/visitor/pass?code=...')`。
- **候选2 SmsUtil 收敛**（新 `sms/SmsPlan.java` + `sms/SmsResolver.java`）：biz→签名模板→渠道→驱动 4 段查询链收敛到 `SmsResolver.resolve(bizCode)`，sendSms/executeScheduledTasks/retry 三路径共用（原三处重复代码删除）；驱动解析+缓存移入 SmsResolver；**删除含旧环境 URL（1.94.26.126:3000）的 sendVisitorApprovalSms 桥接**（无调用方）；sendTestSms/5参桥接保留（SmsConfigController 测试入口，已加注释）。SmsUtil 507→约330行。
- **验证**（mvn clean package EXIT=0 + npm build:prod 两次；deploy.py full + frontend 至 192.168.1.60 均成功，后端 PID=100735 HTTP 200）：
  1. 冒烟脚本（/prod-api 前缀）：GET C935B53C 两次状态保持 PENDING（纯读）→ POST checkin 变 VISITING ✓；无效码返回"通行码无效或已过期" ✓；APPROVED→VISITING ✓。
  2. 浏览器（Playwright）：H5 通行页 `?code=C0DB503A` 自动查询显示"✅ 通行有效"（纯读）；guard 核验页手动输入 C0DB503A → "✅ 通行有效 · 已到访"（GET+POST 联动）；库确认 9073 PENDING→VISITING，其余未受影响 ✓。截图 `截图/arch_guard_checkin.png`。
  3. 发现并确认：Nginx 无 `/pass/` 反代（直接访问返回 SPA HTML），前端实际走 `/prod-api/pass/`；旧 `window.open('/pass/…')` 为空白页缺陷，本次已修复为 H5 页。
- **注意**：appointment 表 del_flag='1' 的记录通行码会显示"无效"（正常业务行为，selectAppointmentByPassCode 带 del_flag='0'）。DeptScopeInterceptor 白名单判断基于主表，公开 /pass 接口 deptId=null 时跳过过滤，不受影响。

## 保安核验登录修复 + 生产验证（2026-10-23）
- **需求背景**：用户问"保安核验登录页面是哪一个，我找不到"，要求提供可访问 /h5/guard 的账号；随后明确"需要"。
- **问题1（补建保安权限）**：生产库原本没有 visitor:guard:verify 权限点、无保安角色。已补建：menu 2027「保安核验」F 权限（parent 2004）、role 101「保安」role_key=securityGuard、user 905 guard01（密码 Guard@2026，**注：初次生成的 bcrypt 哈希与密码不匹配已重生成**）、绑定 role_menu(101,2027) 与 user_role(905,101)。权限链路：905→101→2027 已查库确认。
- **问题2（H5 登录系统性缺陷）**：后端 SysLoginService.validateCaptcha 在 sys.account.captchaEnabled=true 时强校验验证码，而 H5 登录页（src/views/h5/login.vue）原版无验证码输入框、传固定 code='h5' → **所有 H5 账号登录必失败"验证码已失效/用户不存在或密码错误"**（先报验证码失效被吞，实际先过 validateCaptcha）。修复：h5/login.vue 参照 PC 登录页增加验证码输入框+图片点击刷新（getCodeImg→captchaEnabled/img/uuid，login 传 code+uuid，失败自动刷新验证码），
pm run build:prod 成功 + deploy.py --mode frontend 部署 1.94.26.126 成功。
- **验证（Playwright 实机全链路）**：
  1. /h5/login 刷新后验证码正常显示；guard01/Guard@2026 + 验证码（答案从 Redis captcha_codes:{uuid} 读取，如 "4"/"1"）→ 登录成功跳 /h5；
  2. H5 首页显示 5 入口：访客登记/访客审批/通行码/保安核验/现场登记，点击进入 /h5/guard；
  3. /h5/guard 手动输入有效通行码 54BA4A6E → "✅ 通行有效 · 已到访"，显示 孙扬/15669267932/被访人 岑梦婷（截图 guard_valid.png）；
  4. 输入无效码 BADCODE01 → "❌ 无效通行码"（截图 guard_invalid.png）；
  5. 库确认：9091（A168A0DA）核验后 APPROVED→VISITING（checkin 写操作生效）；9090（54BA4A6E）同样变 VISITING。
- **注意事项**：① 保安账号 guard01 登录入口 = H5 首页（/h5）「保安核验」卡片，或直输 /h5/guard（路由权限 visitor:guard:verify，侧边栏 hidden）；② H5 登录页现在与 PC 一致需验证码（生产 captchaEnabled=true 保持）；③ 验证码为数学算式，答案存 Redis key captcha_codes:{uuid}（测试调试可用，勿外传）。

## 二维码出示通道：手机号公开查询 + 二维码大屏（2026-10-23）

- **需求背景**：访客预约完成后缺"门卫处出示二维码"通道——访客通行码页只显示文字无二维码，保安摄像头无码可扫。用户拍板方案：**先做成输入访客手机号公开查询的公开接口**（免登录）。
- **后端**（新增公开接口，已部署 1.94.26.126）：
  1. `VisitorPassController` 新增 `GET /pass/list?phone=`（挂 /pass/**，SecurityConfig 已 permitAll，无需改鉴权；DeptScopeInterceptor 对匿名 deptId=null 跳过过滤）；
  2. 新 DTO `domain/vo/AppointmentBriefDTO.java`（appointmentId/passCode/visitorName/visitorPhone/visitorCompany/hostName/hostDept/visitTime/status，**不含身份证**）；
  3. Mapper/XML `selectAppointmentBriefByPhone`（visitor_phone 精确匹配 + del_flag='0' + create_time 倒序；**列别名驼峰**——项目未开 mapUnderscoreToCamelCase 全局映射，resultType DTO 必须 `as camelCase` 别名，否则字段全 null）。
- **前端**：
  1. 新页 `h5/appointments.vue`（我的预约：手机号→列表→「出示通行码」跳 /h5/visitor/pass?code=）；路由注册 `/h5/appointments`；
  2. `h5/visitor-pass.vue` 升级为二维码出示大屏：新增依赖 `qrcodejs2`，查询有效后渲染二维码（内容=通行码）+ 访客信息卡 + 通行码文字兜底 + 复制按钮；
  3. `permission.js` whiteList 新增 `/h5/appointments`、`/h5/visitor/pass`；
  4. H5 首页「通行码」卡片改名「我的预约」跳 /h5/appointments。
- **关键 Bug（白屏）与修复**：白名单公开页未登录访问白屏。根因：staticRoutes（动态路由）**只在登录后 GenerateRoutes 时 addRoutes**，未登录白名单页无 token 直接放行但路由从未注册 → `currentRoute.matched=[]` → 渲染空。修复 `permission.js`：新增 `ensureStaticRoutes()`（未登录也 addRoutes 一次），守卫顶部对白名单且 `to.matched` 为空时先注册再 `next({...to, replace:true})` 强制重新解析（**必须 replace，否则匹配不重算，仍白屏**；防循环靠 matched 判空）。
- **实测（生产 HTTPS 1.94.26.126，真实 Chrome）**：
  1. 免登录访问 /h5/appointments 正常渲染（白名单生效）；
  2. 输入 15669267932 → 返回 3 条孙扬预约（已到访+2 已通过，含被访人岑梦婷/时间/状态）；
  3. 点「出示通行码」→ /h5/visitor/pass?code=54BA4A6E → "✅ 通行有效"+二维码+复制按钮（截图 DHg1ttpVdz / 8EwMR6hhU5 / oEhTkzngim）。
- **安全提示（已向用户说明）**：公开按手机号查询 = 通行码泄露面（知道手机号即可调出二维码进门），属用户拍板的既有方案；列表/出示页均只读不写状态，checkin 仍须保安端显式 POST。
- **待办（用户未最终表态）**：短信直达链接（审批通过短信拼 `https://1.94.26.126/h5/visitor/pass?code=` 进 passcode 变量）——二维码页已在白名单，随时可加。
## 门卫扫码入口：统一「访客入场」选择页（2026-10-23）

- **需求**：访客预约完成的出示通道统一走"扫描入口二维码"模式——门卫处张贴固定二维码（内容 = `https://1.94.26.126/h5/entry`），访客扫码落地选择页，二选一：已有预约 / 新增预约。
- **改动**（commit 待本次提交，已部署 1.94.26.126 生效）：
  1. 新页 `h5/entry.vue`（访客入场：hero + 两个大卡片——「已有预约」跳 `/h5/appointments`、「新增预约」跳 `/h5-register`；深蓝渐变风格与 H5 其他页统一）；
  2. `router/index.js` constantRoutes 注册 `/h5/entry`（constantRoutes 免登录天然可访问，不依赖动态路由注册修复）；
  3. `permission.js` whiteList 加 `/h5/entry`（否则无 token 会被拦到登录页）；
  4. `visitor/h5/register.vue` 文案"登记"→"预约"语义：标题「智慧园区访客预约」/「XXX访客预约」、副标题「请填写以下信息提交预约申请」、按钮「提交预约」、成功页「预约提交成功」+ 提示「审批通过后，请向门卫出示此通行码」、按钮「继续填写」。
- **实测（生产 HTTPS + 真实 Chrome + 接口级回归）**：
  1. `/h5/entry` 免登录渲染：访客入场 / 已有预约（查询我的预约记录并出示通行码）/ 新增预约（填写来访信息提交预约申请）✅（截图 vP9Yku51NU）；
  2. 点「已有预约」→ `/h5/appointments`（我的预约查询页）✅（截图 8rJAJmDV7r）；
  3. 点「新增预约」→ `/h5-register`：标题「智慧园区访客预约」、被访人下拉为真实员工脱敏列表、按钮「提交预约」✅（截图 PKVVNnWiA1）；
  4. 接口级提交 `POST /prod-api/visitor/h5/submit`（Entry Test/15669267932/岑梦婷）：code 200 → appointmentId=9092、passCode=`0A7BA123`、status=PENDING、host_name 岑梦婷/综合管理部（库核对无乱码）✅。
- **门卫入口二维码**：`门卫入口二维码.png`（项目根目录，qrcode box_size=16 border=6，内容 = https://1.94.26.126/h5/entry，3402B）——打印张贴即可。
- **注意**：入口二维码内容固定 = /h5/entry 选择页 URL；新增预约提交后为 PENDING（被访人审批），审批通过后访客再扫码走「已有预约」出示通行码。bu 工具参数对中文不稳定（含中文 code 会报 Tool not found），浏览器自动化操作用 ASCII 或走 fill_input/接口通道。
## /h5/register 路径统一（2026-10-23）

- **需求**：`/h5-register` → `/h5/register`，与 /h5/login、/h5/entry、/h5/appointments 风格统一。
- **兼容策略**：旧路径保留为 redirect（`/h5-register` → `/h5/register`，query 保留），已打印的部门登记码/老链接不失效；白名单同时保留两路径。
- **改动文件**：
  1. `router/index.js`：`/h5-register` 改为 redirect 路由 + 新增 `/h5/register`（同组件 register.vue）；
  2. `permission.js`：whiteList 增 `/h5/register`（保留 `/h5-register` 供 redirect 首跳）；
  3. `entry.vue`：goRegister → `/h5/register`；
  4. `components/QrCode/index.vue`：默认登记地址 → origin + '/h5/register'；
  5. `visitor/appointment/index.vue`（showQrCode 两处）、`visitor/qrcode/index.vue`（getRegisterUrl/showQrCode 两处）→ '/h5/register'。
- **无需改**：后端 SecurityConfig 放行 `/visitor/h5/**`（接口路径与前端路由独立）；登记码二维码 = 前端运行时拼 URL 不存库（visitor_dept_qrcode 无 URL 列），无数据迁移。
- **实测**：本地 SPA fallback（8181）① /h5/register 直达渲染「智慧园区访客预约」✓ ② /h5-register?code=TESTCODE → 自动跳 /h5/register?code=TESTCODE（query 保留）✓ ③ 入口页「新增预约」→ /h5/register ✓；生产 curl：/h5/register、/h5-register、/h5/entry 均 HTTP 200 ✓。
## 员工密码统一重置为手机号后6位（2026-10-23）

- **范围**：sys_user user_id 2~49（48 名员工，del_flag=0）；不动 admin(1)/guard01(905)/已删 crudtest01(904)。
- **密码规则**：明文 = 手机号后 6 位；BCrypt（$2b$10$，成本10，与 guard01 同格式，Spring Security 兼容验证）。
- **不提示首次改密码**：pwd_update_date 全部置为 NOW()（SysLoginController.initPasswordIsModify = initPasswordModify==1 && pwdUpdateDate==null，置非空即不触发）。
- **执行方式**：mysqldump 备份 sys_user → /opt/leinuo/backup_sys_user_20261023.sql；本地 python bcrypt 批量生成 hash → UPDATE 48 行。
- **验证**：库查 48/48 已更新且 has_date=48；真实登录接口 cenmengting/689986 → code 200 + JWT token ✅。
- **注意**：bcrypt hash 含 $ 字符，远端 bash 双引号内会变量展开导致查询失败（用单引号或避开 $ 比较）；redis-cli GET captcha 答案带引号需 strip('"')。
## 域名 HTTPS 受信证书部署（2026-10-23 腾讯云）

- **目标**：fangke.netsightplus.cn 用受信证书（消除自签证书浏览器警告导致的"访问慢"）。
- **证书**：用户腾讯云申请 TrustAsia DV 证书（nginx 包），本地验证 CN/SAN=fangke.netsightplus.cn、公私钥匹配、有效期 2026-10-23 ~ 2027-01-21（约 90 天，到期需重新申请替换）。
- **部署**：证书+私钥上传 /etc/nginx/ssl/fangke_netsightplus.cn_bundle.crt|key；nginx 配置改为双 443 server（SNI 分流）：域名块挂腾讯证书、default_server 块挂原自签 leinuo 证书（保证 IP 直访保持自签、无 SNI 命中自签）；80 仍 301→https。原配置备份 leinuo.conf.bak_20261023。
- **验证**：本地 openssl s_client：域名 SNI→CN=fangke.netsightplus.cn（腾讯证书）✓、无 SNI→CN=1.94.26.126（自签）✓、nginx -t/reload OK、服务器本地 curl 域名 200 ✓。
- **阻塞（未备案拦截，技术无解）**：华为云按 SNI 拦截未备案域名——HTTP 返回「非法阻断2404」页面、HTTPS 直接 Connection reset（TCP 直连 IP+域名 SNI 也被 reset；同一 IP 不带域名 SNI 正常 200）。certbot/acme.sh 的 HTTP-01、TLS-ALPN-01 验证也全部被拦，DNS-01 因阿里云短信 key 无 DNS 权限失败。
- **结论**：证书已就绪待域名备案。备案通过后（需华为云接入备案），域名 443 应恢复正常，绿锁生效、域名访问速度恢复正常。当前 H5/后台访问继续用 https://1.94.26.126（自签，浏览器需信任或 -k）。

## 访客通行码生成时机改造（2026-10-23）

- **需求**：预约提交后不再立即生成通行码，改为「提交 → 等待审批页 → 审批通过后才生成通行码/二维码」，符合"审批通过才具备入场资格"的业务逻辑；审批拒绝不短信通知（用户拍板）；短信中通行码维持现状（不拼链接，用户拍板）。
- **后端改动**（通行码生成点收敛为唯一：审批通过补码）：
  1. `VisitorAppointmentServiceImpl.registerWalkin`：去掉"生成通行码"步骤（H5 预约提交的预约 pass_code 置空、PENDING）；返回 passCode=""；
  2. `VisitorAppointmentController.add`：去掉自动生成通行码（后台新增预约同样不预生成）；
  3. `VisitorAppointmentState`：`Event.CHECKIN` 允许来源收紧为**仅 APPROVED**（原 PENDING/APPROVED）——堵"待审批也能核验入场"漏洞；
  4. `VisitorAppointmentServiceImpl.checkInVisitor`：状态不允许时抛 ServiceException「预约尚未审批通过，无法核验入场」（原静默返回）；
  5. `VisitorPassController.buildPassInfo`：预约类通行码仅 APPROVED/VISITING/COMPLETED 有效，PENDING/REJECTED/CANCELLED → "通行码无效或已过期"；来访记录（现场登记 WALKIN）分支不受影响。
- **前端改动**：`register.vue` 成功页改为「等待审批」（⏳ + 不再显示通行码）；`appointments.vue` 出示通行码按钮仅 APPROVED/VISITING 显示，PENDING 行显示「审批通过后生成通行码，请耐心等待」。
- **验证**（生产 1.94.26.126 实测）：
  - H5 提交新预约 → 返回 passCode=""、库内 pass_code=NULL ✅；
  - GET /pass/0A7BA123（历史 PENDING 码）→ "通行码无效或已过期" ✅；
  - POST /pass/0A7BA123/checkin → "预约尚未审批通过，无法核验入场" ✅；
  - 审批通过链路（用户后台操作完成）：9093 补码 CDF66503 + sms_sent=Y（短信带码）→ GET /pass/CDF66503 200（VISITING）✅；VISITING 重复 checkin 被状态机拒 ✅；
  - 前端浏览器：我的预约页 15657477316 已通过/已到访显示按钮；15669267932 的 Entry Test（待审批）无按钮+等待提示 ✅（截图 docs/appt-status-ok.png）。
- **注意**：历史 6 条 PENDING 测试预约（9068/9069/9070/9072/9084/9092）已带码，新逻辑下其码全部失效（读/核验均被拦），如需清理可删（预约 9093 码时机验证 VISITING 亦为测试数据）。

## 来访记录通行码同步修复 + 离开功能分析（2026-10-23）

- **问题**：访客管理-来访记录（visitor_log）通行码为空。根因：改造后预约提交不生成码（VisitorLogFactory.from 从预约同步 passCode → 提交时记录无码），审批通过补码只更新预约表（visitor_appointment），未同步来访记录表（visitor_log）——旧逻辑"创建即生成码"掩盖了这个缺口。
- **修复**：approveAppointment 补码后调用新增 `visitorLogService.syncPassCodeByAppointmentId(appointmentId, passCode)`（Mapper `syncPassCodeByAppointmentId`：UPDATE visitor_log SET pass_code WHERE appointment_id）。存量数据一次性 SQL 同步（JOIN 预约表补齐 85 条，缺失=0）。
- **验证**（生产实测）：① 存量 9093 → log 9189 pass_code=CDF66503（与预约一致）✅；② 新链路：提交 9095 无码 → admin 审批通过 → 预约 EEC836F3 且**记录 9191 同步 EEC836F3** ✅。
- **离开功能分析**（来访记录操作栏"离开"，PUT /visitor/log/exit/{logId}）：
  - 作用：访客离场时由门卫/管理员点击，只把该条来访记录 exit_time 置为当前时间；前端 exit_time 非空显示"已离开"。数据用于进出时间统计/在场判断；pass 查询（log 分支）据此判 status=COMPLETED。
  - **与预约模块是两套独立结束路径**：预约模块另有"完成来访"（completeAppointment，预约 status→COMPLETED + leave_time）；"离开"不动预约表（status 仍 VISITING，预约列表仍显示"已到访"）→ 两表状态可能不一致。
  - **权限码不一致**：前端按钮 v-hasPermi='visitor:log:exit'，后端接口 @PreAuthorize('visitor:log:edit') → 角色权限配置需两处都授（若只授 exit，按钮显示但点击 403）。
  - 建议（待用户拍板）：① 离开时联动预约状态→COMPLETED；② 前后端权限码统一为 visitor:log:exit。

## 离开功能收口：联动预约完成 + 权限统一（2026-10-23）

- **联动**：`VisitorLogServiceImpl.updateLog`（离开接口）设置记录 exit_time 后，若记录有 appointmentId，走状态机 `Event.COMPLETE` 校验（VISITING/APPROVED → COMPLETED）将预约置为 COMPLETED 并写 leave_time；已结束的预约幂等跳过。
- **权限统一**：`VisitorLogController.exit` 注解由 `visitor:log:edit` 改为 `visitor:log:exit`（与前端按钮一致）；数据库菜单（menu 2022）与角色授权（园区管理员/访客管理员均已含 visitor:log:exit）无需变更。
- **验证**（生产实测）：exit 9189（预约 9093 VISITING）→ 记录 exit_time=2026-10-23 20:54:41、预约 status=COMPLETED + leave_time 同步；幂等重试状态不变 ✅。

## H5 预约登记类型修复（2026-10-23）

- **问题**：H5 新增预约提交后，来访记录登记类型显示"现场登记"。根因：`/visitor/h5/submit` 复用 `registerWalkin`（后台现场登记服务方法），其创建记录时 register_type 写死 "WALKIN"；H5 访客预约也被标成现场登记。
- **修复**：`registerWalkin` 新增 4 参重载（含 registerType）；H5 控制器传 "APPOINTMENT"（预约登记），后台现场登记保持 "WALKIN"。存量 81 条 H5 提交记录（判据 create_by=visitor_name AND appointment_id 非空）已修正为 APPOINTMENT。
- **验证**（生产实测）：H5 提交 → log 9195 register_type=APPOINTMENT ✅；后台现场登记 → log 9194 register_type=WALKIN（create_by=admin）✅；存量修正后 APPOINTMENT=86、WALKIN=1 ✅。
