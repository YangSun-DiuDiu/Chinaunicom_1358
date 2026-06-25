#!/bin/bash
# 智慧园区管理系统 — 一键部署脚本
set -e

echo "=== 1. 停止旧服务 ==="
PID=$(ps aux | grep "ruoyi-admin.jar" | grep -v grep | awk '{print $2}')
[ -n "$PID" ] && kill $PID && echo "Stopped PID: $PID" || echo "No running service"

echo ""
echo "=== 2. 部署前端文件 ==="
mkdir -p /usr/share/nginx/zhihuiyuanqu
cp -r dist/* /usr/share/nginx/zhihuiyuanqu/dist/
echo "Frontend files copied"

echo ""
echo "=== 3. 配置 Nginx ==="
cp nginx.conf /etc/nginx/conf.d/zhihuiyuanqu.conf
nginx -t && nginx -s reload && echo "Nginx reloaded" || echo "Nginx config error!"

echo ""
echo "=== 4. 启动后端服务 ==="
chmod +x start-server.sh stop-server.sh
bash start-server.sh
sleep 5
echo "Checking backend..."
curl -s -o /dev/null -w "HTTP %{http_code}" http://localhost:8090/ && echo " — Backend OK" || echo "Backend NOT responding"

echo ""
echo "=== 部署完成 ==="
echo "前端: http://$(hostname -I | awk '{print $1}'):3001"
echo "后端: http://$(hostname -I | awk '{print $1}'):8090"
