#!/bin/bash
# ========================================
#   智慧园区管理系统 - 服务端启动脚本
# ========================================

echo "========================================"
echo "  智慧园区管理系统 - 服务端启动脚本"
echo "========================================"

# 检查Java
if ! command -v java &> /dev/null; then
    echo "[错误] 未找到Java运行环境，请安装JDK 17+"
    exit 1
fi

# JVM参数
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+HeapDumpOnOutOfMemoryError"

# 查找JAR
JAR_FILE="./ruoyi-admin.jar"
if [ ! -f "$JAR_FILE" ]; then
    echo "[错误] 未找到 $JAR_FILE"
    exit 1
fi

echo "[信息] 启动应用: $JAR_FILE"
echo "[信息] JVM参数: $JAVA_OPTS"

# 后台启动
nohup java $JAVA_OPTS -jar $JAR_FILE --server.port=8080 > app.log 2>&1 &

echo "[信息] 应用已启动，PID: $!"
echo "[信息] 日志文件: app.log"
echo "[信息] 查看日志: tail -f app.log"
