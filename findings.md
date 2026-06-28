# 研究发现

## 架构发现

- 全静态路由，前端 `meta.permissions` 过滤
- 租户=Dept，DeptScopeInterceptor 全局自动过滤，需检测 JOIN 别名
- Quartz RAMJobStore，启动时从 DB 加载任务
- ResourcesConfig 映射 `/profile/**` 到文件系统（需 Nginx 代理）

## 常见问题模式

| 问题 | 根因 | 修复方式 |
|------|------|---------|
| 403 无权限 | Controller/路由/菜单权限不一致 | 三方统一 |
| 404 No static resource | 缺少 Controller | 新建端点 |
| dept_id ambiguous | SQL JOIN 两表都有 dept_id | 拦截器检测别名 |
| 照片无法预览 | Nginx 缺 /profile/ 代理 | 添加 location |
| 房间状态不更新 | check-in/out 三元判断不完整 | 统一为 actual vs capacity 比较 |
| 旧数值残留 | 前端默认值和表单用旧值 | 全局替换为字符串状态 |

## 房间状态规则

| 条件 | 状态 | 颜色 |
|------|------|------|
| actual=0 | GREEN | 绿色 |
| 0<actual<capacity | CYAN | 青色 |
| actual>=capacity | BLUE | 蓝色 |
| 手动设置 | GRAY | 灰色(维修) |

## 技术债务（代码审查发现，暂不修复）

| 严重度 | 问题 | 位置 |
|--------|------|------|
| CRITICAL | Controller 直接使用 JdbcTemplate 违反分层 | TenantController, RoomController |
| HIGH | 物理 DELETE 未用 delFlag 逻辑删除 | ApartmentInfoMapper, RoomInfoMapper, TenantInfoMapper |
| HIGH | ServiceImpl 缺少 @DataScope | ApartmentInfoServiceImpl, RoomInfoServiceImpl, TenantInfoServiceImpl |
| HIGH | checkIn/renew 裸 Map 入参无校验 | TenantController |
| HIGH | JdbcTemplate 无异常捕获 | TenantController, RoomController |
| MEDIUM | ServiceImpl 循环单删 | ApartmentInfoServiceImpl, RoomInfoServiceImpl, TenantInfoServiceImpl |
| MEDIUM | checkIn/checkOut 状态重算代码重复 | TenantController |
| MEDIUM | cardList N+1 查询 | RoomController |
| MEDIUM | checkin.vue pageSize=1000 硬编码 | checkin.vue |
| LOW | renew 端点无前端调用 | TenantController |

> 来源: 2026-06-28 代码审查 (Angle C/H/E)

## 关键配置

| 配置项 | 值 |
|--------|-----|
| 后端端口 | 8090 |
| 前端端口 | 80 (Nginx) |
| 部署目录 | /root/zhihuiyuanqu20260625/ |
| 上传目录 | /home/ruoyi/uploadPath/ |
| 数据库 | 1.94.26.126:3306/ry101 (sadmin) |
| GitHub | https://github.com/YangSun-DiuDiu/Chinaunicom_1358 |
