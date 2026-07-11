# 公寓房源 + 卡片可视化 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task.

**Goal:** 新增公寓信息管理、房间表格 CRUD、房间卡片可视化视图（6色、固定尺寸、居中弹窗、6 Tab 骨架）

**Architecture:** 两张表 apartment_info + room_info；标准若依分层 Domain→Mapper→Service→Controller；前端卡片视图与表格共用API；弹窗 6 Tab 预埋骨架

**Tech Stack:** Java 17, SpringBoot 4.0.3, MyBatis, Vue 2.6, Element UI 2.15

## Global Constraints

- 卡片 280x220px固定尺寸、弹性网格自适应换行
- 6色状态：绿/蓝/黄/橙/红/灰
- 弹窗 6 Tab：基础信息✅ + 5个"功能开发中"
- 表格/卡片数据双向同步（共用一套API）
- 租客/设备/账单/能耗/报修 不做

---

### Task 1: 数据库建表 + 菜单 SQL

**Files:** Create `sql/hostel_management.sql`

- [ ] **建表 SQL**

```sql
CREATE TABLE apartment_info (
    apartment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    apartment_name VARCHAR(100) NOT NULL COMMENT '公寓名称',
    address VARCHAR(200) COMMENT '地址',
    contact_phone VARCHAR(20) COMMENT '联系电话',
    area_desc VARCHAR(500) COMMENT '区域描述',
    dept_id BIGINT COMMENT '所属组织',
    create_by VARCHAR(64), create_time DATETIME,
    update_by VARCHAR(64), update_time DATETIME,
    remark VARCHAR(500),
    INDEX idx_apt_dept (dept_id)
) COMMENT='公寓信息';

CREATE TABLE room_info (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_code VARCHAR(20) NOT NULL COMMENT '房间编号',
    apartment_id BIGINT COMMENT '公寓ID',
    building VARCHAR(50) COMMENT '楼栋',
    floor VARCHAR(50) COMMENT '楼层',
    unit_type VARCHAR(50) COMMENT '户型',
    area DECIMAL(10,2) COMMENT '面积(m²)',
    rent_start DATE COMMENT '租期开始',
    rent_end DATE COMMENT '租期结束',
    tenant_count INT DEFAULT 0 COMMENT '合租人数',
    door_type VARCHAR(20) DEFAULT 'NFC' COMMENT 'ONLINE/NFC',
    device_status VARCHAR(20) DEFAULT 'UNBOUND' COMMENT 'BOUND/UNBOUND',
    status VARCHAR(20) DEFAULT 'GREEN' COMMENT 'GREEN/BLUE/YELLOW/ORANGE/RED/GRAY',
    dept_id BIGINT COMMENT '所属组织',
    create_by VARCHAR(64), create_time DATETIME,
    update_by VARCHAR(64), update_time DATETIME,
    remark VARCHAR(500),
    INDEX idx_room_apt (apartment_id),
    INDEX idx_room_status (status),
    INDEX idx_room_dept (dept_id)
) COMMENT='房间信息';

-- 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('公寓房源管理', 0, 10, 'hostel', NULL, 'M', '0', '0', NULL, 'admin', NOW());
SET @hostel_parent = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('公寓信息', @hostel_parent, 1, 'apartment', 'hostel/apartment/index', 'C', '0', '0', 'hostel:apartment:list', 'admin', NOW());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('房间管理-表格', @hostel_parent, 2, 'room', 'hostel/room/index', 'C', '0', '0', 'hostel:room:list', 'admin', NOW());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('房间卡片视图', @hostel_parent, 3, 'room/card', 'hostel/room/card', 'C', '0', '0', 'hostel:room:card', 'admin', NOW());
```

- [ ] **执行** `mysql < sql/hostel_management.sql`
- [ ] **验证** 两表存在 + 4 菜单创建
- [ ] **Commit** `feat: add hostel apartment and room tables + menus`

---

### Task 2: 后端 Domain + Mapper + Service

**Files:** 创建 ruoyi-system 下标准文件：
- `domain/ApartmentInfo.java`, `domain/RoomInfo.java` (extends BaseEntity)
- `mapper/ApartmentInfoMapper.java`, `mapper/RoomInfoMapper.java`
- `resources/mapper/system/ApartmentInfoMapper.xml`, `resources/mapper/system/RoomInfoMapper.xml`
- `service/IApartmentInfoService.java`, `service/IRoomInfoService.java`
- `service/impl/ApartmentInfoServiceImpl.java`, `service/impl/RoomInfoServiceImpl.java`

