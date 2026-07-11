# 08 — ruoyi-ui 前端模块

## 模块定位

基于 **Vue 2.6** + **Element UI 2.15** 构建的智慧园区管理系统前端 SPA。采用全静态路由 + 权限过滤模式，抛弃了原若依的动态路由机制。

**核心依赖**: Vue 2.6.12, Element UI 2.15.14, Vue Router 3.4.9, Vuex 3.6.0, Axios 0.30.3, ECharts 5.4.0

---

## 1. 目录结构

```
ruoyi-ui/
 ├── package.json          → 项目依赖与脚本
 ├── vue.config.js         → Webpack: 代理、别名、代码分割、Gzip
 ├── .env.development      → VUE_APP_BASE_API=/dev-api (代理到 :8080)
 ├── .env.production       → VUE_APP_BASE_API=/prod-api
 ├── public/               → index.html, favicon.ico
 └── src/
     ├── main.js           → Vue 初始化 + 全局注册
     ├── App.vue           → 根组件
     ├── permission.js     → 路由守卫（Token检查、权限加载）
     ├── settings.js       → 布局默认配置
     ├── api/              → 17 个 API 模块（Axios 封装）
     ├── assets/           → 样式、80+ SVG 图标、图片
     ├── components/       → 25 个共享组件
     ├── directive/        → 6 个自定义指令
     ├── layout/           → 布局框架（侧边栏/顶部栏/标签页）
     ├── plugins/          → 5 个 Vue 插件
     ├── router/           → 静态路由配置（1 文件）
     ├── store/            → Vuex 状态管理（7 模块）
     ├── utils/            → 20 个工具模块
     └── views/            → 46+ 页面组件
```

---

## 2. 路由体系

### 2.1 路由模式

```
history 模式（无 # 号）
全静态路由 — 所有路由预先定义，登录后按用户权限过滤
```

### 2.2 constantRoutes — 无需权限

| 路径 | 说明 |
|------|------|
| `/login` | 登录页 |
| `/404`, `/401` | 错误页 |
| `/` → `/index` | 首页重定向 |
| `/h5-register` | 访客 H5 注册（独立页） |
| `/repair-complete` | 维修确认（独立页） |
| `/lock` | 锁屏页 |
| `/user/profile` | 个人中心 |

### 2.3 staticRoutes — 按权限过滤

| 路由 | 权限标识 | 说明 |
|------|----------|------|
| `/system/user` | system:user:list | 用户管理 |
| `/system/dept` | system:dept:list | 部门管理 |
| `/system/role` | system:role:list | 角色管理 |
| `/system/menu` | system:menu:list | 菜单管理 |
| `/device/list` | device:list:list | 设备列表 |
| `/device/topology` | device:topology:list | 设备拓扑 |
| `/device/heartbeat` | device:heartbeat:list | 心跳日志 |
| `/device/fault` | device:fault:list | 故障管理 |
| `/device/snmp` | device:snmp:list | SNMP 管理 |
| `/device/repair` | device:repair:list | 维修工单 |
| `/device/parts` | device:parts:list | 备件管理 |
| `/visitor/appointment` | visitor:appointment:list | 访客预约 |
| `/visitor/approval` | visitor:approval:list | 审批管理 |
| `/visitor/register` | visitor:register:list | 现场登记 |
| `/visitor/log` | visitor:log:list | 访客记录 |
| `/smart/*` | smart:*:list | 智能化管理（6 个子路由） |
| `/attendance/*` | attendance:*:list | 考勤管理（2 个子路由） |
| `/sms/*` | sms:*:list | 短信管理（2 个子路由） |

---

## 3. API 层（src/api/）

### 请求封装（src/utils/request.js）

```
Axios 实例: baseURL=环境变量, timeout=10s

请求拦截器:
  ├── 注入 Authorization: Bearer <Token> (Cookie读取)
  ├── GET 请求参数序列化
  └── 防重复提交检查 (sessionStorage, 1s间隔)

响应拦截器:
  ├── 200 → 返回 res.data
  ├── 401 → 弹出重新登录框 → LogOut → 跳转首页
  ├── 500 → Error 消息通知
  ├── 601 → Warning 消息
  └── 网络错误 → 中文化错误提示
```

### API 模块列表

