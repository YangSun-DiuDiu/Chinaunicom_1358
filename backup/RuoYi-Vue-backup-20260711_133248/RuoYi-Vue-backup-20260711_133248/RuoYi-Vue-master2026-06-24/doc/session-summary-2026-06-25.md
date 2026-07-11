# 会话总结 — 2026-06-25

> 智慧园区管理系统完整改造与部署会话记录

---

## 一、GitHub 仓库

```
https://github.com/YangSun-DiuDiu/Chinaunicom_1358
分支: master
远程: fe74094 (最新)
```

## 二、本次会话完成的工作

### Phase 1: 项目架构文档化
- 探索 6 个模块结构，输出 10 篇架构文档到 `doc/`
- 文档覆盖：项目概述、模块架构、各模块详解、工作流程与数据流

### Phase 2: 代码精简 + 多租户改造（15 Tasks, 18 commits）
| 类别 | 内容 |
|------|------|
| 删除死代码 | ScheduleConfig, GenConstants, menu.js, dict/data.js, generator/, index_v1.vue |
| 抽取基类 | BaseCrudController, BaseCrudService, BaseCrudServiceImpl |
| 前端 Mixin | listPageMixin, formDialogMixin, CrudTable 组件 |
| 多租户核心 | BaseEntity.deptId, DeptScopeInterceptor, DeptFillAspect |
| 组织切换 | SysDeptController.switchContext/clearContext, Navbar 下拉框 |
| XML 映射 | 13 个 Mapper XML 添加 dept_id 映射 |
| 数据库 | 9 张业务表添加 dept_id 列 + 索引 |

### Phase 3: 部署上线
- 后端端口改为 8090，Nginx 端口 80
- 打包 `zhihuiyuanqu20260625` 部署到 `1.94.26.126`
- 创建部署 skill: `.claude/skills/zhihuiyuanqu-deploy/SKILL.md`
- 输出部署文档: `doc/部署指南-智慧园区管理系统.md`

### Phase 4: Bug 修复（9 commits）
| 问题 | 修复 |
|------|------|
| 字典接口 404 | 新建 `SysDictDataController` |
| 配件使用记录 SQL 错误 | 加 `repair_no` 列 |
| 维修反馈页 500 | 修复 XML `r.dept_id` → `dept_id` |
| 上传路径 Windows→Linux | `D:/ruoyi/uploadPath` → `/home/ruoyi/uploadPath` |
| 短信模板改造 | 统一为"设备离线告警，设备：{name}，已离线，请及时处理。设备登录码：{token}" |
| 维修页面重构 | 新增 token 输入模式 |
| 照片预览不显示 | Nginx 加 `/profile/` 代理 |
| 离线不发短信 | 心跳任务自动创建工单 + 短信 |
| 离线双短信 | 统一走 repair create 模板 |

---

## 三、服务器关键配置

| 项目 | 值 |
|------|-----|
| 前端 | `http://1.94.26.126:80` (Nginx) |
| 后端 | `http://1.94.26.126:8090` (SpringBoot) |
| 部署目录 | `/root/zhihuiyuanqu20260625/` |
| 前端目录 | `/usr/share/nginx/zhihuiyuanqu/dist/` |
| 上传目录 | `/home/ruoyi/uploadPath/` |
| Nginx 配置 | `/etc/nginx/conf.d/zhihuiyuanqu.conf` |
| 数据库 | `1.94.26.126:3306/ry101` (sadmin/Chinaunicom@1358) |
| Redis | `1.94.26.126:6379` (db:0) |
