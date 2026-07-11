---
name: zhihuiyuanqu-deploy
description: |
  通用业务部署 Skill。当你需要部署、打包、发布、上线、更新前端/后端到服务器时使用。触发词包括但不限于：部署、打包、发布、上线、重新部署、更新服务器、编译打包、推到服务器、
  build and deploy、ship、release。无论用户说的是"部署一下"、"把前端更新到服务器"、"重启后端"还是"帮我把代码推上去"，都应该触发此 Skill。
  支持：全量部署、仅前端、仅后端、仅重启四种模式，支持多服务器配置。
---

# 通用业务部署 Skill

## 概述

将当前项目编译并部署到远程服务器，支持多种部署模式和服务器环境。

**部署架构**：

- 前端: Nginx → `/usr/share/nginx/<项目>/dist/`
- 后端: SpringBoot jar → `/root/<项目>/ruoyi-admin.jar`
- API代理: Nginx `/prod-api/*` → `http://127.0.0.1:<后端端口>/`

## 前置步骤：读取配置

部署前必须读取项目根目录的 `deploy.json`：

```json
{
  "default": "production",
  "servers": {
    "<环境名>": {
      "name": "生产环境",
      "ip": "...", "user": "...", "password": "...",
      "frontend": { "remoteDir": "...", "nginxDir": "...", "buildCmd": "...", "buildDir": "..." },
      "backend": { "remoteDir": "...", "jarName": "...", "port": 8090, "buildCmd": "...", "buildOutput": "..." },
      "nginx": { "configPath": "...", "apiProxyPath": "...", "apiProxyTarget": "...", "rootPath": "..." }
    }
  }
}
```

**配置项说明**：

- `frontend.remoteDir` — 前端 dist 文件上传到服务器的临时路径
- `frontend.nginxDir` — Nginx 实际 serve 前端文件的路径
- `backend.remoteDir` — 后端 jar 包在服务器上的存放目录
- `nginx.configPath` — Nginx 站点配置文件路径
- `nginx.apiProxyPath` — 前端调用后端的 API 前缀（如 `/prod-api/`）
- `nginx.apiProxyTarget` — 代理转发的后端地址（如 `http://127.0.0.1:8090/`）

**多服务器选择**：如果 `servers` 中有多个环境，让用户选择目标服务器。如果只有一个，直接使用。

## 部署模式选择

首先根据用户指令判断模式：

| 用户说了什么 | 模式 |
| :--- | :--- |
| "部署"、"全部部署"、"全量部署" | **全量部署** |
| "部署前端"、"前端更新"、"只更新页面" | **仅前端** |
| "部署后端"、"更新后端"、"更新jar" | **仅后端** |
| "重启"、"重启后端"、"重启服务" | **仅重启** |

如果用户没有明确说明，检查 git status 判断改了什么：

- 只有 `ruoyi-ui/` 下的文件改动 → 建议**仅前端**
- 只有 `ruoyi-admin/` 或 `ruoyi-system/` 等后端模块改动 → 建议**仅后端**
- 前后端都有改动 → 建议**全量部署**
- 询问用户确认后再执行

**仅重启模式**：不编译、不上传，直接 SSH 到服务器重启后端服务。适用于修改配置文件后重新加载。

## 四种部署模式

### 模式 1：全量部署

完整编译前后端 + 上传 + 重启。

**Step 1: 编译后端**

```bash
cd <项目根目录>
<backend.buildCmd>
```

验证：`<backend.buildOutput>` 文件存在。

**Step 2: 编译前端**

```bash
cd <项目根目录>/<frontend.buildDir>
<frontend.buildCmd>
```

验证：`dist/` 目录生成。

**Step 3: 上传后端并重启**

```bash
scp -o StrictHostKeyChecking=no <backend.buildOutput> <user>@<ip>:<backend.remoteDir>
ssh <user>@<ip> "cd <backend.remoteDir> \
  && PID=\$(ps aux | grep '<jarName>' | grep -v grep | awk '{print \$2}') \
  && [ -n \"\$PID\" ] && kill \$PID && echo \"Stopped PID: \$PID\" \
  && nohup java -jar <jarName> --server.port=<port> > app.log 2>&1 & \
  && sleep 5 && curl -s -o /dev/null -w 'Backend: HTTP %{http_code}\n' http://localhost:<port>/"
```

