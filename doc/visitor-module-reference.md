# 访客管理模块 — 全流程业务逻辑与调用路径

> 版本：v1.0 | 日期：2026-06-30 | 部署：1.94.26.126:8090

---

## 一、模块概览

```
┌────────────────────────────────────────────────────────────┐
│                      访客管理模块                            │
│                                                            │
│  入口层       后台管理端          H5扫码端       保安前台端    │
│              (需登录+权限)      (公开访问)    (需登录+权限)   │
│              ─────────         ────────       ─────────     │
│  页面        预约管理/审批        /h5-register   来访记录     │
│  数据表      visitor_appointment    visitor_log             │
│  外部依赖    短信中台(SmsUtil)   通行码验证(/pass)           │
└────────────────────────────────────────────────────────────┘
```

---

## 二、数据模型

### 2.1 visitor_appointment（访客预约表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `appointment_id` | BIGINT PK | 预约ID |
| `visitor_name` | VARCHAR(50) | 访客姓名 |
| `visitor_phone` | VARCHAR(20) | 访客电话 |
| `visitor_id_card` | VARCHAR(20) | 访客身份证号 |
| `visitor_company` | VARCHAR(100) | 访客单位 |
| `visit_reason` | VARCHAR(200) | 来访事由 |
| `host_name` | VARCHAR(50) | 被访人姓名 |
| `host_dept` | VARCHAR(100) | 被访人部门 |
| `host_phone` | VARCHAR(20) | 被访人电话 |
| `visit_time` | DATETIME | 计划来访时间 |
| `leave_time` | DATETIME | 实际离开时间 |
| `status` | VARCHAR(20) | 预约状态（六态） |
| `approver_id` | BIGINT | 审批人ID（自动匹配） |
| `approve_time` | DATETIME | 审批时间 |
| `approve_remark` | VARCHAR(500) | 审批备注 |
| `sms_sent` | CHAR(1) | 短信发送状态 Y/N |
| `sms_content` | VARCHAR(500) | 短信内容 |
| `pass_code` | VARCHAR(20) | 通行码（8位UUID） |
| `has_car` | CHAR(1) | 是否开车 0/1 |
| `car_plate` | VARCHAR(20) | 车牌号 |
| `has_goods` | CHAR(1) | 是否携带物资 0/1 |
| `goods_desc` | VARCHAR(200) | 物资说明 |
| `dept_id` | BIGINT | 数据范围（匹配被访人部门） |
| `tenant_id` | BIGINT | 租户ID |
| `del_flag` | CHAR(1) | 删除标志 0/1 |
| `create_by/time` | | 继承 BaseEntity |
| `update_by/time` | | 继承 BaseEntity |

### 2.2 visitor_log（来访记录表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `log_id` | BIGINT PK | 记录ID |
| `appointment_id` | BIGINT FK | 关联预约ID |
| `visitor_name/phone/id_card/company` | | 同预约表 |
| `visit_reason` | VARCHAR(200) | 来访事由 |
| `host_name` | VARCHAR(50) | 被访人 |
| `host_dept` | VARCHAR(100) | 被访部门 |
| `entry_time` | DATETIME | 进入时间 |
| `exit_time` | DATETIME | 离开时间 |
| `register_type` | VARCHAR(20) | APPOINTMENT / WALKIN |
| `pass_code` | VARCHAR(20) | 通行码 |

---

## 三、状态机

```
                        ┌──────────┐
         创建预约 ─────→ │ PENDING  │ ←──── 创建预约
         (后台新增)      │  待审批   │       (H5登记/现场登记)
                        └────┬─────┘
                    ┌────────┴────────┐
                    │                 │
               审批通过             审批拒绝
                    │                 │
               ┌────▼────┐     ┌─────▼──────┐
               │APPROVED │     │  REJECTED  │ ← 终态
               │ 已通过   │     │  已拒绝     │
               └────┬────┘     └────────────┘
                    │
            通行码验证(到访)
                    │
               ┌────▼────┐
               │VISITING │ ← checkInVisitor() 自动更新
               │ 来访中   │
               └────┬────┘
                    │
              完成来访(离开)
                    │
               ┌────▼────┐
               │COMPLETED│ ← 终态
               │ 已完成   │
               └─────────┘

    任意时刻取消 (PENDING / APPROVED → CANCELLED → 终态)
```

