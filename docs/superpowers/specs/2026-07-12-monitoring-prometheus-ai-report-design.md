# 监控体系改造 — 设备管理Prometheus融合 + AI网络运行报告 设计文档

- 日期：2026-07-12
- 范围：两个子系统一起做
- 决策记录：迁移模式A(彻底) / 采集器A(统一Categraf) / 可视化(设备列表跳转+统一入口) / 认证C(暂不管) / 模型C(硬编码application.yml)

## 基础设施

| 组件 | 地址 |
|------|------|
| Grafana | http://115.159.194.149:3000 |
| Prometheus | http://115.159.194.149:9090 |
| AlertManager | 待部署 @115.159.194.149 |
| DeepSeek API | https://api.deepseek.com/anthropic (key见application.yml) 模型deepseek-v4-flash |

---

## 子系统 A：设备管理 Prometheus/Grafana 融合

### 架构

```
Categraf(SNMP插件+Ping插件) → Prometheus(TSDB) → AlertManager(告警规则)
                                                        ↓ Webhook POST
                                          Java /device/alert/webhook
                                                        ↓ 复用
                                          updateDeviceStatus() → 短信 + 维修工单
前端设备列表[监控按钮] ─ window.open ─→ Grafana单设备Dashboard(var-instance={ip})
前端[监控大盘入口]     ─ window.open ─→ Grafana总览Dashboard
```

### 改动清单

| # | 组件 | 动作 |
|---|------|------|
| A1 | Categraf | 每设备TOML加 ping 输入插件配置（存活探测），复用 SnmpConfigServiceImpl 生成逻辑 |
| A2 | AlertManager | 部署 + 告警规则 `up==0 for 3m` → Webhook |
| A3 | Java Webhook | 新增 `AlertWebhookController.POST /device/alert/webhook`：解析AlertManager payload → 提取device_ip → 调 updateDeviceStatus(OFFLINE/ONLINE) |
| A4 | DeviceHeartbeatTask | **删除** heartbeatCheck() 的 iot_device Ping 分支（改由AlertManager驱动）；其余3类设备(person/vehicle/video)暂保留或一并迁移 |
| A5 | SnmpService(1000行) | 保留，仅供前端「MIB查询」手动功能 |
| A6 | 前端 device/list/index.vue | 操作列加「监控」按钮 → `window.open('http://115.159.194.149:3000/d/{dashUid}?var-instance='+ip)` |
| A7 | 前端 统一入口 | 设备管理菜单加「监控大盘」项（外链路由）→ Grafana总览 |
| A8 | Grafana | 建2类Dashboard：总览(全设备) + 单设备模板(instance变量) |

### Webhook payload 契约

AlertManager 发送格式：
```json
{
  "alerts": [{
    "status": "firing|resolved",
    "labels": { "instance": "192.168.1.10", "alertname": "DeviceDown" }
  }]
}
```
Java 提取 `labels.instance` → 匹配 iot_device.ip_address → firing=OFFLINE, resolved=ONLINE。

### Webhook 白名单

`/device/alert/webhook` 需加入 SecurityConfig.permitAll()（AlertManager无token）+ IP白名单校验(仅115.159.194.149)。

---

## 子系统 B：AI 网络运行报告

### 架构

```
Quartz定时(6周期) → NetReportService
  → PrometheusQueryService 聚合查询(在线率/端口UP率/带宽利用率/故障次数)
  → HealthScoreCalculator 三维加权评分(纯Java)
  → DeepSeekClient 组装Prompt+调用大模型
  → 生成Markdown → 存 net_report 表
  → 前端「运行报告」页 列表+详情+下载
```

### 健康评分算法（三维加权，纯Java可测）

| 维度 | 权重 | 计算公式 |
|------|------|---------|
| 设备健康 deviceScore | 40% | 在线率×0.6 + (1 - min(故障次数/设备数,1))×0.4 |
| 线路健康 lineScore | 30% | 端口UP率×0.5 + (1-平均丢包率)×0.5 |
| 流量健康 flowScore | 30% | (1-带宽超阈值占比)×0.7 + (1-流量抖动系数)×0.3 |

总分 = deviceScore×0.4 + lineScore×0.3 + flowScore×0.3，范围0-100。
分档：优≥90 / 良75-89 / 中60-74 / 差<60。

