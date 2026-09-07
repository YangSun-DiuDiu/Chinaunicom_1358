---
name: zhihuiyuanqu-deploy
description: |
  通用业务部署 Skill。当你需要部署、打包、发布、上线、更新前端/后端到服务器时使用。触发词包括但不限于：部署、打包、发布、上线、重新部署、更新服务器、编译打包、推到服务器、
  build and deploy、ship、release。无论用户说的是"部署一下"、"把前端更新到服务器"、"重启后端"还是"帮我把代码推上去"，都应该触发此 Skill。
  支持：全量部署、仅前端、仅后端、仅重启四种模式，支持多服务器配置。部署统一走内置 scripts/deploy.py（paramiko），禁止手写 scp/ssh。
---

# 通用业务部署 Skill

## 概述

将当前项目编译并部署到远程服务器，支持多种部署模式和服务器环境。

**部署架构**：

- 前端: Nginx → `/usr/share/nginx/<项目>/dist/`
- 后端: SpringBoot jar → `/root/<项目>/ruoyi-admin.jar`
- API代理: Nginx `/prod-api/*` → `http://127.0.0.1:<后端端口>/`

## ⚠️ 强制规范（必须先读）

1. **部署一律调用本 skill 内置 `scripts/deploy.py`**，不要现场手写 ssh/scp 命令、不要手写新的 paramiko 脚本。
   Windows Git Bash 下密码认证 `scp`/`ssh` 不可用，脚本已封装 paramiko 传输 + 远端执行 + 后台进程坑防护。
   调用：`python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode <模式> [--env <环境>]`
2. **后端启动/重启必须走服务器官方脚本 `start-topo.sh`**（deploy.json `backend.startCmd` 已配置），禁止裸 `nohup java`。
3. **本 skill 不负责本地编译**。deploy.py 只做「上传 + 远端执行 + 验证」，要求本地产物已存在。构建命令见 deploy.json 的 `buildCmd`。
4. **验证前置**：本地 mvn / npm 构建产物必须已生成（否则 deploy.py 会明确报错并提示先构建）。
5. 服务器连接统一 paramiko（CLAUDE.md「服务器连接规范」），密码来自 deploy.json，禁止硬编码。

## scripts/deploy.py 一键部署

本 skill 内置通用部署脚本（被 git 跟踪，随 skill 生效）：

```
位置: .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py
作用: 读项目根 deploy.json → 上传本地产物 → 远端备份/解压/重启 → HTTP+进程轮询验证
```

**常用调用**：

```bash
# 只验连通性 + 本地产物存在性（最安全，先跑它确认环境）
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --check

# 全量部署（= 前端 + 后端一起）
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode full

# 仅前端: 打包 ruoyi-ui/dist → 上传 → nginx reload
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode frontend

# 仅后端: 上传 jar → start-topo.sh 重启
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode backend

# 仅重启后端（不编译不上传）
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode restart

# 指定环境（多服务器时） / 部署后跳过自动验证
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode full --env production
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode full --no-verify
```

