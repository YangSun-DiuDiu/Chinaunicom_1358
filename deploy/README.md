# 智慧园区管理系统 - 部署说明

## 部署包结构

```
deploy/
├── dist/                  # 前端静态文件（nginx 部署）
├── ruoyi-admin.jar        # 后端 Spring Boot 可执行JAR（86MB）
├── sql/                   # 数据库初始化脚本
├── start-server.bat       # Windows 启动脚本
├── start-server.sh        # Linux/Mac 启动脚本
└── nginx.conf             # Nginx 配置示例
```

## 部署步骤

### 1. 数据库初始化

```bash
# 在 MySQL 中创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS `ry-vue100` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci"

# 导入初始化脚本
mysql -u root -p ry-vue100 < sql/ry_20260417.sql
mysql -u root -p ry-vue100 < sql/ry-vue100-migration.sql
mysql -u root -p ry-vue100 < sql/ry-vue100-accounts.sql
mysql -u root -p ry-vue100 < sql/quartz.sql
```

### 2. 后端部署

```bash
# Windows
start-server.bat

# Linux/Mac
chmod +x start-server.sh && ./start-server.sh
```

默认端口: 8080，可修改 `application.yml` 中的 `server.port`

### 3. 前端部署 (Nginx)

1. 安装 nginx
2. 将 `dist/` 目录复制到 nginx 的 html 目录
3. 复制 `nginx.conf` 中的配置到 nginx 配置文件
4. 启动 nginx: `nginx -s reload`

### 4. 访问系统

```
http://localhost
默认账号: admin / admin123
```

## 配置说明

### 后端配置

- 配置文件位置: JAR包内的 `application.yml` 和 `application-druid.yml`
- 可外部覆盖: `java -jar ruoyi-admin.jar --spring.profiles.active=prod`
- 关键配置:
  - 数据库连接: `spring.datasource.*`
  - Redis连接: `spring.redis.*`
  - 文件上传路径: `ruoyi.profile`
  - JWT密钥: `token.secret`

### 短信配置

登录系统后进入"短信管理 → 短信配置"页面设置：
- 阿里云 AccessKey ID / Secret
- 短信签名
- 各业务模板CODE
- 短信开关

## 变更记录

本次部署包含以下变更:
1. 移除右上角源码地址、文档地址、消息通知
2. 修复设备管理和访客管理的导出功能
3. 短信配置重构为阿里云短信API
4. 新增心跳日志导出接口
