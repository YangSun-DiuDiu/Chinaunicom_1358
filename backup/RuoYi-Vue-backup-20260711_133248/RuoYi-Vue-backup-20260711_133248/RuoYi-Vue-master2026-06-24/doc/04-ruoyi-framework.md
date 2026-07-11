# 04 — ruoyi-framework 框架核心模块

## 模块定位

**框架核心层**，提供横切关注点基础设施：Spring Security + JWT 认证、Druid 数据源（主从）、Redis 缓存、AOP 切面（日志/数据权限/限流）、过滤器链、异步任务、全局异常处理、服务器监控、验证码、国际化。

**Maven 坐标**: `com.ruoyi:ruoyi-framework:3.9.2` | 文件数: 38 个 Java 源文件

**依赖**: ruoyi-system → ruoyi-common（传递依赖）

---

## 1. 包结构总览

```
com.ruoyi.framework
 ├── aspectj/          — 4 AOP 切面
 │   ├── DataScopeAspect.java      (数据权限 SQL 注入)
 │   ├── DataSourceAspect.java     (主从数据源动态切换)
 │   ├── LogAspect.java            (操作日志自动记录)
 │   └── RateLimiterAspect.java    (Redis+Lua 限流)
 ├── config/           — 14 配置类
 │   ├── SecurityConfig.java       (Spring Security 核心)
 │   ├── DruidConfig.java          (Druid 连接池配置)
 │   ├── RedisConfig.java          (Redis 序列化配置)
 │   ├── FilterConfig.java         (XSS/Referer/Repeatable Filter)
 │   ├── ResourcesConfig.java      (CORS + 资源映射 + 拦截器)
 │   ├── CaptchaConfig.java        (Kaptcha 验证码)
 │   ├── MyBatisConfig.java        (SqlSessionFactory)
 │   ├── ThreadPoolConfig.java     (异步线程池)
 │   ├── I18nConfig.java           (国际化)
 │   ├── ServerConfig.java         (服务器监控配置)
 │   └── properties/               (DruidProperties, PermitAllUrlProperties)
 ├── datasource/       — 动态数据源
 │   ├── DynamicDataSource.java            (AbstractRoutingDataSource)
 │   └── DynamicDataSourceContextHolder.java (ThreadLocal 数据源 key)
 ├── interceptor/      — 拦截器
 │   ├── RepeatSubmitInterceptor.java      (抽象重复提交拦截器)
 │   └── impl/SameUrlDataInterceptor.java  (Redis 去重实现)
 ├── manager/          — 管理器
 │   ├── AsyncManager.java          (异步任务线程池单例)
 │   └── factory/AsyncFactory.java  (日志持久化任务工厂)
 ├── security/         — 安全组件
 │   ├── filter/JwtAuthenticationTokenFilter.java  (JWT 认证过滤器)
 │   ├── handle/AuthenticationEntryPointImpl.java  (401 处理)
 │   ├── handle/LogoutSuccessHandlerImpl.java      (登出处理)
 │   └── context/                                   (ThreadLocal 持有)
 └── web/
     ├── exception/GlobalExceptionHandler.java  (全局异常 @RestControllerAdvice)
     ├── domain/Server.java                     (OSHI 服务器监控)
     └── service/                               (安全相关服务)
         ├── PermissionService.java       (权限校验 "ss")
         ├── SysLoginService.java         (登录流程编排)
         ├── SysPasswordService.java      (密码重试锁定)
         ├── SysPermissionService.java    (权限加载)
         ├── SysRegisterService.java      (用户注册)
         ├── TokenService.java            (JWT + Redis 令牌管理)
         └── UserDetailsServiceImpl.java  (Spring Security UserDetails 加载)
```

---

## 2. 安全认证流程

### 2.1 Spring Security Filter Chain

```
请求 → CorsFilter → JwtAuthenticationTokenFilter → SecurityFilterChain
                                                          │
                                    允许匿名: /login, /register, /captchaImage,
                                            /pass/**, /visitor/h5/**, /ws/**,
                                            /swagger-ui/**, /druid/**, 静态资源
                                                          │
                                    其他: 需要认证 → 401
```

**关键设计**:
- **无状态**: `SessionCreationPolicy.STATELESS`
- **CSRF 禁用**: JWT 模式下不需要
- **BCrypt 密码编码**: `BCryptPasswordEncoder`
- **方法级权限**: `@EnableMethodSecurity(prePostEnabled = true)`

### 2.2 JWT 令牌管理（TokenService）

```
登录 → 生成 fastUUID → 存入 JWT (HS512) → LoginUser 存入 Redis
请求 → 提取 Authorization Header → 解析 JWT → Redis 查找 LoginUser
续期 → 20 分钟内自动刷新 Redis TTL + JWT 过期时间
登出 → 删除 Redis 中 LoginUser
角色变更 → 扫描所有在线用户，刷新权限集合
```

