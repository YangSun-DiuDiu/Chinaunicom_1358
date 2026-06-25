# 07 — ruoyi-admin Web 入口模块

## 模块定位

**Web 服务入口**，提供 REST API 控制器层、Spring Boot 启动、配置管理、静态资源托管、WebSocket 端点。不包含业务逻辑，所有逻辑委托给 ruoyi-system 的 Service 层。

**Maven 坐标**: `com.ruoyi:ruoyi-admin:3.9.2` | 打包: JAR（支持 WAR）
**依赖**: ruoyi-framework + ruoyi-quartz

---

## 1. 启动类与入口

### RuoYiApplication.java

```java
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class RuoYiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RuoYiApplication.class, args);
    }
}
```

- 排除 DataSourceAutoConfiguration — 使用 DruidConfig 手动管理数据源
- 启动成功后打印 ASCII Banner

### RuoYiServletInitializer.java

```java
public class RuoYiServletInitializer extends SpringBootServletInitializer {
    // 支持 WAR 部署到外部 Tomcat
}
```

---

## 2. 配置文件一览

| 文件 | 路径 | 核心内容 |
|------|------|----------|
| `application.yml` | src/main/resources/ | 端口8080、Redis、JWT(120分钟)、MyBatis、SpringDoc、XSS配置、文件上传限制 |
| `application-druid.yml` | src/main/resources/ | 主从数据源、Druid监控台(ruoyi/123456)、慢SQL、防火墙 |
| `logback.xml` | src/main/resources/ | 4个Appender: 控制台 + Info日志 + Error日志 + 用户操作日志(60天保留) |
| `banner.txt` | src/main/resources/ | 佛祖保佑 Banner |
| `mybatis/mybatis-config.xml` | src/main/resources/mybatis/ | MyBatis 全局配置 |
| `i18n/messages.properties` | src/main/resources/i18n/ | 18 条国际化消息（校验/登录/权限错误提示） |

---

## 3. 控制器全景（40+ Controller）

### 3.1 系统管理（10 个 Controller）

| Controller | URL 前缀 | 功能 |
|------------|----------|------|
| `SysLoginController` | `/` | 登录、获取用户信息、路由、系统配置 |
| `SysIndexController` | `/` | 欢迎页、解锁屏幕 |
| `SysProfileController` | `/system/user/profile` | 个人信息、改密、头像上传 |
| `SysUserController` | `/system/user` | 用户CRUD、导入导出、角色分配 |
| `SysRoleController` | `/system/role` | 角色CRUD、数据权限、菜单授权 |
| `SysMenuController` | `/system/menu` | 菜单CRUD、树形选择 |
| `SysDeptController` | `/system/dept` | 部门树CRUD |
| `SysConfigController` | `/system/config` | 参数配置、缓存刷新 |
| `SysNoticeController` | `/system/notice` | 通知公告CRUD、已读管理 |
| `OrgSyncController` | `/system/orgSync` | 组织同步（钉钉/企业微信） |

### 3.2 设备管理（9 个 Controller）

| Controller | URL 前缀 | 功能 |
|------------|----------|------|
| `DeviceController` | `/device` | 设备CRUD、在线统计、拓扑、导出 |
| `DeviceFaultController` | `/device/fault` | 故障管理 |
| `DeviceHeartbeatLogController` | `/device/heartbeatLog` | 心跳日志 |
| `DevicePartsController` | `/device/parts` | 备件管理 |
| `DevicePortController` | `/device/port` | 端口配置 |
| `DeviceRepairController` | `/device/repair` | 维修工单管理 |
| `DeviceRepairPublicController` | `/device/repair/public` | **公共维修完成接口(免登)** |
| `DeviceStatusLogController` | `/device/statusLog` | 状态变更日志 |
| `SnmpController` | `/snmp` | SNMP 监控查询 |

### 3.3 访客管理（4 个 Controller）

| Controller | URL 前缀 | 功能 |
|------------|----------|------|
| `VisitorAppointmentController` | `/visitor/appointment` | 预约CRUD、审批流程 |
| `VisitorH5Controller` | `/visitor/h5` | **H5自助登记(免登)**，生成8位通行码 |
| `VisitorLogController` | `/visitor/log` | 进出记录、导出 |
| `VisitorPassController` | `/visitor/pass` | 通行验证、门禁控制 |

### 3.4 智能化管理（4 个 Controller + WebSocket）

| Controller | URL 前缀 | 功能 |
|------------|----------|------|
| `SmartAccessController` | `/smart/access` | 人车出入设备、视频设备、地图、Ping检测 |
| `SmartPermissionController` | `/smart/permission` | 出入权限管理 |
| `DeviceTestController` | `/smart/test` | 设备测试 |
| `VideoStreamController` | `/smart/video` | 视频流 + **WebSocket端点** `/ws/video/{deviceId}` |

### 3.5 考勤管理（3 个 Controller）

| Controller | URL 前缀 | 功能 |
|------------|----------|------|
| `AttendanceController` | `/attendance` | 考勤记录CRUD、导出 |
| `AttendanceAppController` | `/attendance/app` | 钉钉/企微应用配置、连接测试 |
| `AttendanceCallbackController` | `/attendance/callback` | **钉钉/企微回调接收(免登)** + **WebSocket端点** `/ws/attendance` |

### 3.6 监控与其他（6 个 Controller）

| Controller | URL 前缀 | 功能 |
|------------|----------|------|
| `SysLogininforController` | `/monitor/logininfor` | 登录日志 |
| `SysOperlogController` | `/monitor/operlog` | 操作日志 |
| `CaptchaController` | `/captchaImage` | 验证码生成（数学/字符） |
| `CommonController` | `/common/*` | 文件上传下载 |
| `DashboardController` | `/dashboard/*` | 设备/访客仪表盘统计 |
| `SmsConfigController` + `SmsLogController` | `/sms/*` | 短信配置与日志 |

---

## 4. WebSocket 端点

### 考勤实时推送 — `/ws/attendance`

```
客户端连接 ws://host:port/ws/attendance
接收 JSON: {"type":"CALLBACK", "data":{考勤记录}}
心跳: 客户端发送 {"type":"PING"}
```

### 视频流实时转发 — `/ws/video/{deviceId}`

```
客户端连接 ws://host:port/ws/video/{deviceId}
接收视频帧（通过 viewer 集合广播）
```

---

## 5. 安全白名单（无需登录的 URL）

```
/login, /register, /captchaImage
/pass/**                  (访客通行验证)
/visitor/h5/**             (访客H5自助登记)
/h5-register               (H5注册页面)
/device/repair/public/**   (维修确认公开接口)
/repair-complete
/attendance/callback/**   (钉钉/企微回调)
/ws/**                    (WebSocket端点)
/swagger-ui.html, /v3/api-docs/**, /swagger-ui/**
/druid/**                 (Druid监控)
静态资源 (*.html, *.css, *.js, /profile/**)
```

---

## 6. 静态资源（前端嵌入）

```
src/main/resources/static/
 ├── index.html           → Vue SPA 入口
 ├── pass/index.html      → 访客通行验证页面
 ├── static/css/          → ~30+ CSS chunk（含gzip）
 ├── static/js/           → ~50+ JS chunk（含gzip）
 │   ├── app.*.js         (业务代码)
 │   ├── chunk-libs.*.js  (第三方库)
 │   └── chunk-elementUI.*.js (Element UI)
 ├── static/img/          → logo, 401/404页, 登录背景
 ├── static/fonts/        → Element 图标字体
 └── favicon.ico, robots.txt
```

> 生产部署时前端由 Nginx 提供静态文件，后端 JAR 仅提供 API。