各维度指标从 Prometheus 查询：
- 在线率：`avg(up)`
- 端口UP率：`avg(ifOperStatus==1)`
- 带宽利用率：`rate(ifHCInOctets[5m])*8 / ifHighSpeed`
- 故障次数：查 iot_device_status_log OFFLINE 计数

### 报告周期

| 周期 | Quartz cron | 数据窗口 |
|------|-------------|---------|
| 日报 DAILY | 3 8 * * * | 24h |
| 周报 WEEKLY | 3 8 * * 1 | 7d |
| 月报 MONTHLY | 3 8 1 * * | 30d |
| 季度报 QUARTERLY | 3 8 1 1,4,7,10 * | 90d |
| 半年报 HALF_YEAR | 3 8 1 1,7 * | 180d |
| 年报 YEARLY | 3 8 1 1 * | 365d |

> cron分钟用3避开整点，符合调度分散原则。

### DeepSeek 调用

- application.yml 配置：
```yaml
ai:
  deepseek:
    base-url: https://api.deepseek.com/anthropic
    api-key: sk-ae962dffc574492fb81bf9378f200437
    model: deepseek-v4-flash
    timeout: 60000
```
- DeepSeekClient：HttpClient POST，Anthropic 兼容协议（messages格式），组装含指标数据的system+user prompt，返回Markdown正文。
- Prompt模板：注入健康评分 + 关键指标 + TOP异常设备，要求输出结构化中文报告（概述/评分详情/异常清单/趋势/优化建议）。

### 数据表

```sql
CREATE TABLE net_report (
  report_id     BIGINT PRIMARY KEY AUTO_INCREMENT,
  report_type   VARCHAR(20) NOT NULL COMMENT 'DAILY/WEEKLY/MONTHLY/QUARTERLY/HALF_YEAR/YEARLY',
  period_start  DATETIME NOT NULL,
  period_end    DATETIME NOT NULL,
  health_score  DECIMAL(5,2),
  device_score  DECIMAL(5,2),
  line_score    DECIMAL(5,2),
  flow_score    DECIMAL(5,2),
  content_md    LONGTEXT COMMENT '大模型生成正文',
  raw_metrics   JSON COMMENT '原始指标快照',
  create_by     VARCHAR(64) DEFAULT 'system',
  create_time   DATETIME,
  del_flag      CHAR(1) DEFAULT '0'
) COMMENT='网络运行报告';
```

### 后端结构（遵循若依分层）

```
com.ruoyi.web.controller.report.NetReportController   (/report)
com.ruoyi.system.domain.NetReport
com.ruoyi.system.mapper.NetReportMapper (+XML)
com.ruoyi.system.service.INetReportService / impl.NetReportServiceImpl
com.ruoyi.system.service.impl.HealthScoreCalculator  (纯算法,单测)
com.ruoyi.system.service.impl.DeepSeekClient         (大模型调用)
com.ruoyi.quartz.task.NetReportTask                  (6个入口方法)
```

### 前端

`ruoyi-ui/src/views/report/index.vue`：
- 顶部周期Tab(日/周/月/季/半年/年)
- 报告列表(时间/健康分/评级)
- 点击查看 → Markdown渲染详情
- 下载按钮(导出MD)

菜单：设备管理下加「运行报告」，权限 `report:net:list`。

---

## Skill 更新

新建/更新 Skill 记录基础设施信息：
- `zhihuiyuanqu-deploy`：补充 Grafana/Prometheus/AlertManager 地址
- 新建 `zhihuiyuanqu-monitoring`：记录监控体系架构、DeepSeek配置、健康评分算法，供后续维护

## 实施顺序

```
阶段A(设备管理):
  A1 net表无 → A3 Webhook接口 → A4 删心跳 → A6/A7 前端跳转 → A2/A8 运维(Grafana+AlertManager)
阶段B(AI报告):
  B1 net_report表 → B2 HealthScoreCalculator(TDD) → B3 DeepSeekClient → B4 6周期Task → B5 前端报告页
```

## 不改动

- 维修工单/短信业务逻辑(复用现有)
- SnmpConfigServiceImpl 的 TOML 生成核心
- 权限体系

## 风险点

1. AlertManager Webhook 无认证 → 必须IP白名单
2. DeepSeek API Key 明文在 application.yml → 生产环境建议后续移环境变量
3. 删除 Java 心跳后，Prometheus/AlertManager 若宕机则无告警 → 需监控 AlertManager 自身存活
4. 大模型调用失败 → 报告降级为"纯数据模板"(不含AI分析)，不阻塞
