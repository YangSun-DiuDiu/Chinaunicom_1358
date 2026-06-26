# 会议管理系统 — 设计方案

> **日期**: 2026-06-26
> **状态**: 已审核

---

## 1. 概述

在智慧园区管理系统上新增会议管理模块，包含：会议室管理、会议预约管理、会议管理、平板展示页。

---

## 2. 数据库设计

### 2.1 meeting_room（会议室）

| 列 | 类型 | 说明 |
|----|------|------|
| room_id | BIGINT PK AUTO_INCREMENT | 主键 |
| room_name | VARCHAR(100) NOT NULL | 会议室名称 |
| location | VARCHAR(200) | 位置描述 |
| capacity | INT DEFAULT 10 | 容纳人数 |
| equipment | VARCHAR(500) | 设备清单 |
| status | CHAR(1) DEFAULT '0' | 0启用/1停用 |
| dept_id | BIGINT | 所属组织 |
| create_by / create_time / update_by / update_time / remark | | BaseEntity 审计字段 |

### 2.2 meeting_booking（会议预约）

| 列 | 类型 | 说明 |
|----|------|------|
| booking_id | BIGINT PK AUTO_INCREMENT | 主键 |
| room_id | BIGINT NOT NULL | 会议室ID |
| title | VARCHAR(200) NOT NULL | 会议主题 |
| host_name | VARCHAR(64) | 主持人 |
| host_phone | VARCHAR(20) | 联系电话 |
| start_time | DATETIME NOT NULL | 开始时间 |
| end_time | DATETIME NOT NULL | 结束时间 |
| attendees | INT DEFAULT 1 | 参会人数 |
| status | VARCHAR(20) DEFAULT 'PENDING' | PENDING/APPROVED/REJECTED/CANCELLED |
| dept_id | BIGINT | 所属组织 |
| create_by / create_time / update_by / update_time / remark | | BaseEntity 审计字段 |

**索引**：(room_id, start_time, end_time) 联合索引用于冲突检测。

---

## 3. 后端设计

### 3.1 Controller

| Controller | 路径 | 功能 |
|------------|------|------|
| `MeetingRoomController` | `/meeting/room` | 会议室 CRUD + list/get/add/edit/delete + treeselect |
| `MeetingBookingController` | `/meeting/booking` | 预约 CRUD + approve/reject/cancel + 冲突检测 |
| `MeetingBoardController` | `/meeting/board` | **公开接口**，平板展示用 |

### 3.2 平板 API（无需登录，白名单）

| 端点 | 说明 |
|------|------|
| `GET /meeting/board/{roomId}` | 返回会议室信息 + 今日全部预约 + 当前状态 |
| `GET /meeting/board/{roomId}/refresh` | 轻量轮询：当前状态 + 今日预约列表 + 下一场倒计时 |

返回格式：
```json
{
  "room": { "roomName": "3F-会议室A", "capacity": 12, "equipment": "投影仪/白板" },
  "currentStatus": "IN_USE",  // FREE / IN_USE / NEXT_SOON
  "currentBooking": { "title": "项目评审会", "hostName": "张三", "startTime": "13:00", "endTime": "15:00" },
  "todayBookings": [ { "title": "周例会", "startTime": "09:00", "endTime": "10:00", "status": "FINISHED" }, ... ],
  "nextBooking": { "title": "客户洽谈", "startTime": "15:30", "countdown": 750 }
}
```

### 3.3 冲突检测

```sql
SELECT COUNT(*) FROM meeting_booking
WHERE room_id = ? AND status IN ('PENDING', 'APPROVED')
AND start_time < ? AND end_time > ?
```
count > 0 → 返回错误"该时间段已被预约"。

### 3.4 多租户

- 会议室和预约表均有 `dept_id`，由 `DeptScopeInterceptor` 自动过滤。
- 管理员可跨组织查看。

### 3.5 Domain / Service / Mapper

按若依标准分层：`MeetingRoom` / `MeetingBooking` 实体 → Mapper 接口 + XML → Service 接口 + Impl → Controller。

---

## 4. 前端设计

### 4.1 管理后台（需要登录）

| 页面 | 路由 | 功能 |
|------|------|------|
| 会议室管理 | `/meeting/room` | 表格 CRUD + 搜索（roomName/location/status） |
| 会议预约 | `/meeting/booking` | 日历视图 + 列表，新建弹窗（选会议室+时间+主题），审批按钮 |
| 会议管理 | `/meeting/manage` | 已审批会议的签到/开始/结束操作 |

**菜单结构**：一级菜单"会议管理" → 会议室管理 / 会议预约 / 会议管理

### 4.2 平板展示页（无需登录）`/meeting/board?roomId=xxx`

```
┌──────────────────────┐
│ 🏢 会议室名  容纳:X人  │
│ 设备: xxx            │
├──────────────────────┤
│     🔴 使用中         │
│  项目评审会 13-15时    │
├──────────────────────┤
│ 📅 今日预约           │
│ 09:00-10:00 周例会   │
│ 13:00-15:00 项目评审  │
│ 15:30-16:30 客户洽谈  │
├──────────────────────┤
│ ⏰ 下一场 15:30       │
│    倒计时 12:34       │
└──────────────────────┘
```

**自动刷新**：每 30 秒调 `/refresh` 接口，状态变化自动切换显示，倒计时前端实时更新。

---

## 5. 权限设计

| 权限标识 | 说明 |
|----------|------|
| `meeting:room:list/query/add/edit/remove` | 会议室管理 |
| `meeting:booking:list/query/add/edit/remove/approve` | 会议预约 |
| `meeting:manage:list` | 会议管理 |

---

## 6. 不做的事

- 不发送会议短信通知（后续迭代）
- 不与现有 `iot_device` 设备表关联
- 平板页面不做登录鉴权（白名单公开访问）
- 不做周期性重复会议（如"每周一上午"）

---

## 7. 验证标准

- [ ] 会议室 CRUD 功能正常
- [ ] 预约冲突检测生效
- [ ] 审批流程完整（提交→审批/拒绝）
- [ ] 平板页面正确显示会议室状态
- [ ] 平板页面自动刷新
- [ ] 多租户数据隔离正常
