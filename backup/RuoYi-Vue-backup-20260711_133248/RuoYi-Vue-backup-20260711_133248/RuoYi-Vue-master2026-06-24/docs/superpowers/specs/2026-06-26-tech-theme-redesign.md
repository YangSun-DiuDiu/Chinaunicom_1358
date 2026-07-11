# 智慧园区科技风主题 — 设计方案

> **日期**: 2026-06-26 | **状态**: 已审核 | **方案**: A 科技风

---

## 1. 配色方案

| 用途 | 色值 | 说明 |
|------|------|------|
| 主色 | `#00D4FF` | 科技蓝，按钮/链接/高亮 |
| 主色深 | `#0A1628` | 深空蓝，侧边栏背景 |
| 背景 | `#F0F4F8` | 浅灰蓝，内容区背景 |
| 卡片 | `#FFFFFF` | 纯白，卡片/表格底 |
| 成功 | `#00B894` | 翡翠绿 |
| 警告 | `#FDCB6E` | 暖黄 |
| 危险 | `#E17055` | 珊瑚红 |
| 文字主 | `#2D3436` | 深灰 |
| 文字辅 | `#636E72` | 中灰 |

## 2. 改造范围

| 模块 | 文件 | 改动 |
|------|------|------|
| Element 主题 | `ruoyi-ui/src/assets/styles/element-variables.scss` | 重写所有 CSS 变量 |
| 全局样式 | `ruoyi-ui/src/assets/styles/ruoyi.scss` | 重写侧边栏/顶栏/标签样式 |
| 布局 | `ruoyi-ui/src/layout/` | 侧边栏深色毛玻璃 + Logo 替换 + 顶栏精简 |
| 首页 | `ruoyi-ui/src/views/index.vue` | 仪表盘风格首页 |
| 登录页 | `ruoyi-ui/src/views/login.vue` | 科技风登录页 |
| 版权 | `ruoyi-ui/src/layout/components/Copyright/` | 移除佛祖 Banner，改智慧园区标识 |
| 品牌 | `ruoyi-ui/src/assets/logo/` | 替换 Logo |
| 路由 | `ruoyi-ui/src/router/index.js` | 无改动 |
| 所有页面 | 自动继承新主题 | Element UI CSS 变量覆盖 |

## 3. 具体改动

### 3.1 Element 主题变量

```scss
// 主色
--color-primary: #00D4FF;
--color-primary-light-3: #33DDFF;
--color-primary-light-5: #66E5FF;
--color-primary-dark-2: #00AACC;

// 侧边栏
--menu-bg: #0A1628;
--menu-hover: rgba(0,212,255,0.1);
--menu-active: rgba(0,212,255,0.2);
--menu-text: #A0B4C8;
--menu-active-text: #00D4FF;

// 圆角
--border-radius-base: 8px;
--border-radius-small: 4px;
```

### 3.2 侧边栏毛玻璃效果

- 背景：`rgba(10,22,40,0.95)` + `backdrop-filter: blur(12px)`
- Logo 区域：渐变 `#0A1628 → #00D4FF` 细条装饰
- 菜单项：圆角 8px、选中时左边框 3px `#00D4FF` 发光

### 3.3 顶栏

- 去标签页（tagsView 关闭）
- 面包屑保持
- 右侧用户区显示组织切换器 + 头像

### 3.4 首页

- 上排 4 张数据卡片（设备总数/在线率/访客数/会议数）
- 下排图表区域（ECharts 设备类型饼图 + 访客趋势线图）

### 3.5 登录页

- 左侧科技抽象图（CSS 渐变 + 粒子动画）
- 右侧白色卡片登录框，圆角 12px，阴影

### 3.6 版权/Banner

- 移除 `banner.txt`（佛祖）
- Copyright 改为"© 2026 智慧园区管理系统"
- 移除 `RuoYi/Git` 和 `RuoYi/Doc` 组件

## 4. 不做的事

- 不修改后端代码
- 不修改路由结构
- 不修改 API 接口
- 不修改业务页面逻辑

## 5. 验证标准

- [ ] 侧边栏深色科技蓝 + 毛玻璃
- [ ] 主按钮/链接为 `#00D4FF`
- [ ] 卡片圆角 8px
- [ ] 佛祖 Banner 已移除
- [ ] 登录页无若依品牌痕迹
- [ ] 首页仪表盘风格
- [ ] `npm run build:prod` 通过
