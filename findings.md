# 研究发现

## 架构发现

- 全静态路由模式，前端按 `meta.permissions` 过滤
- 租户=Dept（复用部门表），DeptScopeInterceptor 全局自动过滤
- Quartz 内存模式（RAMJobStore），启动时从 DB 加载任务
- ResourcesConfig 映射 `/profile/**` 到文件系统

## 常见问题模式

| 问题 | 根因 | 修复方式 |
|------|------|---------|
| 403 无权限 | Controller/路由/菜单权限字符串不一致 | 统一为 DB 中的值 |
| 404 No static resource | 缺少 Controller 端点 | 新建对应 Controller |
| dept_id ambiguous | SQL 有 JOIN 时两表都有 dept_id | 拦截器自动检测别名加前缀 |
| 权限授权后菜单不渲染 | Redis 缓存未刷新 + 权限字符不匹配 | 用户重新登录 + 修复权限字符串 |
| 照片上传无法预览 | Nginx 缺少 /profile/ 代理 | 添加 location /profile/ 规则 |
| 定时任务使用Spring注入的Bean | Quartz使用RAMJobStore，无法自动注入Spring Bean | 使用SpringUtils.getBean()获取 |

## 技术债务

- SMS 阿里云模板 CODE 均未配置（`sms.tpl.*` 在 sys_config 中缺失）
- `sms.repair.callback.url` 需根据环境修改
- `zhaojg` 和 `qianxm` 测试账号密码需恢复
- 部分 Controller 使用 JdbcTemplate 拼 SQL（非 MyBatis 标准模式）

## 关键配置

| 配置项 | 值 |
|--------|-----|
| 后端端口 | 8090 |
| 前端端口 | 80 |
| 部署目录 | /root/zhihuiyuanqu20260625/ |
| 上传目录 | /home/ruoyi/uploadPath/ |
| 数据库 | 1.94.26.126:3306/ry101 (sadmin) |
| Redis | 1.94.26.126:6379 (db:0) |
| GitHub | https://github.com/YangSun-DiuDiu/Chinaunicom_1358 |
