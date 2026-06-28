# 公寓房源 + 卡片可视化 — 设计方案

> **日期**: 2026-06-28 | **状态**: 已审核 | **来源**: 公寓智能预付费管理系统 V1.2

---

## 1. 范围

- 公寓信息 CRUD（表格）
- 房间管理 CRUD（传统表格）
- **房间卡片可视化**（固定卡片+居中弹窗，与表格数据双向同步）
- 房间 6 色状态：绿/蓝/黄/橙/红/灰

## 2. 数据库

### apartment_info

| 列 | 类型 | 说明 |
|----|------|------|
| apartment_id | BIGINT PK | 主键 |
| apartment_name | VARCHAR(100) | 公寓名称 |
| address | VARCHAR(200) | 地址 |
| contact_phone | VARCHAR(20) | 联系电话 |
| area_desc | VARCHAR(500) | 区域描述 |
| dept_id | BIGINT | 所属组织 |
| +BaseEntity | | 审计字段 |

### room_info

| 列 | 类型 | 说明 |
|----|------|------|
| room_id | BIGINT PK | 主键 |
| apartment_id | BIGINT | 公寓ID |
| room_code | VARCHAR(20) | 房间编号 |
| building | VARCHAR(50) | 楼栋 |
| floor | VARCHAR(50) | 楼层 |
| unit_type | VARCHAR(50) | 户型 |
| area | DECIMAL(10,2) | 面积 |
| rent_start | DATE | 租期开始 |
| rent_end | DATE | 租期结束 |
| tenant_count | INT DEFAULT 0 | 合租人数 |
| door_type | VARCHAR(20) | ONLINE/NFC |
| device_status | VARCHAR(20) | BOUND/UNBOUND |
| status | VARCHAR(20) DEFAULT 'GREEN' | GREEN/BLUE/YELLOW/ORANGE/RED/GRAY |
| dept_id | BIGINT | 所属组织 |
| +BaseEntity | | 审计字段 |

## 3. 后端

### Controller

| Controller | 路径 | 说明 |
|------------|------|------|
| ApartmentController | `/hostel/apartment` | 公寓 CRUD |
| RoomController | `/hostel/room` | 房间 CRUD + 列表 + 卡片数据 |

### 卡片接口

`GET /hostel/room/card/list` — 返回房间列表（仅静态字段），支持筛选：公寓/楼栋/楼层/状态

### 弹窗接口

`GET /hostel/room/{id}/detail` — 返回完整房间信息（供弹窗加载）

## 4. 前端

### 4.1 公寓信息 (`/hostel/apartment`)

标准 CRUD 表格页：搜索（名称）+ 表格（名称/地址/电话）+ 新增/编辑弹窗

### 4.2 房间管理-表格 (`/hostel/room`)

标准 CRUD 表格页：搜索（公寓/楼栋/房间/状态）+ 表格 + 新增/编辑弹窗。顶部【表格/卡片】切换按钮。

### 4.3 房间卡片视图 (`/hostel/room/card`) — 核心

```
卡片 280x220px，弹性网格自适应换行
6 色背景：绿/蓝/黄/橙/红/灰
仅展示：房间编号、户型、面积、公寓-楼栋-楼层、合租人数、门锁类型、设备状态、租期起止
操作：点击卡片 → 居中弹窗
```

**居中弹窗**：6 Tab 骨架
- 基础信息 — 完整房间字段 + 编辑表单 ✅
- 租客管理 — "功能开发中"
- 设备绑定 — "功能开发中"
- 账单 — "功能开发中"
- 能耗分析 — "功能开发中"
- 报修 — "功能开发中"

### 4.4 筛选栏

公寓下拉、楼栋、楼层、状态多选，表格/卡片共用。

### 4.5 菜单

```
公寓房源管理 (M)
├─ 公寓信息 (C)
├─ 房间管理-表格 (C)
└─ 房间卡片视图 (C)
```

## 5. 不做的事

- 租客/设备/账单/能耗/报修（后续子项目）
- 入住办理/退租结算
- MQTT/设备指令对接
- AI 分析

## 6. 验证标准

- [ ] 公寓 CRUD 正常
- [ ] 房间表格 CRUD 正常
- [ ] 卡片视图正确渲染 6 色
- [ ] 卡片-表格数据实时同步
- [ ] 弹窗 6 Tab 骨架正确
- [ ] 弹窗基础信息 Tab 可编辑
- [ ] 编译通过 + 部署成功
