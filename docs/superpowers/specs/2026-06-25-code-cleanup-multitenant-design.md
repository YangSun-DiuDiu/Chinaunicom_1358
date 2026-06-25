# 代码精简 + 多租户改造 — 设计方案

> **日期**: 2026-06-25
> **状态**: 已审核
> **项目**: RuoYi-Vue 智慧园区管理系统 v3.9.2

---

## 1. 目标概述

分两阶段推进：
1. **第一阶段：代码精简** — 删除死代码、抽取重复模板、整理包结构
2. **第二阶段：多租户改造** — 在现有 Dept + DataScope 基础上强化为自动/默认组织隔离

---

## 2. 第一阶段：代码精简

### 2.1 死代码清理清单

| 序号 | 目标 | 位置 | 原因 |
|------|------|------|------|
| 1 | `ScheduleConfig.java` | ruoyi-quartz/config/ | 已全注释，使用 RAMJobStore 替代 |
| 2 | `GenConstants.java` | ruoyi-common/constant/ | 代码生成器常量，本项目不使用 |
| 3 | 旧 Swagger 配置片段 | ruoyi-admin/ResourcesConfig.java | 已迁移到 SpringDoc |
| 4 | `menu.js` | ruoyi-ui/src/api/ | 旧动态路由残留 |
| 5 | `dict/data.js` | ruoyi-ui/src/api/system/ | 空壳文件，总返回空数组 |
| 6 | `generator/` 目录 | ruoyi-ui/src/utils/generator/ | 代码生成器前端工具 |
| 7 | 未使用的 import | 全项目 | 全局扫描清理 |
| 8 | 注释掉的代码块 | 全项目 | 无用的注释代码移除 |

### 2.2 后端重复代码抽取

**新增基类：**

```
ruoyi-common
 └── core/
     ├── controller/BaseCrudController.java     (新增)
     └── service/BaseCrudService.java           (新增)
          └── impl/BaseCrudServiceImpl.java     (新增)
```

**BaseCrudController<E, S extends BaseCrudService<E>>**：
- `list(entity)` → startPage → service.selectList → getDataTable
- `getInfo(id)` → service.selectById
- `add(entity)` → service.insert → toAjax
- `edit(entity)` → service.update → toAjax
- `remove(ids)` → service.deleteByIds → toAjax
- `export(entity)` → service.selectList → ExcelUtil

**BaseCrudServiceImpl<M extends BaseMapper<E>, E>**：
- 默认 CRUD 实现（selectList/selectById/insert/update/deleteByIds）
- 子类通过 `@Override` 覆盖差异化逻辑

**不强制继承的 Controller**：登录、WebSocket、回调、仪表盘等复杂 Controller 保持现有结构。

### 2.3 前端重复代码抽取

**新增 Mixin：**

```
ruoyi-ui/src/mixins/
 ├── listPageMixin.js      (搜索/分页/选择/导出/刷新 通用逻辑)
 └── formDialogMixin.js    (新增/编辑弹窗 表单校验/提交)
```

**新增通用组件：**

```
ruoyi-ui/src/components/
 └── CrudTable/index.vue   (columns 配置化表格组件)
```

**精简策略**：
- 列表页 → 使用 `listPageMixin` 覆盖公共逻辑
- 弹窗 → 使用 `formDialogMixin` 覆盖表单流程
- 不强制所有页面改造，逐步替换

### 2.4 包结构微调

| 问题 | 调整 |
|------|------|
| ruoyi-system/domain/ 根目录 21 个实体 | 按业务子包归类：device/ visitor/ attendance/ sms/ |
| ruoyi-admin/controller/ 目录扁平化 | 保持现状，不增大改动面 |

---

## 3. 第二阶段：多租户改造

### 3.1 租户模型

**租户 = Dept（部门/组织）**

- 用户属于一个 Dept（已有 `sys_user.dept_id`）
- 业务数据属于一个 Dept（新增/复用 `dept_id` 字段）
- 用户默认只看本组织数据

**权限层级**：
```
管理员 (userId=1 或 data_scope=ALL)
  └── 可切换组织视角 → 查看任意组织数据
普通用户
  └── 绑定所属 Dept → 自动过滤，不可切换
```

### 3.2 数据库改造

所有业务表添加 `dept_id` 字段（已有则跳过）：

```sql
-- 设备相关
ALTER TABLE iot_device ADD COLUMN dept_id BIGINT COMMENT '所属组织ID';
ALTER TABLE iot_device_repair ADD COLUMN dept_id BIGINT COMMENT '所属组织ID';
ALTER TABLE iot_device_port ADD COLUMN dept_id BIGINT COMMENT '所属组织ID';
ALTER TABLE iot_device_heartbeat_log ADD COLUMN dept_id BIGINT COMMENT '所属组织ID';
ALTER TABLE iot_device_status_log ADD COLUMN dept_id BIGINT COMMENT '所属组织ID';

-- 访客相关
ALTER TABLE visitor_appointment ADD COLUMN dept_id BIGINT COMMENT '所属组织ID';
ALTER TABLE visitor_log ADD COLUMN dept_id BIGINT COMMENT '所属组织ID';

-- 短信相关
ALTER TABLE sys_sms_log ADD COLUMN dept_id BIGINT COMMENT '所属组织ID';

-- 考勤相关
ALTER TABLE attendance_callback_log ADD COLUMN dept_id BIGINT COMMENT '所属组织ID';

-- 索引
CREATE INDEX idx_iot_device_dept ON iot_device(dept_id);
CREATE INDEX idx_iot_device_repair_dept ON iot_device_repair(dept_id);
CREATE INDEX idx_visitor_appointment_dept ON visitor_appointment(dept_id);
CREATE INDEX idx_visitor_log_dept ON visitor_log(dept_id);
CREATE INDEX idx_sys_sms_log_dept ON sys_sms_log(dept_id);
```

