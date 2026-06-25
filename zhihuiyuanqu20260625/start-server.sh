#!/bin/bash
# 启动后端服务 (端口 8090)
cd "$(dirname "$0")"
nohup java -jar ruoyi-admin.jar --server.port=8090 --spring.devtools.restart.enabled=false > app.log 2>&1 &
echo "Backend PID: $!"
echo "Backend started on port 8090"