六态枚举：`PENDING | APPROVED | REJECTED | VISITING | COMPLETED | CANCELLED`

---

## 四、三条业务流程（完整调用路径）

### 4.1 流程A：后台预约 → 审批（APPOINTMENT 模式）

```
┌──────────────────────────────────────────────────────────────────────┐
│ Step 1: 创建预约（后台管理员）                                         │
│                                                                      │
│ 前端: visitor/appointment/index.vue                                  │
│   ↓ handleAdd() / submitForm()                                       │
│   ↓ POST /visitor/appointment                                        │
│                                                                      │
│ 后端: VisitorAppointmentController.add()                             │
│   ├─ 参数校验 (@Validated)                                            │
│   ├─ 自动生成通行码 UUID.substring(0,8).toUpperCase()                 │
│   ├─ setCreateBy(getUsername())                                      │
│   └─ visitorAppointmentService.insertAppointment(appointment)        │
│       ├─ setStatus(PENDING)                                          │
│       ├─ tryAssignApprover(appointment)  ← 匹配hostName→sys_user     │
│       │   ├─ sysUserService.selectUserByUserName(hostName) 精确匹配   │
│       │   └─ appointmentMapper.selectUserByNickName(hostName) 昵称匹配│
│       ├─ 匹配成功→setApproverId+setDeptId                            │
│       ├─ 匹配失败→兜底 SecurityUtils.getDeptId()                     │
│       └─ appointmentMapper.insertAppointment(appointment)            │
│                                                                      │
│ 权限: visitor:appointment:add                                        │
└──────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────┐
│ Step 2: 审批操作（审批人）                                             │
│                                                                      │
│ 前端: visitor/approval/index.vue                                     │
│   ↓ created() → getList()                                            │
│   ↓ GET /visitor/appointment/pending                                 │
│                                                                      │
│ 后端: VisitorAppointmentController.pending()                         │
│   ├─ 非admin → approverId = getUserId()                             │
│   └─ visitorAppointmentService.selectPendingList(approverId)         │
│       └─ Mapper: WHERE status='PENDING' AND del_flag='0'             │
│                  AND (approver_id=? OR approver_id IS NULL)          │
│                  [+ DeptScopeInterceptor 注入 dept_id=?]             │
│                                                                      │
│ 审批通过:                                                             │
│   前端: handleApprove(row) → submitApprove()                         │
│   ↓ PUT /visitor/appointment/approve {status:"APPROVED"}             │
│                                                                      │
│ 后端: VisitorAppointmentController.approve()                         │
│   ├─ 参数校验                                                        │
│   └─ visitorAppointmentService.approveAppointment(id,status,remark)  │
│       ├─ 校验: 预约存在 + status=PENDING                             │
│       ├─ setApproverId + setApproveTime                              │
│       ├─ appointmentMapper.updateAppointment()                       │
│       ├─ [通过时] 创建 VisitorLog (registerType=APPOINTMENT)          │
│       │   └─ 防重复: 先查是否已有 log                                │
│       ├─ [通过时] sendApprovalSms() → smsUtil.sendSms(               │
│       │   "visitor_approval", phone,                                 │
│       │   {visitor_name, host_name, pass_code}, 1, null)             │
│       └─ 更新 sms_sent='Y'                                           │
│                                                                      │
│ 审批拒绝:                                                             │
│   前端: handleReject(row) → submitReject()                           │
│   ↓ PUT /visitor/appointment/approve {status:"REJECTED"}             │
│   后端: 同上，不创建log、不发短信                                     │
│                                                                      │
│ 权限: visitor:approval:list / visitor:appointment:approve            │
└──────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────┐
│ Step 3: 通行码验证（保安/闸机）                                       │
│                                                                      │
│ 前端: 闸机扫码 / 浏览器打开 /pass/{code}                              │
│   ↓ GET /pass/{passCode}                                             │
│                                                                      │
│ 后端: VisitorPassController.getPassInfo(passCode)  ← 公开接口         │
│   ├─ visitorAppointmentService.selectAppointmentByPassCode(code)     │
│   ├─ [找到] 返回访客信息 + visitorAppointmentService.checkInVisitor() │
│   │   └─ 更新 status=VISITING                                        │
│   ├─ [未找到] visitorLogService.selectLogByPassCode(code)            │
│   └─ [都未找到] 返回 "通行码无效或已过期"                             │
│                                                                      │
│ 认证: 无需登录 (SecurityConfig: /pass/** permitAll)                  │
└──────────────────────────────────────────────────────────────────────┘
                                    ↓
┌──────────────────────────────────────────────────────────────────────┐
│ Step 4: 完成来访（访客离开）                                          │
│                                                                      │
│ 前端: visitor/appointment/index.vue → 更多 → 完成来访                 │
│   ↓ PUT /visitor/appointment/complete/{id}                           │
│                                                                      │
│ 后端: VisitorAppointmentController.complete()                        │
│   └─ visitorAppointmentService.completeAppointment(id)               │
│       ├─ 校验: status=VISITING 或 APPROVED                           │
│       └─ setStatus(COMPLETED) + setLeaveTime(now)                    │
│                                                                      │
│ 权限: visitor:appointment:complete                                   │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.2 流程B：H5 自助登记（WALKIN 模式）

```
┌──────────────────────────────────────────────────────────────────────┐
│ Step 1: 访客扫码进入 H5 页面                                         │
│                                                                      │
│ 前端: visitor/h5/register.vue (公开页面)                              │
│   ↓ 填写：姓名/电话/被访人/来访事由                                   │
│   ↓ POST /visitor/h5/submit                                          │
│                                                                      │
│ 后端: VisitorH5Controller.submit(appointment)  ← 公开接口             │
│   ├─ 参数校验: visitorName+visitorPhone+hostName 必填                │
│   └─ visitorAppointmentService.registerWalkin(appointment,visitorName)│
│       ├─ 生成 passCode                                                │
│       ├─ this.insertAppointment(appointment)                         │
│       │   ├─ setStatus(PENDING)                                      │
│       │   ├─ tryAssignApprover() → 匹配被访人为审批人                 │
│       │   └─ appointmentMapper.insertAppointment()                   │
│       ├─ 创建 VisitorLog (registerType=WALKIN, entryTime=now)        │
│       └─ 返回 {passCode, appointmentId, log}                         │
│                                                                      │
│ 认证: 无需登录 (SecurityConfig: /visitor/h5/** permitAll)             │
│ 后续: 仍需审批人审批（注意：approver_id已自动设置）                    │
└──────────────────────────────────────────────────────────────────────┘
```

### 4.3 流程C：现场登记（保安台 WALKIN 模式）

```
┌──────────────────────────────────────────────────────────────────────┐
│ Step 1: 保安现场登记                                                  │
│                                                                      │
│ 前端: visitor/log/index.vue → 现场登记按钮                            │
│   ↓ POST /visitor/log                                                │
│                                                                      │
│ 后端: VisitorLogController.add(log)                                  │
│   ├─ 将 VisitorLog 转为 VisitorAppointment                           │
│   └─ visitorAppointmentService.registerWalkin(appointment,getUsername)│
│       └─ 同流程B的 Service 方法，registerType=WALKIN                  │
│                                                                      │
│ 权限: visitor:log:add                                                 │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│ Step 2: 访客离开记录                                                  │
│                                                                      │
│ 前端: visitor/log/index.vue → 记录离开                                │
│   ↓ PUT /visitor/log/exit/{logId}                                    │
│                                                                      │
│ 后端: VisitorLogController.exit(logId)                               │
│   └─ visitorLogService.updateLog(log)                                │
│       └─ setExitTime(now)                                            │
│                                                                      │
│ 权限: visitor:log:edit                                                │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 五、全量 API 清单

### 5.1 预约管理（需认证）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| GET | `/visitor/appointment/list` | `visitor:appointment:list` | 分页查询预约列表 |
| GET | `/visitor/appointment/{id}` | `visitor:appointment:query` | 查询预约详情 |
| POST | `/visitor/appointment` | `visitor:appointment:add` | 新增预约→PENDING |
| PUT | `/visitor/appointment` | `visitor:appointment:edit` | 修改预约 |
| DELETE | `/visitor/appointment/{ids}` | `visitor:appointment:remove` | 批量删除（逻辑删） |
| PUT | `/visitor/appointment/approve` | `visitor:appointment:approve` | 审批→APPROVED/REJECTED |
| PUT | `/visitor/appointment/cancel/{id}` | `visitor:appointment:cancel` | 取消预约→CANCELLED |
| PUT | `/visitor/appointment/complete/{id}` | `visitor:appointment:complete` | 完成来访→COMPLETED |
| GET | `/visitor/appointment/pending` | `visitor:approval:list` | 待审批列表（自动过滤） |
| POST | `/visitor/appointment/export` | `visitor:appointment:export` | 导出 Excel |

### 5.2 公开接口（无需认证）

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/visitor/h5/submit` | H5 自助登记 |
| GET | `/visitor/h5/hosts` | 被访人列表（供H5选择） |
| GET | `/pass/{passCode}` | 通行码验证 + 到访确认 |

### 5.3 来访记录（需认证）

| 方法 | URL | 权限 | 说明 |
|------|-----|------|------|
| GET | `/visitor/log/list` | `visitor:log:list` | 分页查询来访记录 |
| GET | `/visitor/log/{id}` | `visitor:log:query` | 查询记录详情 |
| POST | `/visitor/log` | `visitor:log:add` | 现场登记（创建预约+记录） |
| PUT | `/visitor/log/exit/{id}` | `visitor:log:edit` | 记录离开时间 |
| POST | `/visitor/log/export` | `visitor:log:export` | 导出 Excel |

---

## 六、权限体系

| 权限标识 | 作用 | 典型角色 |
|----------|------|---------|
| `visitor:appointment:list` | 查看预约列表 | 管理员/前台 |
| `visitor:appointment:query` | 查看预约详情 | 管理员/前台 |
| `visitor:appointment:add` | 新增预约 | 管理员 |
| `visitor:appointment:edit` | 修改预约 | 管理员 |
| `visitor:appointment:remove` | 删除预约 | 管理员 |
| `visitor:appointment:approve` | 审批预约 | **被访人/管理员** |
| `visitor:appointment:cancel` | 取消预约 | 管理员 |
| `visitor:appointment:complete` | 完成来访 | 管理员/前台 |
| `visitor:appointment:export` | 导出预约 | 管理员 |
| `visitor:approval:list` | 查看待审批列表 | **被访人/管理员** |
| `visitor:log:list` | 查看来访记录 | 管理员/前台 |
| `visitor:log:query` | 查看记录详情 | 管理员/前台 |
| `visitor:log:add` | 现场登记 | 前台 |
| `visitor:log:edit` | 记录离开 | 前台 |
| `visitor:log:export` | 导出记录 | 管理员 |

**数据范围机制**：
- 管理员（admin）：`selectPendingList(null)` → 无 approver_id 过滤 → 看到全部
- 非管理员：`selectPendingList(当前userId)` → `WHERE (approver_id=userId OR IS NULL) AND dept_id=用户部门`
- `tryAssignApprover()` 自动将 hostName 匹配到 sys_user 并设置 approver_id + dept_id

---

## 七、关键架构决策

### 7.1 被访人→审批人自动匹配

```
创建预约时 tryAssignApprover():
  hostName="陈诺"
    ├─ selectUserByUserName("陈诺")  → user_name 精确匹配
    │   陈诺的 user_name="chennuo" → 不匹配
    │
    └─ selectUserByNickName("陈诺")  → nick_name 匹配（Map返回）
        陈诺的 nick_name="陈诺" → 匹配 userId=108, deptId=101
        → setApproverId(108) + setDeptId(101)
```

### 7.2 短信通知链路

```
审批通过
  └─ VisitorAppointmentServiceImpl.approveAppointment()
      └─ sendApprovalSms()
          └─ smsUtil.sendSms("visitor_approval", phone,
               {visitor_name, host_name, pass_code}, 1, null)
              └─ SmsUtil → biz_code路由 → 阿里云/腾讯云/HTTP 三驱动
```

### 7.3 数据范围穿透

```
DeptScopeInterceptor 全局行为:
  - admin → 不过滤 → 看到全部
  - 非admin → 注入 dept_id = 当前用户.dept_id
  - 预约的 dept_id 在创建时由 tryAssignApprover() 自动设置
  - 匹配失败时兜底 SecurityUtils.getDeptId()
  - H5 公开接口无登录上下文 → 仅依赖 tryAssignApprover 匹配
```

### 7.4 层次结构

```
Controller 层:  参数校验 + 调用 Service + 返回 AjaxResult
Service 层:     业务逻辑 + 事务管理 + 跨表操作
Mapper 层:      数据库访问 + SQL
Entity 层:      数据模型

规则:
  - Controller 禁止注入 Mapper (VisitorPassController 已修复)
  - Controller 禁止包含业务逻辑 (审批后创建log、发短信等已下沉)
  - Service 不直接调其他 Mapper (通过对应 Service)
```

---

## 八、文件索引

### 后端

| 文件 | 路径 |
|------|------|
| Controller - 预约 | `ruoyi-admin/.../visitor/VisitorAppointmentController.java` |
| Controller - H5 | `ruoyi-admin/.../visitor/VisitorH5Controller.java` |
| Controller - 记录 | `ruoyi-admin/.../visitor/VisitorLogController.java` |
| Controller - 通行码 | `ruoyi-admin/.../visitor/VisitorPassController.java` |
| Domain - 预约 | `ruoyi-system/.../domain/VisitorAppointment.java` |
| Domain - 记录 | `ruoyi-system/.../domain/VisitorLog.java` |
| Service 接口 - 预约 | `ruoyi-system/.../service/IVisitorAppointmentService.java` |
| Service 实现 - 预约 | `ruoyi-system/.../service/impl/VisitorAppointmentServiceImpl.java` |
| Service 接口 - 记录 | `ruoyi-system/.../service/IVisitorLogService.java` |
| Service 实现 - 记录 | `ruoyi-system/.../service/impl/VisitorLogServiceImpl.java` |
| Mapper - 预约 | `ruoyi-system/.../mapper/VisitorAppointmentMapper.java` |
| Mapper XML - 预约 | `ruoyi-system/.../mapper/system/VisitorAppointmentMapper.xml` |
| Mapper - 记录 | `ruoyi-system/.../mapper/VisitorLogMapper.java` |
| Mapper XML - 记录 | `ruoyi-system/.../mapper/system/VisitorLogMapper.xml` |
| 安全配置 | `ruoyi-framework/.../config/SecurityConfig.java` |
| 短信工具 | `ruoyi-system/.../sms/SmsUtil.java` |

### 前端

| 页面 | 路径 |
|------|------|
| 预约管理 | `ruoyi-ui/src/views/visitor/appointment/index.vue` |
| 审批管理 | `ruoyi-ui/src/views/visitor/approval/index.vue` |
| 来访记录 | `ruoyi-ui/src/views/visitor/log/index.vue` |
| H5 登记 | `ruoyi-ui/src/views/visitor/h5/register.vue` |
| API 模块 | `ruoyi-ui/src/api/visitor/visitor.js` |

### SQL

| 文件 | 说明 |
|------|------|
| `sql/ry-vue100-migration.sql` | 建表 DDL (visitor_appointment, visitor_log) |
| `sql/ry-vue100-v11-visitor-columns.sql` | 补充字段 (pass_code, has_car 等) |
| `sql/ry-vue100-v15-visitor-refactor.sql` | 重构 DDL (del_flag) |
