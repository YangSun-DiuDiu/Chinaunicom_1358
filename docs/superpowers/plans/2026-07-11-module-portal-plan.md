# 模块导航门户 + 深色科技风改造 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除驾驶舱大屏，替换为 10 张 CSS 几何抽象模块导航卡片门户，侧边栏按模块过滤，整体对齐深色科技风。

**Architecture:** 首页纯静态 10 卡 Vue 组件（零 API 依赖），路由 meta 标记 `parentMenuId` 驱动侧边栏过滤，侧边栏样式从 v7 科技风升级到 v8 深色渐变。

**Tech Stack:** Vue2 + Element UI + SCSS + ECharts（已移除）

## Global Constraints

- 不引入新 npm 依赖
- 不修改任何后端 Java 代码
- 模块内部页面（表格/表单/弹窗）不改深色，只改侧边栏和首页
- 所有卡片图案为 CSS 纯代码，不使用图片资源
- 路由 history 模式，路径前缀 `/`

---

### Task 1: 数据库菜单拆分（3 条 SQL）

**Files:**
- 无代码文件修改，直接执行 SQL

- [ ] **Step 1: 备份 sys_menu 表**

```bash
ssh root@1.94.26.126 "mysqldump -uroot -p'Chinaunicom@1358' ry101 sys_menu > /root/sys_menu_backup_$(date +%Y%m%d_%H%M%S).sql"
```

- [ ] **Step 2: 执行菜单拆分 SQL**

```sql
-- 1. 重命名"人员管理"为"系统管理"
UPDATE sys_menu SET menu_name = '系统管理' WHERE menu_id = 1;

-- 2. 通知公告提升为顶级目录
UPDATE sys_menu SET parent_id = 0, order_num = 11 WHERE menu_id = 107;

-- 3. 日志管理归入系统监控
UPDATE sys_menu SET parent_id = 2 WHERE menu_id = 108;
-- 子菜单 500(操作日志) 和 501(登录日志) 自动跟随
```

- [ ] **Step 3: 验证菜单结构**

```sql
SELECT m1.menu_id, m1.menu_name, m1.parent_id, m1.order_num
FROM sys_menu m1
WHERE m1.parent_id = 0 AND m1.menu_type = 'M' AND m1.visible = '0'
ORDER BY m1.order_num;
```

