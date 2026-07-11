#!/bin/bash
# Categraf + Nginx 部署脚本 — 在 192.168.1.60 上执行
# Prometheus 已部署在 192.168.1.60:9090

set -e

echo "=== 1. 安装 Categraf ==="
CATEGRAF_VERSION="0.3.45"
cd /tmp

# 尝试 Docker 部署
if command -v docker &>/dev/null; then
  echo "Docker 可用，尝试 Docker 部署..."
  if docker run --rm -d --name categraf \
    -v /opt/categraf/conf:/opt/categraf/conf \
    -v /opt/categraf/logs:/opt/categraf/logs \
    --network host \
    flashcatcloud/categraf:latest 2>/dev/null; then
    echo "Categraf Docker 部署成功"
    exit 0
  else
    echo "Docker 部署失败，改用二进制部署"
    docker rm -f categraf 2>/dev/null || true
  fi
fi

# 二进制部署
echo "使用二进制部署 Categraf..."
wget -q "https://download.flashcat.cloud/categraf-v${CATEGRAF_VERSION}-linux-amd64.tar.gz" -O categraf.tar.gz 2>/dev/null || {
  echo "下载失败，请手动下载 categraf 到 /opt/categraf/"
  exit 1
}
tar -xzf categraf.tar.gz -C /opt/
mkdir -p /opt/categraf/conf/input.snmp/bak
mkdir -p /opt/categraf/logs

# 配置 Categraf Remote Write 到 Prometheus
cat > /opt/categraf/conf/config.toml << 'EOF'
[global]
hostname = "192.168.1.60"
interval = 15

[[writers]]
url = "http://192.168.1.60:9090/api/v1/write"
timeout = 10

[writers.dial_timeout]
duration = "3s"

[log]
file_name = "/opt/categraf/logs/categraf.log"
level = "INFO"
EOF

# systemd 服务
cat > /etc/systemd/system/categraf.service << 'EOF'
[Unit]
Description=Categraf SNMP Collector
After=network.target

[Service]
Type=simple
ExecStart=/opt/categraf/categraf -configs /opt/categraf/conf
ExecReload=/bin/kill -HUP $MAINPID
Restart=always
WorkingDirectory=/opt/categraf

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable categraf
systemctl start categraf

echo "=== 2. 配置 Prometheus Remote Write ==="
# 确认 Prometheus 已启用 remote write receiver
if ! grep -q "web.enable-remote-write-receiver" /etc/systemd/system/prometheus.service 2>/dev/null; then
  echo "⚠ Prometheus 需要添加 --web.enable-remote-write-receiver 参数"
fi

echo "=== 3. 配置 Nginx ==="
# Nginx /profile/ 静态文件服务（防止上传后无法预览问题）
if ! grep -q "location /profile/" /etc/nginx/conf.d/zhihuiyuanqu.conf 2>/dev/null; then
  sed -i '/location \/ws\//i\    # 上传文件静态资源\n    location /profile/ {\n        alias /home/ruoyi/uploadPath/;\n        expires 7d;\n    }\n' /etc/nginx/conf.d/zhihuiyuanqu.conf
fi

# Grafana 代理
if ! grep -q "location /grafana/" /etc/nginx/conf.d/zhihuiyuanqu.conf 2>/dev/null; then
  sed -i '/location \//i\    # Grafana 代理\n    location /grafana/ {\n        proxy_pass http://127.0.0.1:3000/;\n        proxy_set_header Host \$host;\n    }\n' /etc/nginx/conf.d/zhihuiyuanqu.conf
fi

nginx -t && nginx -s reload

echo "=== Categraf 部署完成 ==="
echo "Categraf 状态: $(systemctl is-active categraf)"
echo "配置文件目录: /opt/categraf/conf/input.snmp/"
echo "Prometheus Remote Write: http://192.168.1.60:9090/api/v1/write"
echo "Nginx 已配置 /profile/ 和 /grafana/ 代理"
