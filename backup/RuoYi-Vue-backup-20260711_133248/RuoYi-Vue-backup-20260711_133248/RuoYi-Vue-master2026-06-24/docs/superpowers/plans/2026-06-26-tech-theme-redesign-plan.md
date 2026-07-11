# 智慧园区科技风主题 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) to implement this plan task-by-task.

**Goal:** 将项目 UI 改为科技风（配色 #00D4FF + 深空蓝侧边栏 + 毛玻璃 + 卡片化），消除若依品牌痕迹

**Architecture:** 纯前端 CSS 改造，不涉及后端。重写 Element UI 变量 + layout 样式 + 登录页 + 首页

**Tech Stack:** Vue 2.6, Element UI 2.15, SCSS, ECharts 5.4

## Global Constraints

- 不改后端代码、路由结构、API 接口、业务页面逻辑
- 编译必须通过：`npm run build:prod`
- 去若依化目标：登录页/侧边栏/顶栏/版权 看不出若依痕迹

---

### Task 1: Element UI 主题变量 + 全局样式重写

**Files:**
- Modify: `ruoyi-ui/src/assets/styles/element-variables.scss`
- Modify: `ruoyi-ui/src/assets/styles/ruoyi.scss`
- Modify: `ruoyi-ui/src/assets/styles/variables.scss`
- Modify: `ruoyi-ui/src/assets/styles/index.scss`
- Modify: `ruoyi-ui/src/assets/styles/sidebar.scss`
- Modify: `ruoyi-ui/src/assets/styles/btn.scss`

- [ ] **Step 1: 重写 element-variables.scss**

```scss
// 科技风主色
$--color-primary: #00D4FF;
$--color-success: #00B894;
$--color-warning: #FDCB6E;
$--color-danger: #E17055;
$--color-info: #636E72;

// 字体
$--font-path: '~element-ui/lib/theme-chalk/fonts';

// 圆角
$--border-radius-base: 8px;
$--border-radius-small: 4px;

// 边框
$--border-color-base: #E8ECF1;
$--border-color-light: #F0F4F8;

// 背景
$--background-color-base: #F0F4F8;

// 菜单
$--menu-text: #A0B4C8;
$--menu-active-text: #00D4FF;
$--menu-bg: #0A1628;
$--menu-hover: rgba(0,212,255,0.1);

@import '~element-ui/packages/theme-chalk/src/index';
```

- [ ] **Step 2: 重写 ruoyi.scss**

```scss
// 全局卡片样式
.el-card { border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }
.el-table { border-radius: 8px; overflow: hidden; }
.el-button--primary { background: #00D4FF; border-color: #00D4FF; }
.el-button--primary:hover { background: #00B8E0; border-color: #00B8E0; }
.el-pagination button:hover, .el-pagination .el-pager li:hover { color: #00D4FF; }

// 表格悬停
.el-table--striped .el-table__body tr.el-table__row--striped td { background: #F8FAFC; }
.el-table tbody tr:hover > td { background: rgba(0,212,255,0.03) !important; }
```

- [ ] **Step 3: 重写 sidebar.scss**

```scss
.sub-sidebar { background: rgba(10,22,40,0.97) !important; backdrop-filter: blur(12px); }
.sub-sidebar .el-menu { background: transparent; border: none; }
.sub-sidebar .el-menu-item { border-radius: 8px; margin: 2px 8px; color: #A0B4C8; }
.sub-sidebar .el-menu-item:hover { background: rgba(0,212,255,0.08); color: #00D4FF; }
.sub-sidebar .el-menu-item.is-active { 
  background: linear-gradient(90deg, rgba(0,212,255,0.15), transparent);
  border-left: 3px solid #00D4FF; color: #00D4FF;
}
```

- [ ] **Step 4**: `npm run build:prod` → 确认不报错
- [ ] **Commit**: `feat: tech theme - element variables, global styles, sidebar`

---

### Task 2: 布局 + Logo + 版权改造

**Files:**
- Modify: `ruoyi-ui/src/layout/index.vue`
- Modify: `ruoyi-ui/src/layout/components/Sidebar/Logo.vue`
- Modify: `ruoyi-ui/src/layout/components/Navbar.vue`
- Modify: `ruoyi-ui/src/layout/components/Copyright/index.vue`
- Modify: `ruoyi-ui/src/settings.js` (关闭 tagsView)
- Modify: `ruoyi-ui/src/App.vue` (移除 theme-picker)

- [ ] **Step 1: Logo 替换**

在 `Logo.vue` 中替换 Logo 内容：
```html
<div class="sidebar-logo">
  <div class="logo-icon">🏢</div>
  <span class="logo-text">智慧园区</span>
</div>
```
CSS: 渐变文字 `background: linear-gradient(90deg, #00D4FF, #00B894); -webkit-background-clip: text;`

- [ ] **Step 2: 关闭标签页**

`settings.js`: `tagsView: false`

- [ ] **Step 3: 移除版权和佛祖**

删除 `banner.txt` 文件。
`Copyright/index.vue` → `© 2026 智慧园区管理系统`

- [ ] **Step 4: 移除 RuoYi 组件引用**

从 `src/main.js` 删除以下全局注册（如存在）：
```javascript
// 删除 RuoYi/Doc, RuoYi/Git 组件的注册
```

`App.vue` 中移除 `<theme-picker />`

- [ ] **Step 5**: `npm run build:prod` → DONE
- [ ] **Commit**: `feat: tech theme - layout, logo, copyright rebranding`

---

### Task 3: 登录页 + 首页改造

**Files:**
- Modify: `ruoyi-ui/src/views/login.vue`
- Modify: `ruoyi-ui/src/views/index.vue`
- Create: `ruoyi-ui/src/views/index_v1.vue` (备份原首页)

- [ ] **Step 1: 登录页改造**

左侧：科技渐变背景 + CSS 粒子动画
```scss
.login-left {
  background: linear-gradient(135deg, #0A1628 0%, #0D2B4E 50%, #00D4FF 100%);
  position: relative; overflow: hidden;
}
.login-right { 
  background: #FFFFFF; border-radius: 12px 0 0 12px;
  box-shadow: -4px 0 20px rgba(0,0,0,0.1);
}
```
移除若依风格标题，改为"智慧园区管理系统"（白色无衬线）。

- [ ] **Step 2: 首页改造**

数据卡片区（4 列）：
```html
<el-row :gutter="16">
  <el-col :span="6">
    <el-card class="stat-card">
      <div class="stat-icon" style="background:#00D4FF">📡</div>
      <div class="stat-info"><h3>{{ deviceTotal }}</h3><p>设备总数</p></div>
    </el-card>
  </el-col>
  <!-- 在线率 / 访客数 / 会议数 同理 -->
</el-row>
```
图表区：设备类型饼图 + 访客趋势线图（复用已有 ECharts 组件）。

- [ ] **Step 3**: `npm run build:prod` → DONE
- [ ] **Commit**: `feat: tech theme - login page and dashboard redesign`

---

### Task 4: 构建部署验证

- [ ] **Step 1**: `npm run build:prod` → DONE
- [ ] **Step 2**: 部署 `scp -r dist/* root@1.94.26.126:/usr/share/nginx/zhihuiyuanqu/dist/`
- [ ] **Step 3**: `ssh root@1.94.26.126 'nginx -s reload'`
- [ ] **Step 4**: 浏览器验证首页/登录页/侧边栏配色
- [ ] **Commit**: `chore: deploy tech theme` + push origin master
