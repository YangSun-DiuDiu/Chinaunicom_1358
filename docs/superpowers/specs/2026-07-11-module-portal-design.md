# 模块导航门户 + 深色科技风改造 — 设计文档

## 概述

移除首页驾驶舱大屏，替换为 10 张模块导航卡片门户。进入模块后，侧边栏只显示当前模块子菜单。同步将侧边栏和首页配色对齐深色科技风。

## 10 模块卡片

| # | 卡片 | menu_id | 路由 | 几何图案 | 配色 |
|---|------|---------|------|---------|------|
| 1 | 系统管理 | 1 | /system | 嵌套方块+齿轮 | #5470c6 蓝紫 |
| 2 | 设备管理 | 2000 | /device/list | 电路板网格 | #00c864 绿 |
| 3 | 智能化管理 | 2009 | /smart | 雷达扫描纹 | #ff9100 橙 |
| 4 | 访客管理 | 2004 | /visitor/appointment | 闸机竖线 | #409EFF 蓝 |
| 5 | 考勤管理 | 2021 | /attendance | 日历方格 | #f0a000 琥珀 |
| 6 | 系统监控 | 2 | /monitor | 心电图波形 | #e8453c 红 |
| 7 | 短信中台 | 2132 | /sms | 波纹扩散 | #1890ff 蓝 |
| 8 | 会议管理 | 2031 | /meeting | 环形会议桌 | #00b482 青绿 |
| 9 | 公寓管理 | 2113 | /hostel | 建筑窗户网格 | #a078c8 紫 |
| 10 | 通知公告 | 107 | /system/notice | 公告板纹 | #fac858 金 |

## 数据库变更

3 条 SQL：
1. `UPDATE sys_menu SET menu_name='系统管理' WHERE menu_id=1`
2. `UPDATE sys_menu SET parent_id=0, order_num=11 WHERE menu_id=107`（通知公告提升为顶级）
3. `UPDATE sys_menu SET parent_id=2 WHERE menu_id=108`（日志管理归入系统监控）

## 前端改动

### 1. views/index.vue — 完全重写
- 删除全部 cockpit 代码（~700 行）
- 替换为 3×4 网格卡片门户
- 深蓝渐变背景 `#0a1628 → #162240 → #0d1f3c`
- 10 张卡片，CSS 几何抽象图案（纯代码，零图片资源）
- 点击卡片跳转对应模块首页
- 0 个 API 调用（纯静态导航）

### 2. router/index.js — 加 parentMenuId 标记
- 10 个模块路由各加 `meta: { parentMenuId: 数据库menu_id }`
- 子路由通过 `parentMenuId` 关联到父模块

### 3. Sidebar/SidebarItem.vue — 加过滤逻辑
- 从 `$route.meta.parentMenuId` 读取当前模块
- 只渲染 parent_id 匹配的子菜单项
- 无 parentMenuId 时显示完整菜单树（兼容）

### 4. Sidebar/index.vue — 样式对齐
- 背景：`#1a2332 → #263445` 渐变
- Logo 区加 10px `#00d4ff` LED 发光圆点
- 激活态：左侧 3px `#00d4ff` 竖线
- 子菜单：4px 实心圆点

### 5. variables.scss — 侧边栏变量
- 菜单背景改为渐变
- 其余 Element UI 变量不变

### 6. Navbar.vue — 返回首页入口
- 除首页外，顶部左侧显示「← 返回首页」

## 不改动的部分

- 所有模块内部页面（设备列表、访客审批等）保持原样
- Element UI 主题变量（仅 sidebar 变量调整）
- 后端代码零改动
- 权限体系不变

## 配色令牌

| 变量 | 值 | 用途 |
|------|-----|------|
| `--bg-shell` | `#0a1628 → #162240 → #0d1f3c` | 首页背景渐变 |
| `--bg-sidebar` | `#1a2332 → #263445` | 侧边栏渐变 |
| `--accent` | `#00d4ff` | 主强调色 |
| `--text-sidebar` | `#bfcbd9` | 侧边栏文字 |
| `--text-sidebar-dim` | `#6b7b8d` | 侧边栏弱化文字 |

## 卡片点击跳转逻辑

点击卡片 → `this.$router.push(模块path)` → 侧边栏检测 `$route.meta.parentMenuId` → 只渲染该模块子菜单。

"← 返回首页" → `this.$router.push('/index')` → 侧边栏恢复完整菜单（或空菜单）。
