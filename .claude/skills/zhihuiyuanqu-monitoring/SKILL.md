---
name: zhihuiyuanqu-monitoring
description: |
  智慧园区监控体系 Skill。触发词：监控体系、Prometheus、Grafana、AlertManager、Categraf、网络运行报告、健康评分、AI报告、DeepSeek。
  记录监控基础设施地址、DeepSeek大模型配置、健康评分算法，供设备监控改造与AI报告功能维护。
---

# 监控体系 Skill

## 基础设施地址

| 组件 | 地址 | 用途 |
|------|------|------|
| Grafana | http://115.159.194.149:3000 | 监控看板可视化 |
| Prometheus | http://115.159.194.149:9090 | 时序指标存储 |
| AlertManager | http://115.159.194.149:9093 (待部署) | 告警规则→Webhook |
| Categraf | 中心/各设备 | 统一采集器(SNMP插件+Ping插件) |

## 大模型配置（AI报告）

| 项 | 值 |
|----|-----|
| 提供商 | DeepSeek |
| baseUrl | https://api.deepseek.com/anthropic |
| API Key | sk-ae962dffc574492fb81bf9378f200437 |
| 模型名 | deepseek-v4-flash |
| 协议 | Anthropic 兼容(messages格式) |

application.yml 配置段：
```yaml
ai:
  deepseek:
    base-url: https://api.deepseek.com/anthropic
    api-key: sk-ae962dffc574492fb81bf9378f200437
    model: deepseek-v4-flash
    timeout: 60000
```

## 设备状态架构（方案A：Prometheus驱动）

```
Categraf(SNMP+Ping) → Prometheus → AlertManager(up==0 for 3m)
                                          ↓ Webhook POST /device/alert/webhook
                                   Java updateDeviceStatus() → 短信+维修工单
```

- Java Ping 心跳任务已删除，设备状态由 AlertManager 告警驱动
- Webhook 接口需 permitAll + IP白名单(仅115.159.194.149)
- SnmpService(纯Java 1000行)保留，仅供前端「MIB查询」手动功能

## Grafana 跳转

- 设备列表操作列「监控」按钮：`http://115.159.194.149:3000/d/{dashUid}?var-instance={ip}`
- 统一入口「监控大盘」：Grafana总览Dashboard
- 认证：暂不处理，链接直达

## 健康评分算法（三维加权，0-100）

| 维度 | 权重 | 公式 |
|------|------|------|
| 设备 deviceScore | 40% | 在线率×0.6 + (1-min(故障数/设备数,1))×0.4 |
| 线路 lineScore | 30% | 端口UP率×0.5 + (1-平均丢包率)×0.5 |
| 流量 flowScore | 30% | (1-带宽超阈值占比)×0.7 + (1-流量抖动)×0.3 |

总分 = deviceScore×0.4 + lineScore×0.3 + flowScore×0.3
分档：优≥90 / 良75-89 / 中60-74 / 差<60

Prometheus 查询：
- 在线率 `avg(up)`
- 端口UP率 `avg(ifOperStatus==1)`
- 带宽利用率 `rate(ifHCInOctets[5m])*8 / ifHighSpeed`

## 报告周期（Quartz cron）

| 周期 | cron | 窗口 |
|------|------|------|
| 日报 | 3 8 * * * | 24h |
| 周报 | 3 8 * * 1 | 7d |
| 月报 | 3 8 1 * * | 30d |
| 季度报 | 3 8 1 1,4,7,10 * | 90d |
| 半年报 | 3 8 1 1,7 * | 180d |
| 年报 | 3 8 1 1 * | 365d |

## net_report 表

report_id / report_type / period_start / period_end / health_score / device_score / line_score / flow_score / content_md / raw_metrics / create_time / del_flag

## 后端结构

```
controller.report.NetReportController        (/report)
system.domain.NetReport
system.mapper.NetReportMapper(+XML)
system.service.INetReportService/impl
system.service.impl.HealthScoreCalculator    (纯算法,单测)
system.service.impl.DeepSeekClient           (大模型调用)
quartz.task.NetReportTask                    (6周期入口)
web.controller.device.AlertWebhookController (/device/alert/webhook)
```

## 降级策略

- 大模型调用失败 → 报告降级为纯数据模板(无AI分析)，不阻塞
- AlertManager 宕机 → 需额外监控其存活
