#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
zhihuiyuanqu-deploy 通用部署脚本（paramiko）

职责边界：纯上传 + 远端执行（本地构建在脚本外完成，产物存在才可部署）。
读项目根 deploy.json 驱动，支持全量/前端/后端/重启四种模式，跨若依项目通用。

用法：
    python deploy.py --mode full|frontend|backend|restart [--env production] [--no-verify]
    python deploy.py --mode frontend            # 仅前端: 上传 dist → nginx reload
    python deploy.py --mode backend             # 仅后端: 上传 jar → startCmd 重启
    python deploy.py --mode restart             # 仅重启: 远端执行 startCmd
    python deploy.py --check                    # 只验连通性 + 本地产物存在性

约束（对齐 CLAUDE.md 部署规范）：
    1. Windows Git Bash 密码认证不可用 → 全部用 paramiko，禁止 ssh/scp 命令行。
    2. 后端启动必须走 deploy.json backend.startCmd（本项目 = start-topo.sh）。
    3. paramiko 后台进程坑: exec_command 执行 nohup 后 java 持有 SSH 通道 fd，
       stdout.read() 会阻塞超时——这是正常现象，用读超时保护 + 独立轮询验证。
"""
import argparse
import json
import os
import socket
import sys
import tarfile
import tempfile
import time

try:
    # Windows 控制台默认 GBK，重配置为 utf-8 避免中文/符号打印崩（CLAUDE.md 已知坑）
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

try:
    import paramiko
except ImportError:
    print("缺少依赖 paramiko，请先执行: pip install paramiko")
    sys.exit(1)


def log(msg):
    print("[deploy] %s" % msg, flush=True)


def die(msg):
    print("[deploy] 错误: %s" % msg, file=sys.stderr, flush=True)
    sys.exit(1)


def load_config(project_root):
    """读取 deploy.json，返回 (servers, default_env)。"""
    cfg_path = os.path.join(project_root, "deploy.json")
    if not os.path.isfile(cfg_path):
        die("未找到 %s —— 请确认在项目根目录或通过 --project 指定" % cfg_path)
    with open(cfg_path, encoding="utf-8") as f:
        cfg = json.load(f)
    servers = cfg.get("servers", {})
    if not servers:
        die("deploy.json 缺少 servers 配置")
    return cfg, cfg.get("default", next(iter(servers)))


def resolve_project_root(script_dir, argv_project):
    """定位项目根：优先 --project，否则从脚本目录向上找含 deploy.json 的祖先。"""
    if argv_project:
        root = os.path.abspath(argv_project)
        if not os.path.isfile(os.path.join(root, "deploy.json")):
            die("--project 指向的目录下没有 deploy.json: %s" % root)
        return root
    cur = os.path.abspath(script_dir)
    for _ in range(6):  # scripts -> skill -> .claude -> 项目根，够深即停
        if os.path.isfile(os.path.join(cur, "deploy.json")):
            return cur
        parent = os.path.dirname(cur)
        if parent == cur:
            break
        cur = parent
    die("从脚本位置向上未找到 deploy.json，请用 --project <项目根> 指定")


class Remote:
    """paramiko 远端封装：连接 / 执行 / 上传，内置后台进程读超时保护。"""

    def __init__(self, ip, user, password):
        self.ip = ip
        self.client = paramiko.SSHClient()
        self.client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
        self.client.connect(ip, username=user, password=password, timeout=20)

    def close(self):
        try:
            self.client.close()
        except Exception:
            pass

    def exec(self, cmd, timeout=120):
        """执行命令并读完整输出。带读超时保护——后台 nohup 进程持有通道 fd 时
        read 会阻塞到 timeout，属正常（进程已启动），仅打超时标记不视为失败。"""
        try:
            stdin, stdout, stderr = self.client.exec_command(cmd, timeout=timeout)
            out = stdout.read().decode("utf-8", "replace")
            err = stderr.read().decode("utf-8", "replace")
            code = stdout.channel.recv_exit_status()
            return code, out, err
        except socket.timeout:
            return -1, "", "READ_TIMEOUT(后台进程已接管，属预期)"

    def upload(self, local_path, remote_path):
        sftp = self.client.open_sftp()
        try:
            sftp.put(local_path, remote_path)
        finally:
            sftp.close()


def ssh(remote, cmd, timeout=120, label=""):
    """执行 + 打印，非 0 退出码 raise。"""
    code, out, err = remote.exec(cmd, timeout=timeout)
    if out.strip():
        print(out.strip()[-1500:], flush=True)
    if err.strip() and "READ_TIMEOUT" not in err:
        print("STDERR> " + err.strip()[-800:], flush=True)
    if code not in (0, -1):  # -1 为超时(后台进程)不算失败
        die("%s 远端命令失败(code=%s): %s" % (label, code, cmd[:200]))


def pack_dist(dist_dir):
    """将 dist 目录打成 tar.gz 返回临时文件路径（python tarfile 跨平台）。"""
    fd, tmp_path = tempfile.mkstemp(suffix=".tar.gz", prefix="frontend-dist-")
    os.close(fd)
    with tarfile.open(tmp_path, "w:gz") as tar:
        tar.add(dist_dir, arcname="")
    log("前端产物已打包 %s (%.1f KB)" % (tmp_path, os.path.getsize(tmp_path) / 1024))
    return tmp_path


def deploy_frontend(remote, cfg, env_cfg, project_root):
    """上传本地 dist 到服务器并解压到 nginxDir + reload。"""
    fe = env_cfg.get("frontend", {})
    build_dir = fe.get("buildDir")
    if not build_dir:
        die("deploy.json frontend 缺少 buildDir")
    dist_dir = os.path.join(project_root, build_dir, "dist")
    if not os.path.isdir(dist_dir):
        die("本地前端产物不存在: %s —— 请先执行 %s 构建" % (dist_dir, fe.get("buildCmd")))
    nginx_dir = fe.get("nginxDir") or die("deploy.json frontend 缺少 nginxDir")
    remote_dir = fe.get("remoteDir") or "/tmp"
    tar_remote = os.path.join(remote_dir, "frontend-dist.tar.gz")

    local_tar = pack_dist(dist_dir)
    try:
        log("上传前端包 → %s" % tar_remote)
        remote.upload(local_tar, tar_remote)
    finally:
        os.unlink(local_tar)

    # 备份旧 dist → 清空 → 解压新包 → nginx reload（幂等覆盖）
    cmd = (
        "bak=%s.bak && rm -rf $bak && [ -d %s ] && cp -r %s $bak || echo 'no old dist'"
        % (nginx_dir.rstrip("/"), nginx_dir, nginx_dir)
    )
    ssh(remote, cmd, label="备份旧dist")
    ssh(
        remote,
        "rm -rf %s && mkdir -p %s && tar -xzf %s -C %s && nginx -t && nginx -s reload"
        % (nginx_dir, nginx_dir, tar_remote, nginx_dir),
        timeout=120,
        label="解压+reload",
    )
    log("前端部署完成 → %s" % nginx_dir)


def deploy_backend(remote, cfg, env_cfg, project_root, verify=True):
    """上传本地 jar → 备份旧 jar → startCmd 重启 → 轮询验证。"""
    be = env_cfg.get("backend", {})
    build_output = be.get("buildOutput")
    jar_name = be.get("jarName") or "ruoyi-admin.jar"
    remote_dir = be.get("remoteDir") or die("deploy.json backend 缺少 remoteDir")
    port = be.get("port") or 8090

    if not build_output or not os.path.isfile(os.path.join(project_root, build_output)):
        die("本地后端产物不存在: %s —— 请先执行 %s 构建" % (build_output, be.get("buildCmd")))
    local_jar = os.path.join(project_root, build_output)
    remote_jar = os.path.join(remote_dir, jar_name)

    # 备份远端旧 jar（保留 .bak 便于回滚），再上传新 jar
    ssh(remote, "cp -f %s %s.bak 2>/dev/null || echo 'no old jar'" % (remote_jar, remote_jar), label="备份旧jar")
    log("上传后端 jar → %s" % remote_jar)
    remote.upload(local_jar, remote_jar)

    start_cmd = be.get("startCmd") or (
        "cd %s && setsid nohup java -jar %s --server.port=%s </dev/null > app.log 2>&1 &"
        % (remote_dir, jar_name, port)
    )
    if not start_cmd.startswith("cd"):
        start_cmd = "cd %s && %s" % (remote_dir, start_cmd)

    log("执行远端启动命令: %s" % start_cmd[:150])
    code, out, err = remote.exec(start_cmd, timeout=10)
    if "READ_TIMEOUT" not in err:
        if out.strip():
            print(out.strip()[-500:], flush=True)
        if err.strip():
            print("STDERR> " + err.strip()[-500:], flush=True)
    if code not in (0, -1):
        die("远端启动命令返回异常(code=%s)" % code)

    if verify:
        verify_backend(remote, remote_dir, port, jar_name)


def verify_backend(remote, remote_dir, port, jar_name):
    """轮询后端 HTTP 与进程，最多 60s。"""
    log("等待后端启动 (port=%s)..." % port)
    for i in range(30):
        time.sleep(2)
        code, out, _ = remote.exec("curl -s -o /dev/null -w '%%{http_code}' http://localhost:%s/" % port, timeout=10)
        if out.strip() not in ("", "000"):
            code, pids, _ = remote.exec("ps aux | grep %s | grep -v grep | awk '{print $2}'" % jar_name, timeout=10)
            log("后端 HTTP %s  PID=%s (第%d次探测)" % (out.strip(), pids.strip().split()[0] if pids.strip() else "?", i + 1))
            return
    code, logtail, _ = remote.exec("tail -30 %s/app.log 2>/dev/null || echo 'no log'" % remote_dir, timeout=10)
    die("后端 %ds 内未就绪，app.log 尾部:\n%s" % (60, logtail[-1200:]))


def main():
    ap = argparse.ArgumentParser(description="zhihuiyuanqu-deploy 通用 paramiko 部署脚本")
    ap.add_argument("--mode", choices=["full", "frontend", "backend", "restart"], default="full")
    ap.add_argument("--env", default=None, help="目标环境(读 deploy.json servers 键)，默认取 default")
    ap.add_argument("--project", default=None, help="项目根目录(含 deploy.json)，默认自动向上查找")
    ap.add_argument("--no-verify", action="store_true", help="部署后跳过 HTTP/进程轮询验证")
    ap.add_argument("--check", action="store_true", help="只验连通性 + 本地产物存在性，不做任何部署")
    args = ap.parse_args()

    project_root = resolve_project_root(os.path.dirname(os.path.abspath(__file__)), args.project)
    cfg, default_env = load_config(project_root)
    env_name = args.env or default_env
    env_cfg = cfg["servers"].get(env_name)
    if not env_cfg:
        die("deploy.json 无环境 %s（可选: %s）" % (env_name, ", ".join(cfg["servers"].keys())))
    if not env_cfg.get("ip"):
        die("环境 %s 未配置 ip（跳过空模板）" % env_name)
    ip = env_cfg["ip"]; user = env_cfg.get("user") or "root"; pwd = env_cfg["password"]
    log("目标服务器 %s@%s (env=%s, mode=%s)" % (user, ip, env_name, args.mode))

    if args.check:
        try:
            remote = Remote(ip, user, pwd)
        except Exception as e:
            die("SSH 连接失败: %s" % e)
        fe = env_cfg.get("frontend", {})
        be = env_cfg.get("backend", {})
        dist = os.path.join(project_root, fe.get("buildDir", ""), "dist")
        jar = os.path.join(project_root, be.get("buildOutput", ""))
        log("SSH 连接成功 %s@%s" % (user, ip))
        log("本地 dist: %s | jar: %s" % ("OK" if os.path.isdir(dist) else "缺失", "OK" if os.path.isfile(jar) else "缺失"))
        port = be.get("port") or 8090
        probe = ("curl -s -o /dev/null -w 'backend=%{http_code}' http://localhost:"
                 + str(port) + "/ && echo ' ' && curl -s -o /dev/null -w 'frontend=%{http_code}' http://localhost:80/")
        code, out, _ = remote.exec(probe, timeout=15)
        log("远端探活: %s" % out.strip())
        remote.close()
        return

    remote = Remote(ip, user, pwd)
    try:
        if args.mode in ("full", "frontend"):
            deploy_frontend(remote, cfg, env_cfg, project_root)
        if args.mode in ("full", "backend"):
            deploy_backend(remote, cfg, env_cfg, project_root, verify=not args.no_verify)
        if args.mode == "restart":
            be = env_cfg.get("backend", {})
            remote_dir = be.get("remoteDir") or die("deploy.json backend 缺少 remoteDir")
            start_cmd = be.get("startCmd") or "bash ry.sh restart"
            if not start_cmd.startswith("cd"):
                start_cmd = "cd %s && %s" % (remote_dir, start_cmd)
            log("执行重启: %s" % start_cmd[:150])
            code, out, err = remote.exec(start_cmd, timeout=10)
            if "READ_TIMEOUT" not in err and err.strip():
                print("STDERR> " + err.strip()[-500:], flush=True)
            if code not in (0, -1):
                die("重启命令返回异常(code=%s)" % code)
            if not args.no_verify:
                verify_backend(remote, remote_dir, be.get("port") or 8090, be.get("jarName") or "ruoyi-admin.jar")
    finally:
        remote.close()
    log("部署完成 ✅")


if __name__ == "__main__":
    main()
