---
name: zhihuiyuanqu-deploy
description: |
  智慧园区管理系统完整部署流程。当你需要打包、部署、发布智慧园区管理系统到服务器时使用。
  触发词：部署、打包、发布、上线、重新部署、更新服务器、编译打包、推到服务器。
  包含：修改端口配置、Maven/NPM构建、打包部署文件、SCP上传、SSH远程配置Nginx、启动后端、验证部署。
---

# 智慧园区管理系统 — 一键部署

## 概述

将 RuoYi-Vue 智慧园区管理系统打包并部署到远程服务器。

**目标服务器**: `1.94.26.126` (root / Chinaunicom@1358)

**部署架构**:
- 前端: Nginx 端口 80 → `/usr/share/nginx/zhihuiyuanqu/dist/`
- 后端: SpringBoot 端口 8090 → `/root/zhihuiyuanqu20260625/ruoyi-admin.jar`
- API代理: Nginx `/prod-api/*` → `http://127.0.0.1:8090/`

## 执行流程

严格按照以下 8 个步骤顺序执行，每步确认通过后再继续。

### Step 1: 修改端口配置

如果尚未修改，执行以下配置变更：

**后端端口 (application.yml)**:
```yaml
server:
  port: 8090
```

**前端 API 路径 (.env.production)**:
```
VUE_APP_BASE_API = '/prod-api'
```
> 说明：前端通过 Nginx 代理访问后端，使用相对路径。Nginx 将 `/prod-api/*` 代理到 `http://127.0.0.1:8090/`。

### Step 2: 编译后端

```bash
cd <项目根目录>
mvn clean package -DskipTests -q
```
验证：`ruoyi-admin/target/ruoyi-admin.jar` 文件存在 (~89MB)。

### Step 3: 编译前端

```bash
cd <项目根目录>/ruoyi-ui
npm run build:prod
```
验证：`dist/` 目录生成，包含 `index.html` 和 `static/` 子目录。

### Step 4: 打包部署文件

```bash
cd <项目根目录>
PACKAGE_DIR="zhihuiyuanqu$(date +%Y%m%d)"
mkdir -p $PACKAGE_DIR
cp ruoyi-admin/target/ruoyi-admin.jar $PACKAGE_DIR/
cp -r ruoyi-ui/dist $PACKAGE_DIR/dist

# 创建启动脚本
cat > $PACKAGE_DIR/start-server.sh << 'SCRIPT'
#!/bin/bash
cd "$(dirname "$0")"
nohup java -jar ruoyi-admin.jar --server.port=8090 --spring.devtools.restart.enabled=false > app.log 2>&1 &
echo "Backend PID: $!"
SCRIPT

# 创建停止脚本
cat > $PACKAGE_DIR/stop-server.sh << 'SCRIPT'
#!/bin/bash
PID=$(ps aux | grep "ruoyi-admin.jar" | grep -v grep | awk '{print $2}')
[ -n "$PID" ] && kill $PID && echo "Stopped PID: $PID" || echo "No running backend"
SCRIPT

# 创建 Nginx 配置
cat > $PACKAGE_DIR/nginx.conf << 'NGINX'
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/zhihuiyuanqu/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /prod-api/ {
        proxy_pass http://127.0.0.1:8090/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /ws/ {
        proxy_pass http://127.0.0.1:8090/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
    }
}
NGINX

chmod +x $PACKAGE_DIR/*.sh
echo "Package: $PACKAGE_DIR ready"
```

### Step 5: 上传到服务器

```bash
scp -o StrictHostKeyChecking=no -r zhihuiyuanqu*/ root@1.94.26.126:/root/
```

### Step 6: 远程配置 Nginx 和部署前端

```bash
ssh root@1.94.26.126 << 'ENDSSH'
set -e
PACKAGE_DIR=/root/zhihuiyuanqu20260625
cd $PACKAGE_DIR

# 部署前端文件
mkdir -p /usr/share/nginx/zhihuiyuanqu
rm -rf /usr/share/nginx/zhihuiyuanqu/dist
cp -r dist /usr/share/nginx/zhihuiyuanqu/dist

# 安装 Nginx (如未安装)
which nginx || apt-get install -y nginx

# 配置 Nginx
cp nginx.conf /etc/nginx/conf.d/zhihuiyuanqu.conf
nginx -t && nginx -s reload || (nginx && echo "Nginx started")

echo "Frontend deployed"
ENDSSH
```

### Step 7: 启动后端服务

```bash
ssh root@1.94.26.126 << 'ENDSSH'
PACKAGE_DIR=/root/zhihuiyuanqu20260625
cd $PACKAGE_DIR

# 停止旧服务
PID=$(ps aux | grep "ruoyi-admin.jar" | grep -v grep | awk '{print $2}')
[ -n "$PID" ] && kill $PID && echo "Stopped old PID: $PID"

# 启动新服务
chmod +x start-server.sh
bash start-server.sh
sleep 10

# 验证后端启动
curl -s -o /dev/null -w "Backend: HTTP %{http_code}\n" http://localhost:8090/
ENDSSH
```

### Step 8: 验证部署

```bash
echo "=== 前端 (Nginx:80) ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" --connect-timeout 5 http://1.94.26.126:80/

echo "=== 后端 API (8090) ==="
curl -s -o /dev/null -w "HTTP %{http_code}\n" --connect-timeout 5 http://1.94.26.126:8090/

echo "=== API 代理 (80 → 8090) ==="
curl -s http://1.94.26.126:80/prod-api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","code":"0","uuid":"test"}' | grep -o '"code":[0-9]*\|"msg":"[^"]*"'
```

预期输出：
```
前端 (Nginx:80): HTTP 200
后端 API (8090): HTTP 200
API 代理 (80 → 8090): "code":200 "msg":"操作成功"
```

## 日常运维

### 更新后端
```bash
# 本地重新编译后
scp ruoyi-admin/target/ruoyi-admin.jar root@1.94.26.126:/root/zhihuiyuanqu20260625/
ssh root@1.94.26.126 'cd /root/zhihuiyuanqu20260625 && bash stop-server.sh && bash start-server.sh'
```

### 更新前端
```bash
cd ruoyi-ui && npm run build:prod
scp -r dist/* root@1.94.26.126:/root/zhihuiyuanqu20260625/dist/
ssh root@1.94.26.126 'cp -r /root/zhihuiyuanqu20260625/dist/* /usr/share/nginx/zhihuiyuanqu/dist/ && nginx -s reload'
```

### 查看日志
```bash
ssh root@1.94.26.126 'tail -100 /root/zhihuiyuanqu20260625/app.log'
```

### 重启服务
```bash
ssh root@1.94.26.126 'cd /root/zhihuiyuanqu20260625 && bash stop-server.sh && bash start-server.sh'
```

## 注意事项

1. **云安全组**: 确保服务器安全组放行端口 80（前端）和 8090（后端）
2. **数据库/Redis**: 依赖 MySQL (1.94.26.126:3306) 和 Redis (1.94.26.126:6379) 已运行
3. **文件编码**: 所有脚本使用 LF 换行符，确保在 Linux 服务器上正常执行
4. **配置路径**: 如部署包日期不同，将 `zhihuiyuanqu20260625` 替换为实际目录名
