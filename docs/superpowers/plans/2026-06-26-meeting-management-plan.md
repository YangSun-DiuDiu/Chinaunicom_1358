# 会议管理系统 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task.

**Goal:** 新增会议管理模块：会议室管理、会议预约(含冲突检测+审批)、会议管理、平板展示页(自动刷新)

**Architecture:** 独立 meeting_ 业务域，按若依标准分层 Domain→Mapper→Service→Controller，平板页为公开白名单 H5 页面 30 秒轮询刷新

**Tech Stack:** Java 17, SpringBoot 4.0.3, MyBatis, Vue 2.6, Element UI 2.15

## Global Constraints

- 平板页面白名单公开访问（无需登录），类似 `/repair-complete`
- 预约冲突检测：同会议室同时间段 APPROVED/PENDING 状态冲突时拒绝
- 多租户：meeting_room 和 meeting_booking 有 dept_id，DeptScopeInterceptor 自动过滤
- 不做短信通知、不做周期性会议、不与 iot_device 关联
- 遵循若依 RBAC 权限体系

---

### Task 1: 数据库建表 + 菜单 SQL

**Files:**
- Create: `sql/meeting_management.sql`

- [ ] **Step 1: 写建表 SQL**

```sql
-- 会议室表
CREATE TABLE meeting_room (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_name VARCHAR(100) NOT NULL COMMENT '会议室名称',
    location VARCHAR(200) COMMENT '位置',
    capacity INT DEFAULT 10 COMMENT '容纳人数',
    equipment VARCHAR(500) COMMENT '设备清单',
    status CHAR(1) DEFAULT '0' COMMENT '状态(0启用 1停用)',
    dept_id BIGINT COMMENT '所属组织',
    create_by VARCHAR(64),
    create_time DATETIME,
    update_by VARCHAR(64),
    update_time DATETIME,
    remark VARCHAR(500),
    INDEX idx_meeting_room_dept (dept_id)
) COMMENT='会议室';

-- 会议预约表
CREATE TABLE meeting_booking (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL COMMENT '会议室ID',
    title VARCHAR(200) NOT NULL COMMENT '会议主题',
    host_name VARCHAR(64) COMMENT '主持人',
    host_phone VARCHAR(20) COMMENT '联系电话',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    attendees INT DEFAULT 1 COMMENT '参会人数',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    dept_id BIGINT COMMENT '所属组织',
    create_by VARCHAR(64),
    create_time DATETIME,
    update_by VARCHAR(64),
    update_time DATETIME,
    remark VARCHAR(500),
    INDEX idx_booking_room (room_id),
    INDEX idx_booking_time (room_id, start_time, end_time),
    INDEX idx_booking_dept (dept_id)
) COMMENT='会议预约';

-- 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('会议管理', 0, 9, 'meeting', NULL, 'M', '0', '0', NULL, 'el-icon-s-order', 'admin', NOW());
SET @meeting_parent = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('会议室管理', @meeting_parent, 1, 'room', 'meeting/room/index', 'C', '0', '0', 'meeting:room:list', '#', 'admin', NOW());

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, icon, create_by, create_time)
VALUES ('会议预约', @meeting_parent, 2, 'booking', 'meeting/booking/index', 'C', '0', '0', 'meeting:booking:list', '#', 'admin', NOW());
```

- [ ] **Step 2: 执行 SQL**

```bash
mysql -h 1.94.26.126 -P 3306 -u sadmin -pChinaunicom@1358 ry101 < sql/meeting_management.sql
```

- [ ] **Step 3: 验证**

```bash
mysql ... -e "SHOW TABLES LIKE 'meeting_%'; SELECT menu_name FROM sys_menu WHERE menu_name LIKE '%会议%';"
```

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: add meeting management database tables and menu"
```

---

### Task 2: 后端 — Domain + Mapper + Service

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/MeetingRoom.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/MeetingBooking.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/mapper/MeetingRoomMapper.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/mapper/MeetingBookingMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/system/MeetingRoomMapper.xml`
- Create: `ruoyi-system/src/main/resources/mapper/system/MeetingBookingMapper.xml`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/IMeetingRoomService.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/IMeetingBookingService.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/MeetingRoomServiceImpl.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/MeetingBookingServiceImpl.java`

**MeetingRoom** extends BaseEntity: roomId, roomName, location, capacity, equipment, status, deptId.

**MeetingBooking** extends BaseEntity: bookingId, roomId, title, hostName, hostPhone, startTime, endTime, attendees, status, deptId.

Mapper XML 按若依标准模板：resultMap + selectVo + selectList + selectById + insert + update + deleteByIds。

IMeetingBookingService 额外方法：
```java
boolean hasConflict(Long roomId, Date startTime, Date endTime, Long excludeBookingId);
List<MeetingBooking> selectTodayByRoom(Long roomId);
```

MeetingBookingServiceImpl.hasConflict：
```java
int count = mapper.countConflict(roomId, startTime, endTime, excludeBookingId);
return count > 0;
```
Mapper XML `countConflict`：
```xml
<select id="countConflict" resultType="int">
    SELECT COUNT(*) FROM meeting_booking
    WHERE room_id = #{roomId} AND status IN ('PENDING','APPROVED')
    AND start_time &lt; #{endTime} AND end_time &gt; #{startTime}
    <if test="excludeBookingId != null">AND booking_id != #{excludeBookingId}</if>
