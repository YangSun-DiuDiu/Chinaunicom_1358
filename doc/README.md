# RuoYi-Vue 智慧园区管理系统 — 项目架构文档

> **生成日期**: 2026-06-24
> **项目版本**: 3.9.2
> **技术栈**: SpringBoot 4.0.3 + Java 17 + MyBatis + Vue2 + Element UI + Redis

---

## 文档索引

| 序号 | 文档 | 内容 |
|------|------|------|
| 01 | [项目概述](./01-项目概述.md) | 项目背景、技术栈、部署架构 |
| 02 | [模块架构](./02-模块架构.md) | 多模块结构、依赖关系、层次架构 |
| 03 | [ruoyi-common 通用工具模块](./03-ruoyi-common.md) | 基础类、注解、异常、工具类、枚举、常量 |
| 04 | [ruoyi-framework 框架核心模块](./04-ruoyi-framework.md) | 安全认证、数据源、AOP、过滤器、拦截器、Redis |
| 05 | [ruoyi-system 业务系统模块](./05-ruoyi-system.md) | 权限体系、设备/访客/考勤/SMS 业务实体与服务 |
| 06 | [ruoyi-quartz 定时任务模块](./06-ruoyi-quartz.md) | Quartz 任务调度、CRON 管理 |
| 07 | [ruoyi-admin Web 入口模块](./07-ruoyi-admin.md) | 控制器层、配置、WebSocket、静态资源 |
| 08 | [ruoyi-ui 前端模块](./08-ruoyi-ui.md) | Vue2 前端架构、路由、状态管理、页面组件 |
| 09 | [工作流程与数据流](./09-工作流程与数据流.md) | 请求链路、认证流程、权限流程、数据流 |

---

## 快速导航

### 模块一览

```
ruoyi (父工程 pom)
 ├── ruoyi-common      — 通用工具、基础类、注解、异常 (95+ 类)
 ├── ruoyi-framework    — 框架核心：安全、数据源、AOP、Redis (38 类)
 ├── ruoyi-system       — 业务系统：实体、Service、Mapper (100+ 类)
 ├── ruoyi-quartz       — 定时任务调度管理 (9 类)
 ├── ruoyi-admin        — Web 入口：40+ Controller、配置 (JAR/WAR)
 └── ruoyi-ui           — Vue2 前端：46+ 页面、25 组件 (独立工程)
```

### 技术选型

| 层次 | 技术 | 版本 |
|------|------|------|
| 后端框架 | Spring Boot | 4.0.3 |
| JDK | Java | 17 |
| ORM | MyBatis + PageHelper | 4.0.1 / 2.1.1 |
| 数据库 | MySQL (Druid连接池) | 1.2.28 |
| 缓存 | Redis (Lettuce) | - |
| 安全 | Spring Security + JWT | jjwt 0.9.1 |
| 前端 | Vue2 + Element UI | 2.6.12 / 2.15.14 |
| 状态管理 | Vuex | 3.6.0 |
| 路由 | Vue Router (History) | 3.4.9 |
| 图表 | ECharts | 5.4.0 |
| API文档 | Springdoc OpenAPI 3 | 3.0.2 |
| 定时任务 | Quartz | (Spring Boot Starter) |
