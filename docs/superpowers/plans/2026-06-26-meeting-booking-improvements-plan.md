# 会议预约改进 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task.

**Goal:** 拆分预约/审批为两页、新增时间全景图、修复会议管理数据、会议室表格显示平板地址

**Architecture:** 后端新增 slots 接口 + 预约页加 my 过滤；前端拆分审批页 + 预约页加时间轴 + 管理页改筛选 + 会议室页加地址列

**Tech Stack:** Java 17, SpringBoot 4.0.3, Vue 2.6, Element UI 2.15

## Global Constraints

- 平板展示页不动、冲突检测后端逻辑不动、不新增数据库表
- 审批页独立权限 `meeting:approval:list`
- 时间轴仅展示 APPROVED 和 PENDING 状态的预约

---

### Task 1: 数据库菜单 + 权限调整

- [ ] **Step 1: 删除旧会议预约菜单，新建预约和审批菜单**

```sql
DELETE FROM sys_role_menu WHERE menu_id=2033;
DELETE FROM sys_menu WHERE menu_id=2033;

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('会议预约', 2031, 2, 'booking', 'meeting/booking/index', 'C', '0', '0', 'meeting:booking:list', 'admin', NOW());
SET @booking_id = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('会议审批', 2031, 3, 'approval', 'meeting/approval/index', 'C', '0', '0', 'meeting:approval:list', 'admin', NOW());
SET @approval_id = LAST_INSERT_ID();

INSERT INTO sys_role_menu (role_id, menu_id) VALUES (1, @booking_id), (1, @approval_id);
```

- [ ] **Step 2: 执行 + 验证**

```bash
mysql -h 1.94.26.126 -P 3306 -u sadmin -pChinaunicom@1358 ry101 < sql/meeting_menu_fix.sql
mysql ... -e "SELECT menu_id, menu_name, perms FROM sys_menu WHERE parent_id=2031;"
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "fix: split meeting booking and approval into separate menus with permissions"
```

---

### Task 2: 后端 — slots 接口 + 预约过滤

- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/meeting/MeetingBookingController.java`
- Modify: `ruoyi-system/src/main/resources/mapper/system/MeetingBookingMapper.xml`

**slots 接口**：
```java
@GetMapping("/slots/{roomId}")
public AjaxResult slots(@PathVariable Long roomId, @RequestParam String date) {
    List<MeetingBooking> list = bookingService.selectSlots(roomId, date);
    return success(list);
}
```

**Mapper** (新增 `selectSlots`)：
```xml
<select id="selectSlots" resultMap="MeetingBookingResult">
    SELECT * FROM meeting_booking
    WHERE room_id=#{roomId} AND DATE(start_time)=#{date}
    AND status IN ('PENDING','APPROVED')
    ORDER BY start_time
</select>
```

**预约列表过滤**：`selectList` 增加 `createBy` 参数（预约页传当前用户名）。

- [ ] **Step 1**: `mvn compile -pl ruoyi-admin -am -DskipTests` → BUILD SUCCESS
- [ ] **Step 2**: `git add -A && git commit -m "feat: add slots endpoint and booking my-filter"`

---

### Task 3: 前端 — 审批页 + 预约页改造 + 管理页修复

- Create: `ruoyi-ui/src/views/meeting/approval/index.vue`
- Modify: `ruoyi-ui/src/views/meeting/booking/index.vue`
- Modify: `ruoyi-ui/src/views/meeting/manage/index.vue`
- Modify: `ruoyi-ui/src/router/index.js` (添加 approval 路由)

**审批页** (`approval/index.vue`):
- 列表默认 `status=PENDING`，按 start_time 升序
- 表格：title, roomName, hostName, startTime, endTime, attendees, status
- 操作：通过(PUT /approve)、拒绝(PUT /reject)

**预约页** (`booking/index.vue`):
- 列表自动加 `my=1` 过滤当前用户
- 新建弹窗选会议室后自动加载 slots
- 时间轴显示已占用时段（色块），点击空闲区域选时间

**管理页** (`manage/index.vue`):
- 默认不限制 status
- 增加日期选择器 + 会议室筛选

**路由**：添加 `/meeting/approval` 到 `staticRoutes`

- [ ] `npm run build:prod` → DONE
- [ ] `git add -A && git commit -m "feat: split approval page, add timeline, fix manage data"`

---

### Task 4: 前端 — 会议室管理加平板地址列

- Modify: `ruoyi-ui/src/views/meeting/room/index.vue`

表格新增列：
```html
<el-table-column label="平板地址" min-width="300">
  <template slot-scope="{row}">
    <el-input :value="'http://1.94.26.126:80/meeting/board?roomId='+row.roomId" size="mini" readonly style="width:240px"/>
    <el-button size="mini" icon="el-icon-document-copy" style="margin-left:4px" @click="copyBoardUrl(row)">复制</el-button>
  </template>
</el-table-column>
```
方法：
```javascript
copyBoardUrl(row) {
  const url = 'http://1.94.26.126:80/meeting/board?roomId=' + row.roomId
  navigator.clipboard.writeText(url).then(() => this.$message.success('已复制'))
}
```

- [ ] `npm run build:prod` → DONE
- [ ] `git add -A && git commit -m "feat: add board URL column to meeting room table"`

---

### Task 5: 构建部署验证

- [ ] **构建**: `mvn clean package -DskipTests -q && cd ruoyi-ui && npm run build:prod`
- [ ] **部署**: scp jar + dist to server, restart
- [ ] **验证**: 后端 8090 → 200, 前端 80 → 200, slots API, 审批页加载
- [ ] **Commit**: `chore: deploy meeting booking improvements` + push