</select>
```

- [ ] **Compile**: `mvn compile -pl ruoyi-system -am -DskipTests` → BUILD SUCCESS
- [ ] **Commit**: `git add -A && git commit -m "feat: add meeting domain, mapper, and service"`

---

### Task 3: 后端 — Controllers (会议室 + 预约 + 平板)

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/meeting/MeetingRoomController.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/meeting/MeetingBookingController.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/meeting/MeetingBoardController.java`
- Modify: `ruoyi-admin/src/main/resources/application.yml` (白名单加 `/meeting/board/**`)

**MeetingRoomController** (`/meeting/room`)：
- `GET /list` — 分页列表 @PreAuthorize meeting:room:list
- `GET /{id}` — 详情
- `POST` — 新增 @PreAuthorize meeting:room:add
- `PUT` — 修改 @PreAuthorize meeting:room:edit
- `DELETE /{id}` — 删除 @PreAuthorize meeting:room:remove

**MeetingBookingController** (`/meeting/booking`)：
- `GET /list` — 分页列表（按 roomId/status/日期 筛选）
- `GET /{id}` — 详情
- `POST` — 新增（含冲突检测，冲突返回 error）
- `PUT` — 修改
- `PUT /approve/{id}` — 审批通过
- `PUT /reject/{id}` — 审批拒绝
- `PUT /cancel/{id}` — 取消

**MeetingBoardController** (`/meeting/board`) — **无需登录**：
```java
@GetMapping("/{roomId}")
public AjaxResult board(@PathVariable Long roomId) {
    MeetingRoom room = roomService.selectById(roomId);
    List<MeetingBooking> todayList = bookingService.selectTodayByRoom(roomId);
    // 计算 currentStatus / currentBooking / nextBooking
    Map<String, Object> result = new HashMap<>();
    result.put("room", room);
    result.put("todayBookings", todayList);
    result.put("currentStatus", calcCurrentStatus(todayList));
    result.put("currentBooking", getCurrentBooking(todayList));
    result.put("nextBooking", getNextBooking(todayList));
    return success(result);
}

@GetMapping("/{roomId}/refresh")
public AjaxResult refresh(@PathVariable Long roomId) {
    // 轻量返回 currentStatus + nextBooking
}
```

- [ ] **Compile**: `mvn compile -pl ruoyi-admin -am -DskipTests` → BUILD SUCCESS
- [ ] **Commit**: `git add -A && git commit -m "feat: add meeting controllers with board public API"`

---

### Task 4: 前端 — 会议室管理页 + 会议预约页 + 会议管理页

**Files:**
- Create: `ruoyi-ui/src/views/meeting/room/index.vue`
- Create: `ruoyi-ui/src/views/meeting/booking/index.vue`
- Create: `ruoyi-ui/src/views/meeting/manage/index.vue`
- Create: `ruoyi-ui/src/api/meeting/room.js`
- Create: `ruoyi-ui/src/api/meeting/booking.js`
- Modify: `ruoyi-ui/src/router/index.js` (添加路由)

**会议室管理** (`room/index.vue`)：标准若依 CRUD 页面模板
- 搜索区：roomName, status 下拉
- 表格：roomName, location, capacity, equipment, status, 操作(编辑/删除)
- 弹窗：roomName, location, capacity, equipment

**会议预约** (`booking/index.vue`)：
- 顶部：会议室下拉筛选 + 日期选择
- 主体：列表视图（表格，按 start_time 排序）
- 新建弹窗：room_id 下拉, title, start_time, end_time, attendees, host_name, host_phone
- 状态列：PENDING(橙色)/APPROVED(绿色)/REJECTED(红色)/CANCELLED(灰色) 标签
- 管理员操作：approve/reject 按钮