| 模块 | 路径 | 接口数 |
|------|------|--------|
| login.js | src/api/login.js | 7 (login/register/logout/captcha/info/config) |
| user.js | src/api/system/user.js | 12 (CRUD + 状态 + 密码 + 头像 + 角色授权) |
| role.js | src/api/system/role.js | 10 (CRUD + 数据权限 + 用户授权) |
| dept.js | src/api/system/dept.js | 4 (树形CRUD) |
| menu.js | src/api/system/menu.js | 6 (树形CRUD + 角色菜单) |
| config.js | src/api/system/config.js | 6 (参数配置 + 缓存刷新) |
| notice.js | src/api/system/notice.js | 5 (通知公告) |
| device.js | src/api/device/device.js | 12 (设备CRUD + 拓扑 + 端口 + 状态) |
| repair.js | src/api/device/repair.js | 7 (维修工单流程) |
| visitor.js | src/api/visitor/visitor.js | 15 (预约 + 日志 + 审批流程) |
| dashboard.js | src/api/dashboard/dashboard.js | 2 (设备/访客仪表盘) |
| operlog.js | src/api/monitor/operlog.js | 4 (操作日志) |
| logininfor.js | src/api/monitor/logininfor.js | 4 (登录日志) |
| smslog.js | src/api/smslog.js | 2 (短信日志) |

---

## 4. Vuex 状态管理（src/store/）

| 模块 | 文件 | 核心状态 |
|------|------|----------|
| app | modules/app.js | sidebar 开关、设备类型、组件尺寸 |
| user | modules/user.js | token, roles[], permissions[], userInfo |
| permission | modules/permission.js | 动态生成的路由表 |
| settings | modules/settings.js | 主题、导航类型、标签页样式 |
| tagsView | modules/tagsView.js | 已访问视图集合、缓存视图、持久化 |
| dict | modules/dict.js | 字典数据缓存 |
| lock | modules/lock.js | 锁屏状态 |

---

## 5. 权限控制（三层）

### 第一层：路由级

```
permission.js 路由守卫:
  token 存在 → GetInfo → GenerateRoutes (过滤 staticRoutes)
  token 不存在 → 白名单放行 / 其他跳转登录
```

### 第二层：指令级

```html
<!-- 权限指令 -->
<el-button v-hasPermi="['system:user:add']">新增</el-button>

<!-- 角色指令 -->
<el-button v-hasRole="['admin']">管理员操作</el-button>
```

### 第三层：函数级

```javascript
// JS 中判断
import { checkPermi } from '@/utils/permission'
if (checkPermi('system:user:edit')) { ... }

// Vue 实例中
this.$auth.hasPermi('system:user:edit')
this.$auth.hasRole('admin')
this.$auth.hasPermiOr(['system:user:add', 'system:user:edit'])
```

Permission 工具: `admin` 角色 = 超级管理员绕过，`*:*:*` 权限 = 全部权限

---

## 6. 全局注册组件（8 个）

| 组件 | 文件 | 用途 |
|------|------|------|
| Pagination | components/Pagination/ | 分页组件 |
| RightToolbar | components/RightToolbar/ | 表格工具栏（搜索/列/刷新） |
| Editor | components/Editor/ | Quill 富文本编辑器 |
| FileUpload | components/FileUpload/ | 文件上传 |
| ImageUpload | components/ImageUpload/ | 图片上传 |
| ImagePreview | components/ImagePreview/ | 图片预览 |
| DictTag | components/DictTag/ | 字典标签展示 |
| DictData | components/DictData/ | 字典数据插件 |

---

## 7. 自定义指令（6 个）

| 指令 | 用途 |
|------|------|
| `v-hasPermi` | 权限控制（移除 DOM） |
| `v-hasRole` | 角色控制（移除 DOM） |
| `v-clipboard` | 剪贴板复制 |
| `v-dialogDrag` | 弹窗拖拽 |
| `v-dialogDragWidth` | 弹窗宽度调整 |
| `v-dialogDragHeight` | 弹窗高度调整 |

---

## 8. 布局框架

```
app-wrapper
 ├── Sidebar（侧边栏）
 │   ├── Logo
 │   └── SidebarItem（递归菜单）
 └── main-container
     ├── Navbar（顶部栏）
     │   ├── Hamburger（侧边栏折叠）
     │   ├── Breadcrumb（面包屑）
     │   ├── TopNav（顶部导航 — navType=2）
     │   └── RightMenu（搜索/全屏/尺寸/头像下拉菜单）
     ├── TagsView（标签页）
     └── AppMain（<keep-alive> + <router-view>）
```

**三种导航模式**：
- navType=1: 左侧侧边栏（默认）
- navType=2: 顶部导航菜单
- navType=3: 顶部栏混合模式