### 2.3 用户认证（UserDetailsServiceImpl）

```
loadUserByUsername()
  ├── 查询 SysUser
  ├── 检查删除/禁用状态
  ├── 校验密码（含重试锁定）
  ├── 加载菜单权限 (admin → "*:*:*", 其他 → 查 sys_menu)
  └── 返回 LoginUser (implements UserDetails)
```

---

## 3. 数据源管理

### 3.1 Druid 连接池配置

```
DruidConfig
 ├── masterDataSource   (主库 — 必启)
 ├── slaveDataSource    (从库 — 条件启用, 默认关闭)
 └── DynamicDataSource  (AbstractRoutingDataSource 包装)

池参数: initialSize=5, minIdle=10, maxActive=20, maxWait=60s
防火墙: WallFilter (SQL 注入防护), 多语句允许
监控: StatViewServlet @ /druid/* (ruoyi/123456)
慢SQL: >1000ms 记录日志
```

### 3.2 主从切换（@DataSource）

```
@DataSource(DataSourceType.SLAVE) → DataSourceAspect → DynamicDataSourceContextHolder
                                                         (ThreadLocal 存储)
方法优先级 > 类优先级, @Inherited 可继承
```

---

## 4. AOP 切面详解

### 4.1 LogAspect — 操作日志

```
@Log 注解触发 → @Before 记时 → @AfterReturning 记录成功日志
                              → @AfterThrowing 记录异常日志
记录内容: 操作人、IP、URL、方法、参数、返回、耗时、业务类型
过滤: password 字段、MultipartFile、HttpServletRequest/Response
持久化: AsyncManager 异步写入 sys_oper_log
```

### 4.2 DataScopeAspect — 数据权限

```
@DataScope 注解触发 → 读取当前角色 data_scope → 拼装 SQL 条件
  DATA_SCOPE_ALL (1)          → 无限制
  DATA_SCOPE_CUSTOM (2)       → dept_id IN (子查询)
  DATA_SCOPE_DEPT (3)         → dept_id = user.deptId
  DATA_SCOPE_DEPT_AND_CHILD (4) → find_in_set 递归
  DATA_SCOPE_SELF (5)         → user_id = currentUserId
结果注入: params.dataScope (MyBatis XML 中 ${params.dataScope})
```

### 4.3 RateLimiterAspect — 限流

```
Redis Lua 脚本滑动窗口: INCR key → TTL 设置 → 超阈值拒绝
支持: DEFAULT(全局) / IP(按IP限流)
```

---

## 5. 全局异常处理（GlobalExceptionHandler）

```
@RestControllerAdvice 统一处理:
  AccessDeniedException           → 403
  HttpRequestMethodNotSupportedEx → 405
  ServiceException                → 业务异常(动态code)
  BindException / MethodArgumentNotValidException → 校验失败
  DemoModeException               → 演示模式
  RuntimeException / Exception    → 500 系统错误
```

---

## 6. 过滤器与拦截器

### FilterConfig（3 个过滤器）

| Filter | Order | URL 模式 | 作用 |
|--------|-------|----------|------|
| XssFilter | HIGHEST | `/system/*,/device/*,/visitor/*,/dashboard/*` | XSS 防护，排除 `/system/notice` |
| RefererFilter | HIGHEST | `/profile/*` | 防盗链（默认关闭） |
| RepeatableFilter | LOWEST | `/*` | 使 RequestBody 可重复读 |

### ResourcesConfig（拦截器 + CORS）

| 组件 | 作用 |
|------|------|
| SameUrlDataInterceptor | 防重复提交（Redis 对比 URL+参数+Tokeη） |
| CorsFilter | 允许所有来源、方法、Header（1800s） |
| 资源映射 | `/profile/**` → 文件系统上传目录 |

---

## 7. Redis 配置

```
RedisConfig
 ├── RedisTemplate: Key=StringRedisSerializer, Value=FastJson2JsonRedisSerializer
 ├── LimitScript: DefaultRedisScript<Long> (Lua 限流脚本)
 └── @EnableCaching (Spring Cache 抽象)

Redis 用途:
  - Token/LoginUser 缓存 (LOGIN_TOKEN_KEY)
  - 验证码缓存 (CAPTCHA_CODE_KEY)
  - 系统配置缓存 (SYS_CONFIG_KEY)
  - 字典缓存 (SYS_DICT_KEY)
  - 防重复提交 (REPEAT_SUBMIT_KEY)
  - 限流计数器 (RATE_LIMIT_KEY)
  - 密码重试计数 (PWD_ERR_CNT_KEY)
```

---

## 8. 服务器监控（Server.java）

使用 OSHI 库采集实时数据：
- **CPU**: 核心数、使用率、系统/用户/等待占比
- **内存**: 总量、已用、可用
- **JVM**: 堆内存、最大堆、GC 信息、JDK 版本
- **系统**: 主机名、操作系统、架构
- **磁盘**: 各分区空间统计