**会议管理** (`manage/index.vue`)：简单列表页，筛选 APPROVED 状态，显示今日会议。

- [ ] **Commit**: `git add -A && git commit -m "feat: add meeting frontend admin pages"`

---

### Task 5: 前端 — 平板展示页 + 路由白名单

**Files:**
- Create: `ruoyi-ui/src/views/meeting/board/index.vue`
- Modify: `ruoyi-ui/src/router/index.js` (constantRoutes 添加 `/meeting/board`)

**平板展示页** (`board/index.vue`)：
```vue
<template>
  <div class="board-page">
    <div class="header">
      <h1>🏢 {{ room.roomName }}</h1>
      <span>容纳: {{ room.capacity }}人 | 设备: {{ room.equipment }}</span>
    </div>
    <div class="status-area" :class="currentStatus">
      <div class="status-icon">{{ statusIcon }}</div>
      <div class="status-text">{{ statusText }}</div>
      <div class="current-title" v-if="currentBooking">{{ currentBooking.title }}</div>
      <div class="current-time" v-if="currentBooking">{{ currentBooking.startTime }}-{{ currentBooking.endTime }}</div>
    </div>
    <div class="today-list">
      <h3>📅 今日预约</h3>
      <div v-for="b in todayBookings" :key="b.bookingId" class="booking-item" :class="getBookingClass(b)">
        <span>{{ b.startTime }}-{{ b.endTime }}</span>
        <span>{{ b.title }}</span>
        <span class="tag">{{ getBookingStatus(b) }}</span>
      </div>
    </div>
    <div class="next-area" v-if="nextBooking">
      <span>⏰ 下一场: {{ nextBooking.startTime }}</span>
      <span class="countdown">倒计时 {{ countdown }}</span>
    </div>
  </div>
</template>
```

**JS 逻辑**：
- `mounted()`: 从 `$route.query.roomId` 读取会议室 ID，调 `GET /meeting/board/{roomId}` 加载数据
- `setInterval(() => { refresh() }, 30000)` — 30秒轮询
- `countdown` 用 `setInterval` 每秒递减
- `currentStatus` 计算逻辑：比较当前时间与 booking.startTime/endTime

**路由**：`/meeting/board` 添加到 `constantRoutes`（无需登录）。

- [ ] **Commit**: `git add -A && git commit -m "feat: add meeting board display page with auto-refresh"`

---

### Task 6: 后端白名单 + 路由 + 编译部署验证

**Files:**
- Modify: `ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java` (白名单加 `/meeting/board/**`)
- Modify: `ruoyi-framework/src/main/java/com/ruoyi/framework/config/properties/PermitAllUrlProperties.java` (如有 @Anonymous 则自动)

- [ ] **Step 1: 添加白名单**

在 `SecurityConfig.java` 的 `permitAll` 链中添加：
```java
.antMatchers("/meeting/board/**").permitAll()
```

- [ ] **Step 2: 全量构建**

```bash
cd <项目根> && mvn clean package -DskipTests -q && cd ruoyi-ui && npm run build:prod
```

- [ ] **Step 3: 部署**

```bash
scp ruoyi-admin/target/ruoyi-admin.jar root@1.94.26.126:/root/zhihuiyuanqu20260625/
scp -r ruoyi-ui/dist/* root@1.94.26.126:/usr/share/nginx/zhihuiyuanqu/dist/
ssh root@1.94.26.126 'cd /root/zhihuiyuanqu20260625 && bash stop-server.sh && bash start-server.sh && nginx -s reload'
```

- [ ] **Step 4: 验证**

```bash
# 后端
curl -s -o /dev/null -w "HTTP %{http_code}" http://1.94.26.126:8090/
# 前端
curl -s -o /dev/null -w "HTTP %{http_code}" http://1.94.26.126:80/
# 平板 API（无需登录）
curl -s http://1.94.26.126:8090/meeting/board/1 | head -c 200
# 预约冲突检测
TOKEN=$(登录获取)
curl -s -X POST http://1.94.26.126:8090/meeting/booking -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"roomId":1,"title":"测试","startTime":"2026-06-26 09:00","endTime":"2026-06-26 10:00"}'
```

- [ ] **Step 5: Commit & Push**

```bash
git add -A && git commit -m "chore: deploy meeting management feature" && git push origin master
```