脚本会自动向上查找项目根 `deploy.json`（`scripts/../../../deploy.json`）；换项目部署时用 `--project <项目根>` 覆盖。
脚本输出关键步骤；后端启动为后台进程，脚本用「读超时保护 + 独立轮询」规避 paramiko 阻塞假死（见 CLAUDE.md 部署规范 ⚠️）。

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
      "backend": { "remoteDir": "...", "jarName": "...", "port": 8090, "buildCmd": "...", "buildOutput": "...", "startCmd": "..." },
      "nginx": { "configPath": "...", "apiProxyPath": "...", "apiProxyTarget": "...", "rootPath": "..." }
    }
  }
}
```

**配置项说明**：

- `frontend.remoteDir` — 前端 dist 文件上传到服务器的临时路径
- `frontend.nginxDir` — Nginx 实际 serve 前端文件的路径（部署解压目标）
- `frontend.buildDir` — 前端子项目目录名（如 `ruoyi-ui`），其下 `dist/` 为本地产物
- `backend.remoteDir` — 后端 jar 包在服务器上的存放目录
- `backend.startCmd` — 后端启动命令（本项目 = `bash start-topo.sh`；缺省脚本回退 nohup）
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

**仅重启模式**：不编译、不上传，直接重启后端服务。适用于修改配置文件后重新加载。

## 四种部署模式（统一走 deploy.py）

> 每次部署先 `--check` 确认连通与产物，再按模式执行。产物缺失时先本地构建。

### 模式 1：全量部署

完整编译前后端 + 上传 + 重启。

**Step 1: 本地编译后端**

```bash
cd <项目根目录>
<backend.buildCmd>        # 例如 mvn clean package -DskipTests -q
```

验证：`<backend.buildOutput>`（如 `ruoyi-admin/target/ruoyi-admin.jar`）文件存在。

**Step 2: 本地编译前端**

```bash
cd <项目根目录>/<frontend.buildDir>   # 例如 ruoyi-ui
<frontend.buildCmd>                   # 例如 npm run build:prod
```

验证：`<frontend.buildDir>/dist/` 目录生成。

**Step 3: 调用 deploy.py 全量部署**

```bash
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode full
```

脚本会自动：打包 dist → 上传 jar+前端包 → 远端备份/解压 nginx + reload → start-topo.sh 重启后端 → HTTP/进程轮询验证。

### 模式 2：仅前端

只编译前端 + 上传 + Nginx reload。后端不动。

**Step 1: 本地编译前端**

```bash
cd <项目根目录>/<frontend.buildDir> && <frontend.buildCmd>
```

**Step 2: 调用 deploy.py 前端部署**

```bash
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode frontend
```

**Step 3: 验证**

```bash
curl -s -o /dev/null -w "Frontend: HTTP %{http_code}\n" --connect-timeout 5 http://<ip>:80/
```

### 模式 3：仅后端

只编译后端 + 上传 + 重启后端。前端不动。

**Step 1: 本地编译后端**

```bash
cd <项目根目录> && <backend.buildCmd>
```

**Step 2: 调用 deploy.py 后端部署**

```bash
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode backend
```

脚本自动：上传 jar → 备份旧 jar → `backend.startCmd`(start-topo.sh) 重启 → 轮询 HTTP 200 + PID。

### 模式 4：仅重启

不编译、不上传，直接重启后端服务。

```bash
python .claude/skills/zhihuiyuanqu-deploy/scripts/deploy.py --mode restart
```

脚本执行远端 `backend.startCmd`（start-topo.sh：先 pkill 旧进程再启动）并轮询验证。适用于改 yml 配置后重载、进程异常重启。

## ry.sh 参考

项目根目录自带 `ry.sh` 服务管理脚本（`start|stop|restart|status`），供服务器端手工管理 jar。deploy.py 通过 deploy.json `backend.startCmd` 控制重启方式；如无 start-topo.sh 环境，可改配 `"startCmd": "bash ry.sh restart"`。

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

> 以下连接/查看命令统一用 paramiko 方式执行（见 CLAUDE.md「服务器连接规范」模板），非 ssh 命令行。

### 查看后端日志

```bash
# paramiko 执行远端: tail -100 <backend.remoteDir>app.log
```

### 查看后端状态

```bash
# paramiko 执行远端: ps aux | grep '<jarName>' | grep -v grep
```

### 查看 Nginx 状态

```bash
# paramiko 执行远端: nginx -t && systemctl status nginx
```

## 部署后总结

部署完成后，清晰汇报：

1. 部署模式 + 目标服务器
2. 前端/后端访问 URL 和 HTTP 状态码
3. 如有失败，说明具体错误和排查方向（后端看 `<backend.remoteDir>app.log`，前端看 nginx `error.log`）