**Step 4: 上传前端并更新 Nginx**

```bash
scp -o StrictHostKeyChecking=no -r <frontend.buildDir>/dist/* <user>@<ip>:<frontend.remoteDir>
ssh <user>@<ip> "cp -r <frontend.remoteDir>* <frontend.nginxDir> && nginx -s reload"
```

**Step 5: 验证部署**

```bash
curl -s -o /dev/null -w "Frontend: HTTP %{http_code}\n" --connect-timeout 5 http://<ip>:80/
curl -s -o /dev/null -w "Backend: HTTP %{http_code}\n" --connect-timeout 5 http://<ip>:<port>/
```

### 模式 2：仅前端

只编译前端 + 上传 + Nginx reload。后端不动。

```bash
# 编译
cd <项目根目录>/<frontend.buildDir> && <frontend.buildCmd>

# 上传
scp -o StrictHostKeyChecking=no -r dist/* <user>@<ip>:<frontend.remoteDir>

# 更新 Nginx
ssh <user>@<ip> "cp -r <frontend.remoteDir>* <frontend.nginxDir> && nginx -s reload"

# 验证
curl -s -o /dev/null -w "Frontend: HTTP %{http_code}\n" --connect-timeout 5 http://<ip>:80/
```

### 模式 3：仅后端

只编译后端 + 上传 + 重启后端。前端不动。

```bash
# 编译
cd <项目根目录> && <backend.buildCmd>

# 上传并重启
scp -o StrictHostKeyChecking=no <backend.buildOutput> <user>@<ip>:<backend.remoteDir>
ssh <user>@<ip> "cd <backend.remoteDir> \
  && PID=\$(ps aux | grep '<jarName>' | grep -v grep | awk '{print \$2}') \
  && [ -n \"\$PID\" ] && kill \$PID && echo \"Stopped PID: \$PID\" \
  && nohup java -jar <jarName> --server.port=<port> > app.log 2>&1 & \
  && sleep 8 && curl -s -o /dev/null -w 'Backend: HTTP %{http_code}\n' http://localhost:<port>/"
```

### 模式 4：仅重启

不编译、不上传，直接 SSH 远程重启后端服务。

```bash
ssh <user>@<ip> "cd <backend.remoteDir> \
  && PID=\$(ps aux | grep '<jarName>' | grep -v grep | awk '{print \$2}') \
  && [ -n \"\$PID\" ] && kill \$PID && echo \"Stopped PID: \$PID\" || echo 'No running process' \
  && nohup java -jar <jarName> --server.port=<port> > app.log 2>&1 & \
  && sleep 8 && curl -s -o /dev/null -w 'Backend: HTTP %{http_code}\n' http://localhost:<port>/"
```

## ry.sh 参考

部署包中可附带 `ry.sh` 作为服务管理脚本（见项目根目录），支持 `start|stop|restart|status` 四个命令。完整部署时可将此脚本与 jar 包一起上传到 `backend.remoteDir`。

## Nginx 配置模板

部署时确保服务器 Nginx 包含以下配置（配置内容写入 `<nginx.configPath>`）：

```nginx
server {
    listen 80;
    server_name _;
    root <nginx.rootPath>;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location <nginx.apiProxyPath> {
        proxy_pass <nginx.apiProxyTarget>;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 日常运维

### 查看后端日志

```bash
ssh <user>@<ip> "tail -100 <backend.remoteDir>app.log"
```

### 查看后端状态

```bash
ssh <user>@<ip> "ps aux | grep '<jarName>' | grep -v grep"
```

### 查看 Nginx 状态

```bash
ssh <user>@<ip> "nginx -t && systemctl status nginx"
```

## 部署后总结

部署完成后，清晰汇报：

1. 部署模式 + 目标服务器
2. 前端/后端访问 URL 和 HTTP 状态码
3. 如有失败，说明具体错误和排查方向