**不添加的白名单表（系统表/全局共享）**：
- sys_user, sys_dept, sys_role, sys_menu, sys_post
- sys_user_role, sys_role_menu, sys_role_dept, sys_user_post
- sys_dict_type, sys_dict_data
- sys_config, sys_notice, sys_notice_read
- sys_oper_log, sys_logininfor
- sys_job, sys_job_log

### 3.3 全局过滤实现

**方案：MyBatis 拦截器 + 自动填充**

#### 3.3.1 查询自动过滤

```java
// 新增: ruoyi-framework/interceptor/DeptScopeInterceptor.java
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare",
               args = {Connection.class, Integer.class})
})
public class DeptScopeInterceptor implements Interceptor {
    // 拦截所有 SELECT 查询
    // 1. 检查是否为业务表（不在白名单中）
    // 2. 获取当前用户 dept_id
    // 3. 自动追加 WHERE dept_id = currentUser.deptId
    // 4. 管理员跳过 (userId == 1 或 dataScope == ALL)
}
```

#### 3.3.2 写入自动填充

```java
// BaseEntity 新增字段:
private Long deptId;

// 全局 AOP 拦截 Controller 的 add/edit 方法:
@Aspect
public class DeptFillAspect {
    @Before("execution(* com.ruoyi.web.controller..*.add(..)) || ...")
    public void fillDeptId(JoinPoint joinPoint) {
        Object entity = joinPoint.getArgs()[0];
        if (entity instanceof BaseEntity) {
            BaseEntity be = (BaseEntity) entity;
            if (be.getDeptId() == null) {
                be.setDeptId(SecurityUtils.getDeptId());
            }
        }
    }
}
```

#### 3.3.3 现有 DataScope 保留降级

- 保留 `@DataScope` 注解和 `DataScopeAspect`，用于**复杂跨部门数据权限**（如维修工单跨部门转派）
- 简单场景不再需要手写 `@DataScope`

### 3.4 管理员组织切换

**后端**：新增接口 `/system/dept/switchContext/{deptId}`
- 将选择的 dept_id 存入 Redis（前缀 `DEPT_CONTEXT_KEY + userId`）
- DeptScopeInterceptor 读取该值替代默认 dept_id

**前端**：顶部导航栏新增"组织"下拉选择框
- 仅管理员可见
- 切换后刷新当前页面数据

### 3.5 缓存 Key 改造

所有带业务数据的 Redis 缓存 Key 追加 `deptId` 后缀：

```
改造前: SYS_CONFIG_KEY:sys.config.key
改造后: SYS_CONFIG_KEY:sys.config.key:dept_1

改造前: DICT_KEY:dict_type
改造后: DICT_KEY:dict_type:dept_1
```

> 系统级配置/字典保持不变（不追加 deptId），组织级配置追加。

---

## 4. 影响范围汇总

| 层 | 改动项 | 影响 |
|----|--------|------|
| 数据库 | 业务表 +dept_id + 索引 | ~10 张表 ALTER TABLE |
| common | BaseEntity +deptId, 新增 BaseCrudController/Service | 新增 3 个基类 |
| framework | 新增 DeptScopeInterceptor + DeptFillAspect | 新增 2 个类 |
| system | domain 分包, 各业务实体加 dept_id | ~15 个实体类微调 |
| admin | Controller 支持切换组织 | 1-2 个新接口 |
| quartz | 删除 ScheduleConfig | 1 个文件删除 |
| ui | 删除 generator, menu.js, dict/data.js | 3 个删除 |
| ui | 新增 mixin, CrudTable 组件 | 3 个新增 |
| ui | 顶部栏增加组织切换下拉框 | Navbar 微调 |

---

## 5. 不做的事（明确排除）

1. ❌ 不引入独立的 tenant 表 — 复用 dept 作租户
2. ❌ 不改动若依核心框架代码（ruoyi-framework 基础配置类）
3. ❌ 不做数据库分库分表
4. ❌ 不强制所有 Controller 继承基类
5. ❌ 不改造登录/WebSocket/回调等特殊 Controller
6. ❌ 不修改前端路由结构

---

## 6. 风险与回滚

| 风险 | 应对 |
|------|------|
| DeptScopeInterceptor 影响查询性能 | 白名单机制 + 简单 indexed 列过滤 |
| 清洗误删有效代码 | 每步 commit 独立，Git 可回滚 |
| 已有数据 dept_id 为空 | 发布前执行补数脚本 |
| 管理员切换组织后数据混乱 | 切换操作清空当前页面缓存 |

---

## 7. 验证标准

### 第一阶段完成标准：
- [ ] 所有死代码文件已删除
- [ ] BaseCrudController/Service 可正常工作
- [ ] 前端 mixin 覆盖至少 2 个页面验证
- [ ] 项目编译无新增错误
- [ ] 原有功能回归测试通过

### 第二阶段完成标准：
- [ ] 所有业务表有 dept_id 且索引生效
- [ ] 普通用户登录只能看到本组织数据
- [ ] 管理员可切换组织视角
- [ ] 新增数据自动填充 dept_id
- [ ] 登录/操作日志正常记录
- [ ] 原有权限体系未受影响
