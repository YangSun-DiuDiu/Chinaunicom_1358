# 06 — ruoyi-quartz 定时任务模块

## 模块定位

基于 **Quartz Scheduler** 的定时任务管理模块，支持从数据库动态管理任务，支持 CRON 表达式，支持暂停/恢复/立即执行。

**Maven 坐标**: `com.ruoyi:ruoyi-quartz:3.9.2` | 依赖: ruoyi-system + ruoyi-common

---

## 1. 包结构

```
com.ruoyi.quartz
 ├── config/
 │   └── ScheduleConfig.java          (已注释 — 使用 Spring Boot 自动配置)
 ├── domain/
 │   ├── SysJob.java                  (任务定义实体, 表 sys_job)
 │   └── SysJobLog.java               (执行日志实体, 表 sys_job_log)
 ├── mapper/
 │   ├── SysJobMapper.java            (sys_job Mapper)
 │   └── SysJobLogMapper.java         (sys_job_log Mapper)
 ├── service/
 │   ├── ISysJobService.java          (任务管理接口)
 │   ├── ISysJobLogService.java       (日志管理接口)
 │   └── impl/
 │       ├── SysJobServiceImpl.java   (任务管理核心实现)
 │       └── SysJobLogServiceImpl.java (日志实现)
 ├── task/
 │   ├── RyTask.java                  (示例任务)
 │   └── DeviceHeartbeatTask.java     (设备心跳检测任务)
 └── util/
     ├── AbstractQuartzJob.java       (Job 抽象基类)
     ├── CronUtils.java               (CRON 校验与下一执行时间)
     ├── JobInvokeUtil.java           (反射调用目标方法)
     ├── QuartzDisallowConcurrentExecution.java (禁止并发 Job)
     ├── QuartzJobExecution.java      (允许并发 Job)
     └── ScheduleUtils.java           (创建/更新/删除 Quartz 调度)
```

---

## 2. 核心设计

### 2.1 运行模式：内存调度器

```
ScheduleConfig.java → 已完全注释
Quartz 使用 Spring Boot 自动配置 → RAMJobStore (无数据库持久化)
原因: 单机部署, 内存模式最高效
```

**启动恢复机制**：

```java
@PostConstruct
public void init() {
    scheduler.clear();                        // 清空调度器
    List<SysJob> jobList = jobMapper.selectJobAll();  // 从DB加载所有任务
    for (SysJob job : jobList) {
        ScheduleUtils.createScheduleJob(scheduler, job); // 重新注册
    }
}
```

### 2.2 任务执行流程

```
Quartz 触发
  → AbstractQuartzJob.execute()
    → before() — 记录开始时间(ThreadLocal)
    → doExecute() → JobInvokeUtil.invokeMethod(sysJob)
        解析目标字符串: "beanName.methodName(args)"
        白名单校验: 必须以 com.ruoyi.quartz.task 开头
                    不包含 springframework/apache/java.net.URL 等禁止包
        反射调用: BeanFactory.getBean(beanName) → Method.invoke()
    → after() — 计算耗时 → 写入 SysJobLog → 异常时记录堆栈
```

### 2.3 调用目标字符串格式

```
格式: beanName.methodName(arg1, arg2, ...)
示例: ryTask.ryNoParams()
      ryTask.ryParams("hello")
      ryTask.ryMultipleParams("hello", true, 100L, 3.14D, 42)
      deviceHeartbeatTask.heartbeatCheck()

支持类型: String, Boolean, Long, Double, Integer
```

### 2.4 安全白名单机制

```
白名单: 必须以 Constants.JOB_WHITELIST_STR 开头 (com.ruoyi.quartz.task)
黑名单: 禁止包含 Constants.JOB_ERROR_STR 中的包名
        (org.springframework, org.apache, java.net.URL 等)
```

---

## 3. 数据库表

### 3.1 sys_job（任务定义表）

| 字段 | 类型 | 说明 |
|------|------|------|
| job_id | BIGINT PK | 自增主键 |
| job_name | VARCHAR(64) | 任务名称 |
| job_group | VARCHAR(100) | 任务组名 |
| invoke_target | VARCHAR(500) | 调用目标字符串 |
| cron_expression | VARCHAR(255) | CRON 表达式 |
| misfire_policy | VARCHAR(20) | 错过策略: 0默认/1忽略/2立即/3不执行 |
| concurrent | CHAR(1) | 是否允许并发: 0允许/1禁止 |
| status | CHAR(1) | 状态: 0正常/1暂停 |
| create_by/create_time/update_by/update_time/remark | - | BaseEntity 字段 |

### 3.2 sys_job_log（执行日志表）

| 字段 | 类型 | 说明 |
|------|------|------|
| job_log_id | BIGINT PK | 自增主键 |
| job_name/job_group/invoke_target | - | 执行快照 |
| job_message | VARCHAR(500) | 执行信息(含耗时) |
| status | CHAR(1) | 执行状态: 0成功/1失败 |
| exception_info | VARCHAR(2000) | 异常堆栈(截断) |
| start_time/end_time | DATETIME | 开始/结束时间 |

---

## 4. 任务管理操作

| 操作 | 方法 | DB 行为 | Quartz 行为 |
|------|------|---------|-------------|
| 新增 | insertJob() | INSERT + status=PAUSE | createScheduleJob + pauseJob |
| 修改 | updateJob() | UPDATE | deleteJob + createScheduleJob |
| 删除 | deleteJob() | DELETE | scheduler.deleteJob |
| 暂停 | pauseJob() | status→1 | scheduler.pauseJob |
| 恢复 | resumeJob() | status→0 | scheduler.resumeJob |
| 执行一次 | run() | 无需改表 | scheduler.triggerJob (不影响调度计划) |
| CRON 校验 | checkCronExpressionIsValid() | - | CronExpression.isValidExpression() |

所有操作均为 `@Transactional(rollbackFor = Exception.class)`，保证 DB 与调度器状态一致。

---

## 5. 并发控制

```java
// ScheduleUtils 根据 concurrent 字段选择 Job 类:
concurrent = "0" (允许)  → QuartzJobExecution (无特殊注解)
concurrent = "1" (禁止)  → QuartzDisallowConcurrentExecution
                          (@DisallowConcurrentExecution 注解 — 跳过重叠触发)
```

Quartz 的 `@DisallowConcurrentExecution`：如果上次执行未结束且下次触发到达，**跳过本次触发**而非并发执行。

---

## 6. 自定义任务

### RyTask — 示例任务

```java
@Component("ryTask")
public class RyTask {
    public void ryNoParams() { ... }
    public void ryParams(String s) { ... }
    public void ryMultipleParams(String s, Boolean b, Long l, Double d, Integer i) { ... }
}
```

### DeviceHeartbeatTask — 设备心跳检测

```java
@Component("deviceHeartbeatTask")
public class DeviceHeartbeatTask {
    // 遍历 iot_device 表所有设备
    // 执行 ping 或 SNMP 检测
    // 记录心跳结果
    public void heartbeatCheck() { ... }
}
```