**RoomInfo 实体**：roomId, roomCode, apartmentId, building, floor, unitType, area, rentStart(Date), rentEnd(Date), tenantCount, doorType, deviceStatus, status, deptId。

**RoomInfoMapper 额外方法**：
```java
List<RoomInfo> selectRoomCardList(RoomInfo room); // 用于卡片视图，支持 apartmentId/building/floor/status 筛选
```

**关键**: 编译必须通过 `mvn compile -pl ruoyi-system -am -DskipTests`。

- [ ] **Commit** `feat: add hostel domain, mapper, and service`

---

### Task 3: 后端 Controllers

**Files:**
- Create: `ruoyi-admin/.../controller/hostel/ApartmentInfoController.java`
- Create: `ruoyi-admin/.../controller/hostel/RoomController.java`

**ApartmentInfoController** (`/hostel/apartment`): 标准 CRUD — list/get/add/edit/remove

**RoomController** (`/hostel/room`):
- `GET /list` — 表格分页列表
- `GET /{id}` — 单条详情
- `POST` — 新增
- `PUT` — 修改
- `DELETE /{ids}` — 删除
- `GET /card/list` — 卡片视图数据（仅返回静态字段，加筛选参数）
- `GET /{id}/detail` — 弹窗详情（完整信息）

**权限**：`hostel:apartment:list`、`hostel:room:list`、`hostel:room:card` 等。

**编译**: `mvn compile -pl ruoyi-admin -am -DskipTests` → BUILD SUCCESS

- [ ] **Commit** `feat: add hostel apartment and room controllers`

---

### Task 4: 前端页面（4页）

**Files:**
- Create: `ruoyi-ui/src/views/hostel/apartment/index.vue` — 公寓 CRUD 表格
- Create: `ruoyi-ui/src/views/hostel/room/index.vue` — 房间表格 CRUD + 顶部切换按钮
- Create: `ruoyi-ui/src/views/hostel/room/card.vue` — 卡片可视化
- Modify: `ruoyi-ui/src/router/index.js` — 添加 3 路由

**公寓页**: 搜索（名称）+ 表格（名称/地址/电话）+ 弹窗 CRUD

**房间表格页**: 搜索（公寓下拉/楼栋/房间/状态）+ 表格 + 弹窗 + 顶部【📋 表格 / 🎴 卡片】切换按钮

**卡片页核心逻辑**:
```vue
<template>
  <div class="card-container">
    <el-form :inline="true" class="filter-bar">
      <!-- 公寓/楼栋/楼层/状态筛选 -->
    </el-form>
    <div class="card-grid">
      <div v-for="room in cardList" :key="room.roomId" class="room-card"
        :class="'card-' + room.status" @click="openDetail(room)">
        <div class="card-header">{{ room.roomCode }}</div>
        <div class="card-body">
          <p>{{ room.unitType }} | {{ room.area }}m²</p>
          <p>{{ room.building }}-{{ room.floor }}</p>
          <p>合租: {{ room.tenantCount }}人 | {{ room.doorType === 'ONLINE' ? '联网锁' : 'NFC锁' }}</p>
          <p>{{ room.deviceStatus === 'BOUND' ? '设备已绑定' : '未绑定' }}</p>
        </div>
        <div class="card-footer">{{ room.rentStart }} ~ {{ room.rentEnd }}</div>
      </div>
    </div>
    <!-- 详情弹窗 -->
    <el-dialog :visible.sync="detailOpen" width="80%" top="5vh">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基础信息">...</el-tab-pane>
        <el-tab-pane label="租客管理">功能开发中</el-tab-pane>
        <el-tab-pane label="设备绑定">功能开发中</el-tab-pane>
        <el-tab-pane label="账单">功能开发中</el-tab-pane>
        <el-tab-pane label="能耗分析">功能开发中</el-tab-pane>
        <el-tab-pane label="报修">功能开发中</el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>
```

**卡片样式**: 280x220px, 弹性 `flex-wrap`, 6色通过 CSS class: `.card-GREEN { background: #e8f5e9; }` 等。

**构建**: `npm run build:prod` → DONE

- [ ] **Commit** `feat: add hostel frontend pages with card view`

---

### Task 5: 构建部署验证

- [ ] `mvn clean package -DskipTests -q` + `cd ruoyi-ui && npm run build:prod`
- [ ] scp JAR + dist to server, restart
- [ ] 验证：后端 8090 200，前端 80 200，`/hostel/room/card/list` API 返回数据
- [ ] **Commit**: `chore: deploy hostel management feature` + push
