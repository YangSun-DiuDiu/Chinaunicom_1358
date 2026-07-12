# AlertManager 部署指南（阶段27告警链路收尾）

在服务器 **115.159.194.149**（Prometheus/Grafana 所在机）上执行，打通设备离线告警链路。

## 架构回顾

```
Prometheus(9090) --评估 up==0--> AlertManager(9093) --Webhook--> Java(1.94.26.126:8090/device/alert/webhook)
                                                                        ↓
                                                          updateDeviceStatus → 短信 + 维修工单
```

---

## Step 1：下载安装 AlertManager

```bash
cd /opt
wget https://github.com/prometheus/alertmanager/releases/download/v0.27.0/alertmanager-0.27.0.linux-amd64.tar.gz
tar -xzf alertmanager-0.27.0.linux-amd64.tar.gz
mv alertmanager-0.27.0.linux-amd64 alertmanager
cd alertmanager
```

> 若服务器无法访问 github，可用国内镜像或手动上传 tar 包。

## Step 2：AlertManager 配置（webhook 指向 Java）

创建 `/opt/alertmanager/alertmanager.yml`：

```yaml
global:
  resolve_timeout: 5m

route:
  receiver: 'ruoyi-webhook'
  group_by: ['alertname', 'instance']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 1h

receivers:
  - name: 'ruoyi-webhook'
    webhook_configs:
      - url: 'http://1.94.26.126:8090/device/alert/webhook'
        send_resolved: true
```

## Step 3：启动 AlertManager

```bash
cd /opt/alertmanager
nohup ./alertmanager --config.file=alertmanager.yml --web.listen-address=:9093 > alertmanager.log 2>&1 &
sleep 3
curl -s http://localhost:9093/-/healthy && echo " AlertManager OK"
```

---

## Step 4：Prometheus 配 AlertManager 地址 + 告警规则

### 4.1 修改 Prometheus 主配置（通常 /etc/prometheus/prometheus.yml 或 /opt/prometheus/prometheus.yml）

在 `prometheus.yml` 中加入 alerting + rule_files 两段：

```yaml
# 指向 AlertManager
alerting:
  alertmanagers:
    - static_configs:
        - targets:
            - 'localhost:9093'    # AlertManager 与 Prometheus 同机

# 告警规则文件
rule_files:
  - '/opt/prometheus/rules/device.yml'
```

### 4.2 创建告警规则 /opt/prometheus/rules/device.yml

```yaml
groups:
  - name: device_alerts
    rules:
      - alert: DeviceDown
        expr: up == 0
        for: 3m
        labels:
          severity: critical
        annotations:
          summary: "设备 {{ $labels.instance }} 离线超过3分钟"
          description: "instance={{ $labels.instance }} job={{ $labels.job }}"
```

### 4.3 重载 Prometheus

```bash
# 方式1：热重载（若启用了 --web.enable-lifecycle）
curl -X POST http://localhost:9090/-/reload

# 方式2：重启进程
# systemctl restart prometheus  （视安装方式而定）
```

---

## Step 5：验证告警链路

### 5.1 确认 Prometheus 识别了告警规则

浏览器打开 `http://115.159.194.149:9090/alerts`，应看到 `DeviceDown` 规则（Inactive 状态代表正常）。

### 5.2 确认 AlertManager 已连接

浏览器打开 `http://115.159.194.149:9090/status` → Configuration → 确认 Alertmanagers 里有 `localhost:9093`。

### 5.3 端到端测试（模拟告警）

在 115.159.194.149 上直接给 AlertManager 推一条测试告警，验证它能回调 Java：

```bash
curl -XPOST http://localhost:9093/api/v2/alerts -H 'Content-Type: application/json' -d '[
  {
    "labels": {"alertname":"DeviceDown","instance":"192.168.1.10","severity":"critical"},
    "annotations": {"summary":"测试告警"}
  }
]'
```

然后在 Java 服务器(1.94.26.126)看日志：
```bash
ssh root@1.94.26.126 "grep -i 'AlertWebhook\|告警' /root/zhihuiyuanqu20260625/app.log | tail -5"
```
应看到 AlertWebhook 收到并处理（若该 IP 在 iot_device 表中存在则更新状态）。

---

## ⚠️ 网络前提（关键）

AlertManager(115.159.194.149) 必须能访问 Java(1.94.26.126:8090)：

```bash
# 在 115.159.194.149 上测试到 Java 的连通性
curl -s -o /dev/null -w '%{http_code}\n' --connect-timeout 8 http://1.94.26.126:8090/
```
- 返回非 000 = 可达，链路能打通
- 返回 000 = 不可达，需在 1.94.26.126 防火墙放行 8090 端口给 115.159.194.149，或改用内网地址

---

## 完整地址清单

| 配在哪 | 配置项 | 值 |
|--------|--------|-----|
| prometheus.yml | `alerting.alertmanagers.targets` | `localhost:9093` |
| prometheus.yml | `rule_files` | `/opt/prometheus/rules/device.yml` |
| alertmanager.yml | `webhook_configs.url` | `http://1.94.26.126:8090/device/alert/webhook` |

## 开机自启（可选，systemd）

`/etc/systemd/system/alertmanager.service`：
```ini
[Unit]
Description=AlertManager
After=network.target

[Service]
ExecStart=/opt/alertmanager/alertmanager --config.file=/opt/alertmanager/alertmanager.yml --web.listen-address=:9093
Restart=always

[Install]
WantedBy=multi-user.target
```
```bash
systemctl daemon-reload && systemctl enable --now alertmanager
```