预期输出 10 行：系统管理、设备管理、智能化管理、访客管理、考勤管理、系统监控、短信中台、会议管理、公寓管理、通知公告。

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "feat: 数据库菜单拆分 — 人员管理→系统管理 + 通知公告提升为顶级"
```

---

### Task 2: 侧边栏 SCSS 变量 — 深色渐变背景

**Files:**
- Modify: `ruoyi-ui/src/assets/styles/variables.scss`

- [ ] **Step 1: 修改菜单背景变量**

在 `variables.scss` 中，找到 `$menuBackground`（约第 68 行），替换为：

```scss
// 侧边栏 — 深色科技风渐变（v8 module-portal）
$menuBackground: linear-gradient(180deg, #1a2332 0%, #263445 100%);
$menuColor: #bfcbd9;
```

注意：`$menuBackground` 原本是纯色 `#304156`，现改为渐变。若 Element UI 的 `el-menu` 不支持渐变 background-color，改为固定取渐变的起始色：

```scss
$menuBackground: #1a2332;
$menuColor: #bfcbd9;
```

同时添加：
```scss
// 侧边栏激活态竖线颜色
$menuActiveBorder: #00d4ff;
```

- [ ] **Step 2: Commit**

```bash
git add ruoyi-ui/src/assets/styles/variables.scss
git commit -m "feat: 侧边栏深色渐变背景变量"
```

---

### Task 3: 首页重写 — 10 卡模块导航门户

**Files:**
- Modify: `ruoyi-ui/src/views/index.vue`（完全重写，~350 行）

**Interfaces:**
- Produces: 10 张模块卡片，点击跳转 `$router.push(modulePath)`
- Consumes: 无 API 依赖，纯静态导航

- [ ] **Step 1: 写入新 index.vue**

完整替换 `ruoyi-ui/src/views/index.vue`：

```vue
<template>
  <div class="portal">
    <div class="portal-bg"></div>
    <div class="portal-header">
      <h2>SMART PARK</h2>
      <p>智慧园区 · 综合管理平台</p>
    </div>
    <div class="portal-grid">
      <div
        v-for="card in cards"
        :key="card.id"
        class="portal-card"
        @click="enterModule(card.path)"
      >
        <div :class="['card-pattern', card.pattern]"></div>
        <div class="card-title">{{ card.name }}</div>
        <div class="card-sub">{{ card.sub }}</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PortalIndex',
  data() {
    return {
      cards: [
        { id: 1,    name: '系统管理',   path: '/system/user',         pattern: 'p-admin',      sub: '菜单 · 角色 · 用户 · 部门 · 岗位 · 参数' },
        { id: 2000, name: '设备管理',   path: '/device/list',         pattern: 'p-device',     sub: 'IoT设备 · 拓扑 · 心跳 · 故障 · SNMP · 维修' },
        { id: 2009, name: '智能化管理', path: '/smart/person-access-device', pattern: 'p-smart', sub: '门禁 · 车行 · 视频监控' },
        { id: 2004, name: '访客管理',   path: '/visitor/appointment', pattern: 'p-visitor',    sub: '预约 · 审批 · 登记 · 记录' },
        { id: 2021, name: '考勤管理',   path: '/attendance/import',   pattern: 'p-attendance', sub: '数据对接 · 考勤统计' },
        { id: 2,    name: '系统监控',   path: '/monitor/operlog',     pattern: 'p-monitor',    sub: '操作日志 · 登录日志 · 数据监控 · 缓存' },
        { id: 2132, name: '短信中台',   path: '/sms/channel',         pattern: 'p-sms',        sub: '渠道 · 模板 · 业务 · 黑名单 · 日志' },
        { id: 2031, name: '会议管理',   path: '/meeting/room',        pattern: 'p-meeting',    sub: '会议室 · 预约 · 管理 · 审批' },
        { id: 2113, name: '公寓管理',   path: '/hostel/apartment',    pattern: 'p-hostel',     sub: '房源 · 房间 · 卡片 · 租客' },
        { id: 2100, name: '通知公告',   path: '/system/notice',       pattern: 'p-notice',     sub: '公告发布 · 消息推送' },
      ]
    }
  },
  methods: {
    enterModule(path) {
      this.$router.push(path)
    }
  }
}
</script>

<style scoped>
/* ====== 门户容器 ====== */
.portal {
  position: relative; margin: -20px;
  min-height: calc(100vh - 84px);
  background: linear-gradient(135deg, #0a1628 0%, #162240 50%, #0d1f3c 100%);
  padding: 40px 36px; overflow: auto;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
}

/* 暗纹网格背景 */
.portal-bg {
  position: absolute; inset: 0; pointer-events: none; z-index: 0;
  background-image:
    linear-gradient(rgba(0,212,255,0.025) 1px, transparent 1px),
    linear-gradient(90deg, rgba(0,212,255,0.025) 1px, transparent 1px);
  background-size: 30px 30px;
}

.portal-header { position:relative; z-index:1; text-align:center; margin-bottom:32px; }
.portal-header h2 { font-size:24px; font-weight:300; color:rgba(255,255,255,0.5); letter-spacing:6px; margin:0 0 6px; }
.portal-header p { font-size:13px; color:rgba(255,255,255,0.3); letter-spacing:3px; margin:0; }

/* ====== 3列卡片网格 ====== */
.portal-grid {
  position:relative; z-index:1;
  display:grid;
  grid-template-columns:repeat(3, 1fr);
  gap:18px; max-width:1050px; margin:0 auto;
}

/* ====== 卡片 ====== */
.portal-card {
  position:relative; height:170px; border-radius:12px;
  cursor:pointer; overflow:hidden;
  display:flex; flex-direction:column; justify-content:flex-end; padding:20px;
  border:1px solid rgba(255,255,255,0.06);
  background:rgba(20,28,40,0.6);
  transition:all 0.35s cubic-bezier(0.4,0,0.2,1);
}
.portal-card:hover { transform:translateY(-4px); border-color:rgba(0,212,255,0.3); }
.portal-card:hover .card-pattern { opacity:1; }

.card-title {
  position:relative; z-index:1; font-size:17px; font-weight:600; color:#fff;
  letter-spacing:1px; margin-bottom:4px;
}
.card-sub {
  position:relative; z-index:1; font-size:11px; color:rgba(255,255,255,0.45);
  letter-spacing:0.5px;
}

/* ====== 10个几何抽象图案（纯CSS，零图片） ====== */
.card-pattern { position:absolute; inset:0; opacity:0.5; transition:opacity 0.35s; }

/* 系统管理：嵌套方块 */
.p-admin {
  background:
    repeating-conic-gradient(rgba(84,112,198,0.06) 0% 2%, transparent 2% 8%) 50% 30% / 80px 80px,
    linear-gradient(135deg, rgba(84,112,198,0.1) 0%, rgba(40,60,130,0.03) 100%);
}
.p-admin::before {
  content:''; position:absolute; top:20%; left:30%; right:50%; bottom:30%;
  border:1.5px solid rgba(84,112,198,0.15); border-radius:3px;
  box-shadow:8px 8px 0 rgba(84,112,198,0.08), 16px 16px 0 rgba(84,112,198,0.04);
}

/* 设备管理：电路板网格 */
.p-device {
  background:
    linear-gradient(0deg, transparent 48%, rgba(0,200,100,0.08) 48%, rgba(0,200,100,0.08) 49%, transparent 49%),
    linear-gradient(90deg, transparent 48%, rgba(0,200,100,0.1) 48%, rgba(0,200,100,0.1) 49%, transparent 49%),
    linear-gradient(135deg, rgba(0,200,100,0.12) 0%, rgba(0,160,80,0.04) 100%);
  background-size:30px 30px, 60px 60px, 100% 100%;
}
.p-device::before {
  content:''; position:absolute; top:15%; left:55%; right:25%; bottom:20%;
  border:1px solid rgba(0,200,100,0.08); border-radius:4px;
}
.p-device::after {
  content:''; position:absolute; top:35%; left:30%;
  width:50px; height:1px; background:rgba(0,200,100,0.2);
  box-shadow:0 25px 0 rgba(0,200,100,0.1), 0 50px 0 rgba(0,200,100,0.06);
}

/* 智能化管理：雷达扫描 */
.p-smart {
  background:
    conic-gradient(from 0deg, rgba(255,145,0,0.12) 0deg, transparent 30deg),
    radial-gradient(circle at 70% 30%, rgba(255,145,0,0.08) 0%, transparent 50%),
    linear-gradient(135deg, rgba(255,145,0,0.08) 0%, rgba(180,100,0,0.02) 100%);
}
.p-smart::after {
  content:''; position:absolute; top:20%; right:15%;
  width:60px; height:60px; border-radius:50%;
  border:1px solid rgba(255,145,0,0.2);
  box-shadow:0 0 20px rgba(255,145,0,0.05);
}

/* 访客管理：闸机竖线 */
.p-visitor {
  background:
    linear-gradient(125deg, transparent 60%, rgba(64,158,255,0.08) 60%, rgba(64,158,255,0.08) 62%, transparent 62%),
    linear-gradient(135deg, rgba(64,158,255,0.1) 0%, rgba(30,100,200,0.04) 100%);
}
.p-visitor::before {
  content:''; position:absolute; top:25%; bottom:25%; right:25%;
  width:3px; background:rgba(64,158,255,0.15);
  box-shadow:-15px 0 0 rgba(64,158,255,0.1), -30px 0 0 rgba(64,158,255,0.06), -45px 0 0 rgba(64,158,255,0.03);
}

/* 考勤管理：日历方格 */
.p-attendance {
  background:
    repeating-linear-gradient(0deg, transparent, transparent 13px, rgba(240,160,0,0.04) 13px, rgba(240,160,0,0.04) 14px),
    repeating-linear-gradient(90deg, transparent, transparent 40px, rgba(240,160,0,0.04) 40px, rgba(240,160,0,0.04) 41px),
    linear-gradient(135deg, rgba(240,160,0,0.07) 0%, rgba(180,120,0,0.02) 100%);
}

/* 系统监控：心电图波形 */
.p-monitor {
  background:linear-gradient(135deg, rgba(232,69,60,0.06) 0%, rgba(30,30,40,0) 100%);
}
.p-monitor::after {
  content:''; position:absolute; top:20%; left:15%; right:15%; bottom:60%;
  border-bottom:1px solid rgba(232,69,60,0.15);
  clip-path:polygon(
    0% 100%, 8% 80%, 12% 100%, 16% 30%, 20% 90%, 25% 100%,
    30% 75%, 35% 100%, 40% 20%, 45% 90%, 50% 100%,
    55% 85%, 60% 100%, 70% 40%, 75% 90%, 80% 100%,
    85% 75%, 90% 100%, 95% 85%, 100% 100%
  );
}

/* 短信中台：波纹扩散 */
.p-sms {
  background:
    radial-gradient(circle at 65% 28%, rgba(24,144,255,0.15) 0%, transparent 10%),
    radial-gradient(circle at 65% 28%, rgba(24,144,255,0.08) 10%, transparent 20%),
    radial-gradient(circle at 65% 28%, rgba(24,144,255,0.04) 20%, transparent 30%),
    linear-gradient(135deg, rgba(24,144,255,0.08) 0%, rgba(10,60,130,0.02) 100%);
}

/* 会议管理：环形会议桌 */
.p-meeting {
  background:
    radial-gradient(ellipse 55% 60% at 55% 40%, transparent 30%, rgba(0,180,130,0.06) 30%, rgba(0,180,130,0.06) 32%, transparent 32%),
    radial-gradient(ellipse 40% 45% at 55% 40%, transparent 28%, rgba(0,180,130,0.04) 28%, rgba(0,180,130,0.04) 30%, transparent 30%),
    linear-gradient(135deg, rgba(0,180,130,0.07) 0%, rgba(0,100,80,0.02) 100%);
}

/* 公寓管理：建筑窗户网格 */
.p-hostel {
  background:
    repeating-linear-gradient(0deg, transparent, transparent 14px, rgba(160,120,200,0.04) 14px, rgba(160,120,200,0.04) 15px),
    repeating-linear-gradient(90deg, transparent, transparent 28px, rgba(160,120,200,0.04) 28px, rgba(160,120,200,0.04) 29px),
    linear-gradient(135deg, rgba(160,120,200,0.07) 0%, rgba(80,40,140,0.02) 100%);
}
.p-hostel::before {
  content:''; position:absolute; top:15%; left:15%; right:55%; bottom:30%;
  border:1px solid rgba(160,120,200,0.1);
  box-shadow:40px 0 0 rgba(160,120,200,0.05), inset 0 20px 0 rgba(160,120,200,0.04);
}

/* 通知公告：公告板 */
.p-notice {
  background:
    repeating-linear-gradient(0deg, transparent, transparent 18px, rgba(250,200,88,0.03) 18px, rgba(250,200,88,0.03) 19px),
    linear-gradient(135deg, rgba(250,200,88,0.08) 0%, rgba(180,140,40,0.02) 100%);
}
.p-notice::before {
  content:''; position:absolute; top:18%; left:50%; right:25%; bottom:35%;
  border:1px solid rgba(250,200,88,0.1); border-radius:3px;
  background:linear-gradient(180deg, rgba(250,200,88,0.06) 0%, transparent 100%);
}

/* ====== 响应式 ====== */
@media (max-width:900px) { .portal-grid { grid-template-columns:repeat(2, 1fr); } }
@media (max-width:600px) { .portal-grid { grid-template-columns:1fr; } .portal { padding:20px 14px; } }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add ruoyi-ui/src/views/index.vue
git commit -m "feat: 首页重写 — 10卡CSS几何抽象模块导航门户，移除驾驶舱"
```

---

### Task 4: 路由标记 — 加 parentMenuId + 菜单标签修正

**Files:**
- Modify: `ruoyi-ui/src/router/index.js`

**Interfaces:**
- Produces: 每个模块顶层路由加 `meta.parentMenuId`，子路由通过此 ID 关联父模块
- Consumes: 无

- [ ] **Step 1: 修改首页路由，去 dashboard icon**

找到 `constantRoutes` 中路径 `/` 的路由（第 43-53 行），将 `icon: "dashboard"` 改为 `icon: "home"`：

```js
{
  path: "/",
  component: Layout,
  redirect: "index",
  children: [
    {
      path: "index",
      component: () => import("@/views/index"),
      name: "Index",
      meta: { title: "首页", icon: "home", affix: true },
    },
  ],
},
```

- [ ] **Step 2: 修正"人员管理"→"系统管理"，加 parentMenuId**

找到 `staticRoutes` 中 path `/system` 的"人员管理"路由（第 148-186 行），改名并加标记：

```js
{
  path: "/system",
  component: Layout,
  name: "System",
  alwaysShow: true,
  meta: { title: "系统管理", icon: "system", parentMenuId: 1 },
  children: [ /* 保持原样：user, dept, role */ ],
},
```

- [ ] **Step 3: 给其余 9 个模块路由加 parentMenuId**

在 `staticRoutes` 中，给以下路由的 `meta` 各加一行 `parentMenuId: N`：

| 模块 | meta 中新增 |
|------|------------|
| `/device` (第 188 行) | `parentMenuId: 2000` |
| `/visitor` (第 274 行) | `parentMenuId: 2004` |
| `/smart` (第 400 行) | `parentMenuId: 2009` |
| `/attendance` (第 482 行) | `parentMenuId: 2021` |
| `/sms` (第 512 行) | `parentMenuId: 2132` |
| `/meeting` (第 582 行) | `parentMenuId: 2031` |
| `/hostel` (第 632 行) | `parentMenuId: 2113` |

`/log` 和 `/settings` 路由也需要处理：
- `/log` (第 325 行)：`meta: { title: "日志管理", parentMenuId: 2 }`（归入系统监控）
- `/settings` (第 354 行) 保持不变或移除（菜单管理已归入系统管理）

- [ ] **Step 4: Commit**

```bash
git add ruoyi-ui/src/router/index.js
git commit -m "feat: 路由加parentMenuId标记 + 菜单标签修正"
```

---

### Task 5: 侧边栏过滤逻辑 — SidebarItem.vue

**Files:**
- Modify: `ruoyi-ui/src/layout/components/Sidebar/SidebarItem.vue`
- Modify: `ruoyi-ui/src/layout/components/Sidebar/index.vue`

**Interfaces:**
- Consumes: `$route.meta.parentMenuId`（来自路由配置）
- Produces: 侧边栏只渲染匹配的子菜单

- [ ] **Step 1: 修改 SidebarItem.vue — 加 parentMenuId 过滤**

在 `SidebarItem.vue` 的 script 中，新增 computed 属性 `moduleFilterId`，然后修改 `hasOneShowingChild` 方法：

```js
// 在 export default 的 computed 中新增
computed: {
  moduleFilterId() {
    // 从当前路由读取 parentMenuId，用于过滤侧边栏
    return this.$route.meta && this.$route.meta.parentMenuId
  }
},
```

然后在 `hasOneShowingChild` 方法中（`item.hidden` 检查之后），添加过滤逻辑：

```js
hasOneShowingChild(children = [], parent) {
  if (!children) { children = [] }
  const showingChildren = children.filter(item => {
    if (item.hidden) { return false }
    // Module portal filter: only show items matching parentMenuId
    const filterId = this.moduleFilterId
    if (filterId && item.meta && item.meta.parentMenuId &&
        item.meta.parentMenuId !== filterId) {
      return false
    }
    this.onlyOneChild = item
    return true
  })
  // ... rest unchanged
},
```

注意：`moduleFilterId` 是路由级别的，而菜单项过滤需要逐项匹配。实际上，过滤应该作用于顶层路由列表（在 `Sidebar/index.vue` 中更好），而不是递归的 SidebarItem。

**修正方案**：在 `Sidebar/index.vue` 中过滤 `sidebarRouters`，而不是在 SidebarItem 中。

找到 `Sidebar/index.vue` 的 `sidebar-item` 循环（第 15-20 行），前面加过滤：

```js
// 在 computed 中新增
filteredRouters() {
  const parentId = this.$route.meta && this.$route.meta.parentMenuId
  if (!parentId) {
    // 首页或无标记：显示完整菜单
    return this.sidebarRouters
  }
  return this.sidebarRouters.filter(route => {
    // 保留首页 + 匹配的模块 + hidden 路由
    if (route.path === '/' || route.hidden) return true
    // 直接匹配 parentMenuId
    if (route.meta && route.meta.parentMenuId === parentId) return true
    // 子路由中有匹配的
    if (route.children) {
      return route.children.some(c => c.meta && c.meta.parentMenuId === parentId)
    }
    return false
  })
}
```

然后在 template 中把 `sidebarRouters` 替换为 `filteredRouters`：

```html
<sidebar-item
    v-for="(route, index) in filteredRouters"
    :key="route.path + index"
    :item="route"
    :base-path="route.path"
/>
```

- [ ] **Step 2: Commit**

```bash
git add ruoyi-ui/src/layout/components/Sidebar/
git commit -m "feat: 侧边栏parentMenuId过滤 + 返回首页入口"
```

---

### Task 6: 侧边栏样式对齐 — Logo + 激活态

**Files:**
- Modify: `ruoyi-ui/src/layout/components/Sidebar/Logo.vue`
- Modify: `ruoyi-ui/src/layout/components/Sidebar/index.vue`（仅样式部分）

- [ ] **Step 1: Logo.vue — 加 LED 信号灯**

在 Logo.vue 的 template 中，logo 文字前加一个发光圆点：

```html
<template>
  <div class="sidebar-logo-container" :class="{'collapse':collapse}">
    <transition name="sidebarLogoFade">
      <router-link v-if="showTitle" key="expand" class="sidebar-logo-link" to="/">
        <span class="sidebar-logo-dot"></span>
        <h1 class="sidebar-title">{{ title }}</h1>
      </router-link>
      <router-link v-else key="collapse" class="sidebar-logo-link" to="/">
        <span class="sidebar-logo-dot"></span>
      </router-link>
    </transition>
  </div>
</template>
```

在 style 中加 dot 样式：

```scss
.sidebar-logo-dot {
  display: inline-block;
  width: 10px; height: 10px;
  border-radius: 50%;
  background: #00d4ff;
  box-shadow: 0 0 10px rgba(0,212,255,0.5);
  margin-right: 8px;
  flex-shrink: 0;
}
```

- [ ] **Step 2: Sidebar/index.vue — 激活态竖线**

在 `<style>` 中添加（或在 `sidebar.scss` 中）：

```scss
// 激活菜单项左侧竖线（深色科技风）
.el-menu-item.is-active {
  border-left: 3px solid #00d4ff !important;
}
// 子菜单项前圆点
.el-menu-item .sub-dot {
  display: inline-block;
  width: 4px; height: 4px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.6;
  margin-right: 8px;
}
```

- [ ] **Step 3: Commit**

```bash
git add ruoyi-ui/src/layout/components/Sidebar/Logo.vue ruoyi-ui/src/layout/components/Sidebar/index.vue
git commit -m "feat: 侧边栏LED信号灯+激活态竖线+子菜单圆点"
```

---

### Task 7: Navbar — 返回首页入口

**Files:**
- Modify: `ruoyi-ui/src/layout/components/Navbar.vue`

- [ ] **Step 1: 在面包屑左边加"返回首页"链接**

在模板中面包屑组件前面插入：

```html
<router-link v-if="showBackHome" to="/index" class="back-home-link">
  ← 返回首页
</router-link>
```

在 computed 中：

```js
showBackHome() {
  const parentId = this.$route.meta && this.$route.meta.parentMenuId
  return !!parentId
}
```

在 style 中：

```scss
.back-home-link {
  color: #00d4ff;
  font-size: 13px;
  margin-right: 16px;
  text-decoration: none;
  &:hover { opacity: 0.8; }
}
```

- [ ] **Step 2: Commit**

```bash
git add ruoyi-ui/src/layout/components/Navbar.vue
git commit -m "feat: Navbar加返回首页入口（模块内可见）"
```

---

### Task 8: 编译部署 + 浏览器验证

- [ ] **Step 1: 编译前端**

```bash
cd ruoyi-ui && npm run build:prod
```

- [ ] **Step 2: 清理服务器旧 dist**

```bash
ssh root@1.94.26.126 "rm -rf /usr/share/nginx/zhihuiyuanqu/dist/* /root/zhihuiyuanqu20260625/dist/*"
```

- [ ] **Step 3: 上传并重载 Nginx**

```bash
scp -r ruoyi-ui/dist/* root@1.94.26.126:/root/zhihuiyuanqu20260625/dist/
ssh root@1.94.26.126 "cp -r /root/zhihuiyuanqu20260625/dist/* /usr/share/nginx/zhihuiyuanqu/dist/ && nginx -s reload"
```

- [ ] **Step 4: 浏览器验证**

打开 `http://1.94.26.126/`，验证：
1. 首页显示 10 张模块卡片，3 列排列 ✅
2. 点击"系统管理"卡片 → 跳转 /system/user，侧边栏只显示系统管理子菜单 ✅
3. 顶部 Navbar 显示「← 返回首页」 ✅
4. 点击返回首页 → 回到 10 卡门户 ✅
5. 侧边栏为深色渐变背景，Logo 有 LED 发光圆点 ✅
6. 无 JS 错误 ✅

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "chore: 编译部署验证通过 — 模块导航门户上线"
```

---

### Task 9: 全量权限审计测试 — 前端API × 后端Controller × DB权限 三方对照

**Files:**
- 扫描：`ruoyi-ui/src/api/**/*.js`（所有前端 API 模块）
- 扫描：`ruoyi-admin/src/main/java/com/ruoyi/web/controller/**/*.java`（所有后端 Controller）
- 查询：`ry101.sys_menu`（权限菜单表）+ `ry101.sys_role_menu`（角色权限关联表）
- 产出：`docs/permission-audit-20260711.md`

- [ ] **Step 1: 扫描前端所有 API 调用**

遍历 `ruoyi-ui/src/api/` 下所有 `.js` 文件，提取每个 API 函数对应的 URL 和 HTTP 方法。

```bash
grep -rn "url:" ruoyi-ui/src/api/ --include="*.js" | grep -v "node_modules" | sort
```

将输出整理为清单，格式：`模块 | 函数名 | 方法 | URL路径`

- [ ] **Step 2: 扫描后端所有 Controller @PreAuthorize 注解**

遍历所有 Controller Java 文件，提取每个接口的权限注解。

```bash
grep -rn "@PreAuthorize\|@Anonymous\|@GetMapping\|@PostMapping\|@PutMapping\|@DeleteMapping\|@RequestMapping" ruoyi-admin/src/main/java/com/ruoyi/web/controller/ --include="*.java" | grep -v "import"
```

对每个接口记录：`Controller类 | 方法 | HTTP方法 | URL | @PreAuthorize权限 | 是否公开`

- [ ] **Step 3: 查询数据库权限菜单**

```sql
SELECT m.menu_id, m.menu_name, m.perms, m.url, m.visible,
  CASE m.menu_type WHEN 'M' THEN '目录' WHEN 'C' THEN '菜单' WHEN 'F' THEN '按钮' END AS 类型
FROM sys_menu m
WHERE m.perms IS NOT NULL AND m.perms != ''
ORDER BY m.menu_type, m.order_num;
```

- [ ] **Step 4: 创建专用测试账号**

```sql
-- 创建测试角色
INSERT INTO sys_role (role_name, role_key, role_sort, status, create_by, create_time, remark)
VALUES ('权限审计测试', 'audit_test', 99, '0', 'admin', NOW(), '权限审计专用测试角色');

-- 获取新角色ID (假设为 999)
SET @role_id = LAST_INSERT_ID();

-- 分配权限：根据 sys_menu 中所有 perms 不为空的按钮(F)和菜单(C)权限赋给该角色
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT @role_id, menu_id FROM sys_menu WHERE perms IS NOT NULL AND perms != '';

-- 创建测试用户
INSERT INTO sys_user (user_name, nick_name, password, dept_id, status, create_by, create_time, remark)
VALUES ('audit_tester', '权限审计员', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', 103, '0', 'admin', NOW(), '权限审计专用测试账号');
-- 密码: admin123

-- 分配角色给用户
INSERT INTO sys_user_role (user_id, role_id)
SELECT user_id, @role_id FROM sys_user WHERE user_name = 'audit_tester';
```

- [ ] **Step 5: 生成两张权限对照表**

产出文件：`docs/permission-audit-20260711.md`

**表格 A — 公开接口（无需认证）**

从 SecurityConfig.permitAll() 和白名单路由提取：

| # | 路径 | 方法 | 说明 |
|---|------|------|------|
| 1 | `/login` | POST | 登录 |
| 2 | `/captchaImage` | GET | 验证码 |
| ... | ... | ... | ... |

**表格 B — 受保护接口（需要认证+权限）**

三方对照格式：

| # | 前端API函数 | 前端URL | HTTP | 后端Controller.方法 | @PreAuthorize权限 | DB菜单权限 | 状态 |
|---|-----------|---------|------|-------------------|-------------------|-----------|------|
| 1 | getDeviceList | `/device/list` | GET | DeviceController.list() | `@ss.hasPermi('device:list')` | `device:list` | ✅ 一致 |
| ... | ... | ... | ... | ... | ... | ... | ... |

状态列判断标准：
- ✅ 一致：前端 URL → 后端方法 → @PreAuthorize 权限 → DB sys_menu.perms 三方完全匹配
- ⚠ 缺失注解：后端方法无 @PreAuthorize 或 @Anonymous
- ❌ 权限不一致：前端 URL 对应的后端权限与 DB 记录不匹配
- 🔴 后端缺失：前端 API 在后端找不到对应 Controller 方法

- [ ] **Step 6: 用测试账号逐模块验证**

使用 Playwright 浏览器：
1. 用 `audit_tester / admin123` 登录
2. 遍历首页 10 张卡片，逐一进入每个模块
3. 在每个模块中操作所有可见的页面和按钮
4. 检查浏览器 Console 是否有 401/403 错误
5. 检查 Network 面板是否有失败的 API 请求

```javascript
// Playwright 验证脚本（伪代码）
const modules = ['/system/user', '/device/list', '/smart/person-access-device', ...]
for (const path of modules) {
  await page.goto(`http://1.94.26.126${path}`)
  // 检查无 403 错误
  const errors = await page.evaluate(() => {
    return performance.getEntriesByType('resource')
      .filter(r => r.name.includes('/prod-api/') && (r.responseStatus === 403 || r.responseStatus === 401))
  })
  console.log(`${path}: ${errors.length === 0 ? '✅' : '❌ ' + errors.length + ' errors'}`)
}
```

- [ ] **Step 7: 汇总审计报告**

在 `docs/permission-audit-20260711.md` 末尾追加：

- 总计接口数：XX
- ✅ 一致：XX
- ⚠ 缺失注解：XX（列出具体方法和建议）
- ❌ 权限不一致：XX（列出具体差异）
- 🔴 后端缺失：XX（列出前端调了但后端不存在的方法）

- [ ] **Step 8: 修复发现的问题**

根据审计报告，逐个修复：
1. 后端缺 @PreAuthorize → 添加注解
2. DB 缺权限 → INSERT INTO sys_menu
3. 前后端路径不一致 → 统一路径

- [ ] **Step 9: Commit**

```bash
git add docs/permission-audit-20260711.md
git commit -m "test: 全量权限审计 — 前端API×后端Controller×DB权限 三方对照"
```

