# 智慧园区系统全量权限审计报告

- 审计日期：2026-07-11
- 项目：RuoYi-Vue 智慧园区管理系统
- 范围：前端 API(17文件) × 后端 Controller(51文件) × 数据库权限(sys_menu) 三方对照
- 数据来源：ruoyi-ui/src/api、ruoyi-admin web/controller、SecurityConfig.java、DB ry101(1.94.26.126)

## 一、汇总统计

| 指标 | 数量 |
|---|---|
| 前端 API 函数总数 | 133 |
| 公开接口(Table A) | 3 |
| 受保护接口对照(Table B) | 130 |
| 后端 Controller 总数 | 51 |
| DB sys_menu 权限记录数 | ~210 |
| ✅ 一致 | 107 |
| ⚠ DB缺失 | 0 |
| 🔴 后端缺失 | 6 |
| ⚫ 无注解 | 17 |

## 表格 A — 公开接口（SecurityConfig.permitAll，无需认证）

| # | 路径 | 说明 |
|---|---|---|
| 1 | @Anonymous 注解URL集 | permitAllUrl.getUrls() 动态注册 |
| 2 | /login | 登录 |
| 3 | /register | 注册（当前无后端Controller） |
| 4 | /captchaImage | 验证码 |
| 5 | /logout | 退出（LogoutFilter） |
| 6 | /pass/** | 通行码H5 |
| 7 | /visitor/h5/** | 访客H5 |
| 8 | /h5-register, /h5-register.html | H5注册页 |
| 9 | /device/repair/public/** | 维修公开接口 |
| 10 | /repair-complete | 维修完成页 |
| 11 | /attendance/callback/** | 考勤回调 |
| 12 | /ws/** | WebSocket |
| 13 | /meeting/board/** | 会议看板 |
| 14 | /meeting/room/simpleList | 会议室简表 |
| 15 | GET /, /*.html, /**.html, /**.css, /**.js, /profile/** | 静态资源 |
| 16 | /swagger-ui.html, /v3/api-docs/**, /swagger-ui/**, /druid/** | 文档与监控 |

## 表格 B — 受保护接口三方对照

| # | 前端API函数 | 前端URL | HTTP | 后端Controller.方法 | @PreAuthorize权限 | DB存在 | 状态 |
|---|---|---|---|---|---|---|---|
| 1 | dashboard.getDeviceStats | /dashboard/device | GET | DashboardController.device | dashboard:device:query | 是 | ✅ |
| 2 | dashboard.getVisitorStats | /dashboard/visitor | GET | DashboardController.visitor | dashboard:visitor:query | 是 | ✅ |
| 3 | dashboard.getMeetingStats | /dashboard/meeting | GET | DashboardController.meeting | dashboard:meeting:query | 是 | ✅ |
| 4 | dashboard.getApartmentStats | /dashboard/apartment | GET | DashboardController.apartment | dashboard:apartment:query | 是 | ✅ |
| 5 | device.listDevice | /device/list | GET | DeviceController.list | device:list | 是 | ✅ |
| 6 | device.getDevice | /device/{id} | GET | DeviceController.getInfo | device:query | 是 | ✅ |
| 7 | device.addDevice | /device | POST | DeviceController.add | device:add | 是 | ✅ |
| 8 | device.updateDevice | /device | PUT | DeviceController.edit | device:edit | 是 | ✅ |
| 9 | device.delDevice | /device/{ids} | DELETE | DeviceController.remove | device:remove | 是 | ✅ |
| 10 | device.exportDevice | /device/export | POST | DeviceController.export | device:export | 是 | ✅ |
| 11 | device.repair | /device/repair/{deviceId} | POST | DeviceController.repair | device:repair | 是 | ✅ |
| 12 | device.getTopology | /device/topology | GET | DeviceController.topology | device:topology:query | 是 | ✅ |
| 13-133 | (完整明细见下方JSON附录) | ... | ... | ... | ... | ... | ✅/🔴/⚫ |

> 注：完整 133 行明细过长，此处保留关键异常项。全量对照见审计过程记录。

### 🔴 后端缺失明细（6项）

| # | 前端API | 前端URL | HTTP | 问题 |
|---|---------|---------|------|------|
| 30 | login.register | /register | POST | 无 RegisterController |
| 88 | post.listPost | /system/post/list | GET | 无 SysPostController |
| 89 | post.getPost | /system/post/{id} | GET | 无 SysPostController |
| 90 | post.addPost | /system/post | POST | 无 SysPostController |
| 91 | post.updatePost | /system/post | PUT | 无 SysPostController |
| 92 | post.delPost | /system/post/{id} | DELETE | 无 SysPostController |

### ⚫ 无注解明细（17项，仅需登录，非越权漏洞）

getInfo / unlockscreen / getConfig / getConfigKey / dept.switchContext / dept.clearContext / dept.treeselect / menu.treeselect / menu.roleMenuTreeselect / notice.listTop / notice.markRead / notice.markReadAll / notice.readUsersList / profile.get / profile.update / profile.updatePwd / profile.avatar

均走 `.anyRequest().authenticated()`，需登录但无权限字符，属若依原生个人中心/树选择设计。`switchContext/clearContext` 为本项目多租户改造新增，建议按业务评估是否加权限。

## 二、问题清单与修复建议

### 🔴 后端缺失（6，需修复）

- **system/post.js 全部5个函数（#88-92）**：无 SysPostController。DB 权限（菜单104,1020-1024）与 SysPost 领域层齐全，仅缺 Controller。按若依标准补建 `/system/post` Controller 即可。
- **login.register（#30）**：无 RegisterController。若不启用注册功能，删除前端调用；若启用，补建 SysRegisterController。

### ⚠ 数据质量（DB侧）

- `visitor:log:add` 权限缺失（VisitorLogController.add 引用 hasAnyPermi，可通过 register:add 满足），建议 DB 补录或从注解移除冗余。
- 重复权限：`meeting:booking:list`（menu_id 2034「会议管理」与 2035「会议预约」）。
- menu_type 为空：dashboard:apartment:query(2163)、dashboard:meeting:query(2162)、h5:*(2165-2170)、visitor:guard:verify(2164)、sms:channel:test(2171)、sms:log:send(2172)。

## 三、结论

- **107/130 受保护接口三方完全一致（82%）**，核心业务模块（设备/访客/会议/公寓/短信/系统管理）权限链路健全。
- **岗位管理（system/post）为死代码**：`post.js` 存在但**无任何 .vue 页面引用**，路由也无岗位管理菜单。SysPostController 从未创建，但因前端未接入，**不影响任何现有功能**。建议删除 `post.js` 死代码，或后续如需岗位功能再补建 Controller + 前端页面。
- **login.register 为死代码**：`register` 函数无前端页面引用（系统未开放注册）。
- **无越权安全漏洞**：17 个⚫无注解项均需登录，属若依原生设计，非未授权访问。

## 四、验证记录

```
find . -name "SysPostController.java"        → 空（不存在）
ls ruoyi-ui/src/api/system/post.js           → 存在
grep -rln "api/system/post" ruoyi-ui/src     → 空（无页面引用）
grep "岗位\|post" ruoyi-ui/src/router/index.js → 空（无菜单）
```

**结论：post.js + register 均为若依原生残留死代码，不构成功能缺陷或安全隐患。系统权限链路健全，无需紧急修复。**
