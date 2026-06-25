# 03 — ruoyi-common 通用工具模块

## 模块定位

**基础设施层**，被所有其他模块依赖，提供项目运行所需的基础类、注解、异常体系、枚举、常量、工具类等。

**Maven 坐标**: `com.ruoyi:ruoyi-common:3.9.2` | 文件数: ~95 个 Java 源文件

---

## 1. 包结构总览

```
com.ruoyi.common
 ├── annotation/      — 9 个自定义注解
 ├── config/          — RuoYiConfig + SensitiveJsonSerializer
 ├── constant/        — 6 个常量类
 ├── core/
 │   ├── controller/  — BaseController（所有 Controller 的父类）
 │   ├── domain/      — BaseEntity, TreeEntity, AjaxResult, TreeSelect
 │   │   ├── entity/  — SysUser, SysDept, SysRole, SysMenu, SysDictData, SysDictType
 │   │   └── model/   — LoginUser (UserDetails), LoginBody, RegisterBody
 │   ├── page/        — TableDataInfo, PageDomain, TableSupport
 │   ├── redis/       — RedisCache
 │   └── text/        — CharsetKit, Convert, StrFormatter
 ├── enums/           — 8 个枚举类型
 ├── exception/       — 20+ 异常类（含 user/file/job 子包）
 ├── filter/          — 6 个过滤器/Wrapper 类
 ├── utils/           — 14 顶层工具类 + 11 子包
 └── xss/             — @Xss 注解 + XssValidator
```

---

## 2. 核心基础类

### 2.1 BaseEntity — 所有实体父类

```
字段: createBy, createTime, updateBy, updateTime, remark, params(Map)
特点: Serializable, JSON 日期格式化, params 非空序列化
```

### 2.2 TreeEntity — 树形实体父类

```
extends BaseEntity
新增: parentName, parentId, orderNum, ancestors, children
用于: 部门、菜单等树形结构
```

### 2.3 AjaxResult — 统一 API 响应

```
extends HashMap<String, Object>
静态方法: success(), error(), warn()
状态码: SUCCESS(200), ERROR(500), WARN(601)
```

### 2.4 BaseController — 所有 Controller 父类

```
提供: startPage(), getDataTable(), success(), toAjax()
用户快捷方法: getLoginUser(), getUserId(), getDeptId(), getUsername()
```

---

## 3. 五大核心实体（shared entities）

| 实体 | 对应表 | 关键字段 |
|------|--------|----------|
| `SysUser` | sys_user | userId, deptId, userName, password(只写), email, roles[] |
| `SysDept` | sys_dept | deptId, parentId, ancestors, deptName, leader, children[] |
| `SysRole` | sys_role | roleId, roleName, roleKey, dataScope, permissions |
| `SysMenu` | sys_menu | menuId, parentId, path, component, perms, icon, children[] |
| `SysDictData` | sys_dict_data | dictCode, dictLabel, dictValue, dictType |

---

## 4. 九大注解（annotation/）

| 注解 | Target | 作用 |
|------|--------|------|
| `@DataScope` | METHOD | 数据权限，注入部门/用户 SQL 过滤条件 |
| `@Log` | METHOD | 操作日志记录（标题、业务类型、操作人类别） |
| `@Excel` | FIELD | Excel 列定义（排序、日期格式、字典翻译、颜色） |
| `@Excels` | FIELD | 多 @Excel 容器（嵌套对象导出） |
| `@RateLimiter` | METHOD | Redis+Lua 滑动窗口限流 |
| `@RepeatSubmit` | METHOD | 防重复提交（Redis 参数去重） |
| `@DataSource` | TYPE/METHOD | 主从数据源切换（MASTER/SLAVE） |
| `@Sensitive` | FIELD | 数据脱敏（手机/身份证/邮箱/银行卡） |
| `@Anonymous` | TYPE/METHOD | 标记无需认证的公共接口 |

---

## 5. 异常体系

```
RuntimeException
 ├── BaseException（模块+错误码+i18n 支持）
 │   ├── UserException → 7 个子类
 │   │   ├── BlackListException       (IP黑名单)
 │   │   ├── CaptchaException          (验证码错误)
 │   │   ├── CaptchaExpireException    (验证码过期)
 │   │   ├── UserNotExistsException    (用户不存在)
 │   │   ├── UserPasswordNotMatchException (密码错误)
 │   │   └── UserPasswordRetryLimitExceedException (重试超限)
 │   └── FileException → 5 个子类（文件名/大小/扩展名）
 ├── ServiceException      (业务异常 — 框架全局处理)
 ├── GlobalException       (系统异常 — 框架全局处理)
 ├── DemoModeException     (演示模式限制)
 └── UtilException         (工具类异常)
```

> **注意**: `TaskException` 继承 `Exception`（受检异常），不是 RuntimeException

---

## 6. 关键工具类一览

| 工具类 | 位置 | 核心功能 |
|--------|------|----------|
| `StringUtils` | utils/ | 继承 Apache Lang3，增强 null/集合/驼峰转换 |
| `SecurityUtils` | utils/ | 获取当前用户、BCrypt密码、权限/角色检查 |
| `RedisCache` | core/redis/ | Redis 操作封装（String/List/Set/Map/Hash） |
| `ExcelUtil` | utils/poi/ | @Excel 注解驱动的导入导出引擎 |
| `SpringUtils` | utils/spring/ | 从非 Spring 环境获取 Bean |
| `DictUtils` | utils/ | 字典缓存管理（Redis） |
| `SqlUtil` | utils/sql/ | ORDER BY 防注入、SQL 关键字过滤 |
| `ReflectUtils` | utils/reflect/ | 反射调用 getter/setter |
| `FileUploadUtils` | utils/file/ | 文件上传、MIME 检测 |
| `IpUtils` / `AddressUtils` | utils/ip/ | IP 获取 + 归属地查询 |

---

## 7. 过滤器（filter/）

| 类 | 类型 | 说明 |
|----|------|------|
| `XssFilter` | Servlet Filter | XSS 脚本过滤（排除白名单） |
| `XssHttpServletRequestWrapper` | HttpServletRequestWrapper | 请求参数 HTML 清理 |
| `RepeatableFilter` | Servlet Filter | 使 Request Body 可重复读取 |
| `RepeatedlyRequestWrapper` | HttpServletRequestWrapper | 缓存 Body 支持多次读取 |
| `RefererFilter` | Servlet Filter | 防盗链 Referer 校验 |
| `PropertyPreExcludeFilter` | Fastjson2 Filter | JSON 序列化属性排除 |

---

## 8. 常量（constant/）

| 类 | 主要内容 |
|----|----------|
| `Constants` | 字符集、Token 前缀、超级管理员、JWT Claim Key、数据权限常量、JSON白名单 |
| `HttpStatus` | 200/201/400/401/403/404/500/601 等状态码 |
| `CacheConstants` | Redis Key 前缀（LOGIN_TOKEN_KEY, CAPTCHA_CODE_KEY 等） |
| `UserConstants` | 用户/角色/部门状态、菜单类型、用户名密码长度 |
| `ScheduleConstants` | 任务调度常量、Misfire 策略、Status 枚举 |
