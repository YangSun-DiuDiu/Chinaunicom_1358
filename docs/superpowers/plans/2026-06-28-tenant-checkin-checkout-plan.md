# 租客档案 + 入住退租 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended)

**Goal:** 新增租客档案管理、入住办理、退租结算、租期续费；卡片弹窗"租客管理"Tab 填充真实数据

**Architecture:** tenant_info 表 + TenantController CRUD + check-in/check-out/renew 操作联动 room_info 状态更新

**Tech Stack:** Java 17, SpringBoot 4.0.3, MyBatis, Vue 2.6, Element UI 2.15

## Global Constraints

- 入住→房间变BLUE + tenant_count+1，退租完→变GREEN + 归零
- 租客管理 Tab 从空壳改为真实数据
- 不涉及费率/分摊/扣费

---

### Task 1: 数据库 tenant_info 表 + 菜单

```sql
CREATE TABLE tenant_info (
    tenant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_name VARCHAR(64) NOT NULL COMMENT '姓名',
    id_card VARCHAR(18) COMMENT '身份证号',
    phone VARCHAR(20) COMMENT '手机号',
    gender CHAR(1) DEFAULT 'M' COMMENT 'M/F',
    room_id BIGINT COMMENT 'FK→room_info',
    check_in_date DATE COMMENT '入住日期',
    check_out_date DATE COMMENT '退租日期',
    rent_start DATE COMMENT '租期开始',
    rent_end DATE COMMENT '租期结束',
    status VARCHAR(20) DEFAULT 'NORMAL' COMMENT 'NORMAL/CHECKED_OUT',
    openid VARCHAR(64) COMMENT '微信openid',
    push_enabled CHAR(1) DEFAULT '1' COMMENT '推送开关',
    dept_id BIGINT, create_by VARCHAR(64), create_time DATETIME,
    update_by VARCHAR(64), update_time DATETIME, remark VARCHAR(500)
) COMMENT='租客信息';

-- 菜单
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('租客管理', 0, 11, 'tenant', NULL, 'M', '0', '0', NULL, 'admin', NOW());
SET @tparent = LAST_INSERT_ID();

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('租客档案', @tparent, 1, 'list', 'hostel/tenant/index', 'C', '0', '0', 'hostel:tenant:list', 'admin', NOW());
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, visible, status, perms, create_by, create_time)
VALUES ('入住办理', @tparent, 2, 'checkin', 'hostel/tenant/checkin', 'C', '0', '0', 'hostel:tenant:checkin', 'admin', NOW());
```

- [ ] 执行 + 验证
- [ ] **Commit** `feat: add tenant_info table and menus`

---

### Task 2: 后端 Domain + Mapper + Service + Controller

**Files:**
- Create: `domain/TenantInfo.java` (extends BaseEntity)
- Create: `mapper/TenantInfoMapper.java` + XML
- Create: `service/ITenantInfoService.java` + `impl/TenantInfoServiceImpl.java`
- Create: `ruoyi-admin/.../controller/hostel/TenantController.java`

**TenantController 关键方法**:
```java
@PostMapping("/check-in")
public AjaxResult checkIn(@RequestBody Map<String,Object> body) {
    Long tenantId = ((Number)body.get("tenantId")).longValue();
    Long roomId = ((Number)body.get("roomId")).longValue();
    // 更新租客: room_id, check_in_date, rent_start, rent_end, status=NORMAL
    // 更新房间: status=BLUE, tenant_count+1
}

@PostMapping("/check-out/{tenantId}")
public AjaxResult checkOut(@PathVariable Long tenantId) {
    // 更新租客: check_out_date=NOW, status=CHECKED_OUT
    // 更新房间: tenant_count-1, if count==0 → status=GREEN
}

@PutMapping("/renew/{tenantId}")
public AjaxResult renew(@PathVariable Long tenantId, @RequestBody Map<String,Object> body) {
    // 更新 rent_end
}
```

- [ ] `mvn compile -pl ruoyi-admin -am -DskipTests` → BUILD SUCCESS
- [ ] **Commit** `feat: add tenant CRUD and check-in/check-out/renew APIs`

---

### Task 3: 前端页面 + 卡片 Tab 更新

**Files:**
- Create: `ruoyi-ui/src/views/hostel/tenant/index.vue` — 租客 CRUD 表格
- Create: `ruoyi-ui/src/views/hostel/tenant/checkin.vue` — 入住办理页（选租客+选空置房间+填租期）
- Modify: `ruoyi-ui/src/views/hostel/room/card.vue` — "租客管理"Tab 从空壳改为真实数据（调用 room detail API 获取 tenantList）
- Modify: `ruoyi-ui/src/router/index.js` — 添加租客路由

- [ ] `npm run build:prod` → DONE
- [ ] **Commit** `feat: add tenant frontend pages and card tab integration`

---

### Task 4: 构建部署验证

- [ ] `mvn clean package -DskipTests -q` + `npm run build:prod`
- [ ] scp + restart + nginx reload
- [ ] 验证：入住→卡片变蓝→退租→卡片变绿
- [ ] **Commit**: `chore: deploy tenant management feature` + push
