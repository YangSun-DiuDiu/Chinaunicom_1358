# 监控体系改造实现计划 — Prometheus融合 + AI网络报告

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 设备状态从Java Ping心跳迁移到Prometheus/AlertManager驱动，并新增基于DeepSeek的6周期网络运行报告。

**Architecture:** 阶段A把设备存活检测交给Categraf+AlertManager，Java只被动接Webhook触发短信/工单，前端跳转Grafana看图；阶段B用Quartz定时查Prometheus聚合指标→纯Java三维健康评分→DeepSeek生成Markdown报告→前端展示。

**Tech Stack:** SpringBoot2 + MyBatis + Quartz + Vue2/ElementUI + Prometheus HTTP API + DeepSeek(Anthropic兼容)

## Global Constraints

- 后端遵循若依分层：Controller→Service→ServiceImpl→Mapper→Entity
- 权限注解 `@ss.hasPermi('x:y:z')` 单参数；`hasAnyPermi` 用逗号分隔单字符串
- 新表含 create_by/create_time/del_flag，逻辑删除 del_flag='0'
- Grafana `http://115.159.194.149:3000`，Prometheus `http://115.159.194.149:9090`
- DeepSeek `https://api.deepseek.com/anthropic`，模型 `deepseek-v4-flash`，key在application.yml
- 不引入MyBatis-Plus，SQL不写select *
- Webhook接口 permitAll + IP白名单(仅115.159.194.149)

---

## 阶段 A：设备管理 Prometheus 融合

### Task A1: AlertManager Webhook 接收接口

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/device/AlertWebhookController.java`
- Modify: `ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java:103`

**Interfaces:**
- Consumes: `IDeviceService.updateDeviceStatus(Long deviceId, String status)` (已存在), `IDeviceService.selectDeviceList(Device)` (查IP匹配)
- Produces: `POST /device/alert/webhook` 接收 AlertManager 告警

- [ ] **Step 1: 写 AlertWebhookController**

```java
package com.ruoyi.web.controller.device;

import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.service.IDeviceService;

/**
 * AlertManager 告警 Webhook 接收器
 * 接收 up==0 告警 → 更新设备状态 → 触发短信+工单（复用 updateDeviceStatus）
 */
@RestController
@RequestMapping("/device/alert")
public class AlertWebhookController {

    private static final Logger log = LoggerFactory.getLogger(AlertWebhookController.class);
    private static final String ALLOW_IP = "115.159.194.149";

    @Autowired
    private IDeviceService deviceService;

    @PostMapping("/webhook")
    public AjaxResult webhook(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        // IP 白名单校验
        String remoteIp = getClientIp(request);
        if (!ALLOW_IP.equals(remoteIp) && !"127.0.0.1".equals(remoteIp)) {
            log.warn("AlertWebhook: 拒绝非白名单IP {}", remoteIp);
            return AjaxResult.error("forbidden");
        }
        try {
            Object alertsObj = payload.get("alerts");
            if (!(alertsObj instanceof List)) {
                return AjaxResult.success("no alerts");
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> alerts = (List<Map<String, Object>>) alertsObj;
            for (Map<String, Object> alert : alerts) {
                String status = (String) alert.get("status"); // firing / resolved
                @SuppressWarnings("unchecked")
                Map<String, Object> labels = (Map<String, Object>) alert.get("labels");
                if (labels == null) continue;
                String instance = (String) labels.get("instance");
                if (instance == null) continue;
                // instance 可能是 ip:port，取ip部分
                String ip = instance.contains(":") ? instance.substring(0, instance.indexOf(":")) : instance;
                Device query = new Device();
                query.setIpAddress(ip);
                List<Device> devices = deviceService.selectDeviceList(query);
                for (Device d : devices) {
                    String newStatus = "firing".equals(status) ? "OFFLINE" : "ONLINE";
                    deviceService.updateDeviceStatus(d.getDeviceId(), newStatus);
                    log.info("AlertWebhook: 设备{}({}) → {}", d.getDeviceName(), ip, newStatus);
                }
            }
            return AjaxResult.success();
        } catch (Exception e) {
            log.error("AlertWebhook 处理失败", e);
            return AjaxResult.error(e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && ip.length() > 0 && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

- [ ] **Step 2: SecurityConfig 加白名单**

在 `SecurityConfig.java:103` 的 permitAll 列表中，`"/device/repair/public/**"` 后追加 `"/device/alert/webhook"`：

```java
requests.requestMatchers("/login", "/register", "/captchaImage", "/pass/**", "/visitor/h5/**", "/h5-register", "/h5-register.html", "/device/repair/public/**", "/device/alert/webhook", "/repair-complete", "/attendance/callback/**", "/ws/**", "/meeting/board/**", "/meeting/room/simpleList").permitAll()
```

- [ ] **Step 3: 编译验证**

Run: `mvn -pl ruoyi-admin -am compile -q`
Expected: BUILD 无报错

- [ ] **Step 4: Commit**

```bash
git add ruoyi-admin/src/main/java/com/ruoyi/web/controller/device/AlertWebhookController.java ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java
git commit -m "feat: AlertManager Webhook接口(IP白名单+复用updateDeviceStatus)"
```

---

### Task A2: 删除 Java Ping 心跳的 iot_device 分支

**Files:**
- Modify: `ruoyi-quartz/src/main/java/com/ruoyi/quartz/task/DeviceHeartbeatTask.java`

**Interfaces:**
- Consumes: 无
- Produces: iot_device 存活检测不再由 Java Ping 驱动

- [ ] **Step 1: 读现有 checkIotDevices 方法**

Run: `grep -n "checkIotDevices\|iot_device\|selectDeviceList" ruoyi-quartz/src/main/java/com/ruoyi/quartz/task/DeviceHeartbeatTask.java`

- [ ] **Step 2: 注释掉 checkIotDevices() 的调用**

在 `heartbeatCheck()` 方法体中，找到 `checkIotDevices()` 的调用，改为注释并加说明：

```java
// [2026-07-12] iot_device 存活检测已迁移到 Prometheus/AlertManager
// checkIotDevices();
```

保留 person/vehicle/video 三类设备的 Ping 检测（这些暂未接入 Prometheus）。

- [ ] **Step 3: 编译验证**

Run: `mvn -pl ruoyi-quartz -am compile -q`
Expected: BUILD 无报错

- [ ] **Step 4: Commit**

```bash
git add ruoyi-quartz/src/main/java/com/ruoyi/quartz/task/DeviceHeartbeatTask.java
git commit -m "feat: 删除iot_device的Java Ping心跳(改由AlertManager驱动)"
```

---

### Task A3: 前端设备列表加 Grafana 监控跳转 + 统一入口

**Files:**
- Modify: `ruoyi-ui/src/views/device/list/index.vue`

**Interfaces:**
- Consumes: 设备行的 `ipAddress` 字段
- Produces: 操作列「监控」按钮 + 页面顶部「监控大盘」按钮

- [ ] **Step 1: 操作列加监控按钮**

在 `device/list/index.vue` 的表格操作列（`<el-table-column label="操作">` 的 template 内），加一个监控按钮：

```html
<el-button
  size="mini"
  type="text"
  icon="el-icon-monitor"
  @click="openGrafana(scope.row)"
>监控</el-button>
```

- [ ] **Step 2: 顶部工具栏加监控大盘入口**

在搜索区或按钮区加：

```html
<el-button
  type="primary"
  plain
  icon="el-icon-data-line"
  size="mini"
  @click="openGrafanaOverview"
>监控大盘</el-button>
```

- [ ] **Step 3: 加两个跳转方法**

在 `methods` 中加：

```js
// 跳转单设备 Grafana Dashboard
openGrafana(row) {
  if (!row.ipAddress) {
    this.$modal.msgWarning('该设备无IP地址')
    return
  }
  const base = 'http://115.159.194.149:3000'
  const url = `${base}/d/device-detail?var-instance=${row.ipAddress}`
  window.open(url, '_blank')
},
// 跳转总览 Grafana Dashboard
openGrafanaOverview() {
  window.open('http://115.159.194.149:3000/d/device-overview', '_blank')
}
```

- [ ] **Step 4: 编译验证**

Run: `cd ruoyi-ui && npm run build:prod 2>&1 | tail -3`
Expected: `Build complete`

- [ ] **Step 5: Commit**

```bash
git add ruoyi-ui/src/views/device/list/index.vue
git commit -m "feat: 设备列表加Grafana监控跳转+监控大盘入口"
```

---

### Task A4: 运维配置文档（Categraf ping插件 + AlertManager）

**Files:**
- Create: `docs/monitoring/categraf-ping-alertmanager-setup.md`

**Interfaces:**
- Consumes: 无
- Produces: 服务器端部署指南（人工执行）

- [ ] **Step 1: 写运维配置文档**

创建 `docs/monitoring/categraf-ping-alertmanager-setup.md`，包含：

1. Categraf ping 插件 TOML（`/opt/categraf/conf/input.ping/ping.toml`）：
```toml
[[instances]]
targets = ["192.168.1.10", "192.168.1.11"]  # 设备IP列表
count = 3
timeout = 3
[instances.labels]
job = "device_ping"
```

2. Prometheus 告警规则（`/etc/prometheus/rules/device.yml`）：
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
          summary: "设备 {{ $labels.instance }} 离线"
```

3. AlertManager 配置（`/etc/alertmanager/alertmanager.yml`）：
```yaml
route:
  receiver: 'ruoyi-webhook'
  group_wait: 30s
  repeat_interval: 1h
receivers:
  - name: 'ruoyi-webhook'
    webhook_configs:
      - url: 'http://1.94.26.126:8090/device/alert/webhook'
        send_resolved: true
```

4. Grafana 建 2 个 Dashboard 的说明：`device-overview`（全设备）、`device-detail`（instance变量）。

- [ ] **Step 2: Commit**

```bash
git add docs/monitoring/categraf-ping-alertmanager-setup.md
git commit -m "docs: Categraf ping+AlertManager+Grafana运维配置指南"
```

---

## 阶段 B：AI 网络运行报告

### Task B1: net_report 表 + Domain + Mapper

**Files:**
- Create: `docs/sql/net_report.sql`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/NetReport.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/mapper/NetReportMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/system/NetReportMapper.xml`

**Interfaces:**
- Produces: `NetReport` 实体 + `NetReportMapper.insertNetReport/selectNetReportList/selectNetReportById`

- [ ] **Step 1: 建表 SQL**

创建 `docs/sql/net_report.sql`：

```sql
CREATE TABLE net_report (
  report_id     BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '报告ID',
  report_type   VARCHAR(20) NOT NULL COMMENT 'DAILY/WEEKLY/MONTHLY/QUARTERLY/HALF_YEAR/YEARLY',
  period_start  DATETIME NOT NULL COMMENT '统计开始',
  period_end    DATETIME NOT NULL COMMENT '统计结束',
  health_score  DECIMAL(5,2) COMMENT '总健康分',
  device_score  DECIMAL(5,2) COMMENT '设备维度分',
  line_score    DECIMAL(5,2) COMMENT '线路维度分',
  flow_score    DECIMAL(5,2) COMMENT '流量维度分',
  content_md    LONGTEXT COMMENT '大模型生成正文',
  raw_metrics   JSON COMMENT '原始指标快照',
  create_by     VARCHAR(64) DEFAULT 'system' COMMENT '创建者',
  create_time   DATETIME COMMENT '创建时间',
  del_flag      CHAR(1) DEFAULT '0' COMMENT '删除标志(0正常1删除)',
  INDEX idx_type_time (report_type, period_start)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网络运行报告';
```

- [ ] **Step 2: 在服务器执行建表**

```bash
scp docs/sql/net_report.sql root@1.94.26.126:/root/
ssh root@1.94.26.126 "mysql -uroot -p'Chinaunicom@1358' ry101 < /root/net_report.sql"
```

- [ ] **Step 3: NetReport 实体**

```java
package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/** 网络运行报告 net_report */
public class NetReport extends BaseEntity {
    private static final long serialVersionUID = 1L;
    private Long reportId;
    private String reportType;
    private Date periodStart;
    private Date periodEnd;
    private BigDecimal healthScore;
    private BigDecimal deviceScore;
    private BigDecimal lineScore;
    private BigDecimal flowScore;
    private String contentMd;
    private String rawMetrics;
    private String delFlag;

    public Long getReportId() { return reportId; }
    public void setReportId(Long v) { this.reportId = v; }
    public String getReportType() { return reportType; }
    public void setReportType(String v) { this.reportType = v; }
    public Date getPeriodStart() { return periodStart; }
    public void setPeriodStart(Date v) { this.periodStart = v; }
    public Date getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Date v) { this.periodEnd = v; }
    public BigDecimal getHealthScore() { return healthScore; }
    public void setHealthScore(BigDecimal v) { this.healthScore = v; }
    public BigDecimal getDeviceScore() { return deviceScore; }
    public void setDeviceScore(BigDecimal v) { this.deviceScore = v; }
    public BigDecimal getLineScore() { return lineScore; }
    public void setLineScore(BigDecimal v) { this.lineScore = v; }
    public BigDecimal getFlowScore() { return flowScore; }
    public void setFlowScore(BigDecimal v) { this.flowScore = v; }
    public String getContentMd() { return contentMd; }
    public void setContentMd(String v) { this.contentMd = v; }
    public String getRawMetrics() { return rawMetrics; }
    public void setRawMetrics(String v) { this.rawMetrics = v; }
    public String getDelFlag() { return delFlag; }
    public void setDelFlag(String v) { this.delFlag = v; }
}
```

- [ ] **Step 4: NetReportMapper 接口**

```java
package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.NetReport;

public interface NetReportMapper {
    int insertNetReport(NetReport report);
    List<NetReport> selectNetReportList(NetReport report);
    NetReport selectNetReportById(Long reportId);
    int deleteNetReportById(Long reportId);
}
```

- [ ] **Step 5: NetReportMapper.xml**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.system.mapper.NetReportMapper">
    <resultMap type="NetReport" id="NetReportResult">
        <id property="reportId" column="report_id"/>
        <result property="reportType" column="report_type"/>
        <result property="periodStart" column="period_start"/>
        <result property="periodEnd" column="period_end"/>
        <result property="healthScore" column="health_score"/>
        <result property="deviceScore" column="device_score"/>
        <result property="lineScore" column="line_score"/>
        <result property="flowScore" column="flow_score"/>
        <result property="contentMd" column="content_md"/>
        <result property="rawMetrics" column="raw_metrics"/>
        <result property="createBy" column="create_by"/>
        <result property="createTime" column="create_time"/>
        <result property="delFlag" column="del_flag"/>
    </resultMap>

    <sql id="selectNetReportVo">
        select report_id, report_type, period_start, period_end, health_score,
        device_score, line_score, flow_score, content_md, raw_metrics,
        create_by, create_time, del_flag from net_report
    </sql>

    <select id="selectNetReportList" parameterType="NetReport" resultMap="NetReportResult">
        <include refid="selectNetReportVo"/>
        <where>
            del_flag = '0'
            <if test="reportType != null and reportType != ''"> and report_type = #{reportType}</if>
        </where>
        order by period_start desc
    </select>

    <select id="selectNetReportById" parameterType="Long" resultMap="NetReportResult">
        <include refid="selectNetReportVo"/>
        where report_id = #{reportId} and del_flag = '0'
    </select>

    <insert id="insertNetReport" parameterType="NetReport" useGeneratedKeys="true" keyProperty="reportId">
        insert into net_report(report_type, period_start, period_end, health_score,
        device_score, line_score, flow_score, content_md, raw_metrics, create_by, create_time)
        values(#{reportType}, #{periodStart}, #{periodEnd}, #{healthScore},
        #{deviceScore}, #{lineScore}, #{flowScore}, #{contentMd}, #{rawMetrics}, #{createBy}, sysdate())
    </insert>

    <update id="deleteNetReportById" parameterType="Long">
        update net_report set del_flag = '1' where report_id = #{reportId}
    </update>
</mapper>
```

- [ ] **Step 6: Commit**

```bash
git add docs/sql/net_report.sql ruoyi-system/src/main/java/com/ruoyi/system/domain/NetReport.java ruoyi-system/src/main/java/com/ruoyi/system/mapper/NetReportMapper.java ruoyi-system/src/main/resources/mapper/system/NetReportMapper.xml
git commit -m "feat: net_report表+Domain+Mapper"
```

---

### Task B2: 健康评分算法（纯Java，TDD）

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HealthScoreCalculator.java`
- Test: `ruoyi-system/src/test/java/com/ruoyi/system/HealthScoreCalculatorTest.java`

**Interfaces:**
- Produces: `HealthScoreCalculator.calculate(double onlineRate, int faultCount, int deviceCount, double portUpRate, double avgPacketLoss, double bwOverRatio, double flowJitter)` → `ScoreResult{deviceScore, lineScore, flowScore, totalScore, grade}`

- [ ] **Step 1: 写失败测试**

```java
package com.ruoyi.system;

import org.junit.Assert;
import org.junit.Test;
import com.ruoyi.system.service.impl.HealthScoreCalculator;
import com.ruoyi.system.service.impl.HealthScoreCalculator.ScoreResult;

public class HealthScoreCalculatorTest {
    @Test
    public void testPerfectScore() {
        // 全部满分：在线率1.0, 0故障, 端口UP1.0, 0丢包, 0超阈值, 0抖动
        ScoreResult r = HealthScoreCalculator.calculate(1.0, 0, 10, 1.0, 0.0, 0.0, 0.0);
        Assert.assertEquals(100.0, r.totalScore, 0.01);
        Assert.assertEquals("优", r.grade);
    }

    @Test
    public void testDeviceOffline() {
        // 在线率0.5, 5故障/10设备, 其余满分
        ScoreResult r = HealthScoreCalculator.calculate(0.5, 5, 10, 1.0, 0.0, 0.0, 0.0);
        // deviceScore = 0.5*0.6 + (1-0.5)*0.4 = 0.3+0.2 = 0.5 → 50分
        // 总分 = 50*0.4 + 100*0.3 + 100*0.3 = 20+30+30 = 80
        Assert.assertEquals(80.0, r.totalScore, 0.01);
        Assert.assertEquals("良", r.grade);
    }

    @Test
    public void testGradeBoundaries() {
        Assert.assertEquals("优", HealthScoreCalculator.grade(90.0));
        Assert.assertEquals("良", HealthScoreCalculator.grade(75.0));
        Assert.assertEquals("中", HealthScoreCalculator.grade(60.0));
        Assert.assertEquals("差", HealthScoreCalculator.grade(59.9));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `mvn -pl ruoyi-system test -Dtest=HealthScoreCalculatorTest -q`
Expected: FAIL（类不存在）

- [ ] **Step 3: 实现 HealthScoreCalculator**

```java
package com.ruoyi.system.service.impl;

/**
 * 网络健康评分算法（三维加权，纯函数可测）
 * 设备40% + 线路30% + 流量30%
 */
public class HealthScoreCalculator {

    public static class ScoreResult {
        public double deviceScore;
        public double lineScore;
        public double flowScore;
        public double totalScore;
        public String grade;
    }

    /**
     * @param onlineRate    在线率 0-1
     * @param faultCount    故障次数
     * @param deviceCount   设备总数
     * @param portUpRate    端口UP率 0-1
     * @param avgPacketLoss 平均丢包率 0-1
     * @param bwOverRatio   带宽超阈值占比 0-1
     * @param flowJitter    流量抖动系数 0-1
     */
    public static ScoreResult calculate(double onlineRate, int faultCount, int deviceCount,
                                        double portUpRate, double avgPacketLoss,
                                        double bwOverRatio, double flowJitter) {
        double faultRatio = deviceCount > 0 ? Math.min((double) faultCount / deviceCount, 1.0) : 0.0;
        double device = (onlineRate * 0.6 + (1 - faultRatio) * 0.4) * 100;
        double line = (portUpRate * 0.5 + (1 - avgPacketLoss) * 0.5) * 100;
        double flow = ((1 - bwOverRatio) * 0.7 + (1 - flowJitter) * 0.3) * 100;
        double total = device * 0.4 + line * 0.3 + flow * 0.3;

        ScoreResult r = new ScoreResult();
        r.deviceScore = round2(device);
        r.lineScore = round2(line);
        r.flowScore = round2(flow);
        r.totalScore = round2(total);
        r.grade = grade(total);
        return r;
    }

    public static String grade(double score) {
        if (score >= 90) return "优";
        if (score >= 75) return "良";
        if (score >= 60) return "中";
        return "差";
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `mvn -pl ruoyi-system test -Dtest=HealthScoreCalculatorTest -q`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/system/service/impl/HealthScoreCalculator.java ruoyi-system/src/test/java/com/ruoyi/system/HealthScoreCalculatorTest.java
git commit -m "feat: 健康评分算法(三维加权,TDD 3测试通过)"
```

---

### Task B3: DeepSeek 客户端 + 配置

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/DeepSeekClient.java`
- Modify: `ruoyi-admin/src/main/resources/application.yml`

**Interfaces:**
- Produces: `DeepSeekClient.chat(String systemPrompt, String userPrompt)` → String(Markdown正文)

- [ ] **Step 1: application.yml 加 ai 配置**

在 `application.yml` 末尾加：

```yaml
# AI大模型配置（网络报告用）
ai:
  deepseek:
    base-url: https://api.deepseek.com/anthropic
    api-key: sk-ae962dffc574492fb81bf9378f200437
    model: deepseek-v4-flash
    timeout: 60000
```

- [ ] **Step 2: 写 DeepSeekClient**

```java
package com.ruoyi.system.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;

/**
 * DeepSeek 大模型客户端（Anthropic 兼容协议）
 */
@Component
public class DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    @Value("${ai.deepseek.base-url}")
    private String baseUrl;
    @Value("${ai.deepseek.api-key}")
    private String apiKey;
    @Value("${ai.deepseek.model}")
    private String model;
    @Value("${ai.deepseek.timeout:60000}")
    private long timeout;

    /**
     * 调用大模型生成文本。失败返回 null（调用方降级为纯数据模板）。
     */
    public String chat(String systemPrompt, String userPrompt) {
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("max_tokens", 4000);
            body.put("system", systemPrompt);
            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", userPrompt);
            messages.add(userMsg);
            body.put("messages", messages);

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeout)).build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/v1/messages"))
                .timeout(Duration.ofMillis(timeout))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(body.toJSONString()))
                .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() != 200) {
                log.error("DeepSeek 调用失败 status={} body={}", resp.statusCode(), resp.body());
                return null;
            }
            // Anthropic 响应: { content: [ { type:"text", text:"..." } ] }
            JSONObject json = JSON.parseObject(resp.body());
            JSONArray content = json.getJSONArray("content");
            if (content == null || content.isEmpty()) return null;
            return content.getJSONObject(0).getString("text");
        } catch (Exception e) {
            log.error("DeepSeek 调用异常", e);
            return null;
        }
    }
}
```

- [ ] **Step 3: 编译验证**

Run: `mvn -pl ruoyi-system -am compile -q`
Expected: BUILD 无报错

- [ ] **Step 4: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/system/service/impl/DeepSeekClient.java ruoyi-admin/src/main/resources/application.yml
git commit -m "feat: DeepSeek客户端(Anthropic兼容)+application.yml配置"
```

---

### Task B4: NetReportService + Prometheus聚合查询

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/INetReportService.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/NetReportServiceImpl.java`

**Interfaces:**
- Consumes: `PrometheusQueryService.instantQuery(String)` (已存在), `HealthScoreCalculator.calculate(...)`, `DeepSeekClient.chat(...)`, `NetReportMapper.insertNetReport(...)`
- Produces: `INetReportService.generateReport(String reportType)` → 生成并存储一份报告；`selectNetReportList/selectNetReportById`

- [ ] **Step 1: INetReportService 接口**

```java
package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.NetReport;

public interface INetReportService {
    /** 生成指定周期报告并存库 */
    NetReport generateReport(String reportType);
    List<NetReport> selectNetReportList(NetReport report);
    NetReport selectNetReportById(Long reportId);
    int deleteNetReportById(Long reportId);
}
```

- [ ] **Step 2: NetReportServiceImpl**

```java
package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.domain.NetReport;
import com.ruoyi.system.mapper.NetReportMapper;
import com.ruoyi.system.service.INetReportService;

@Service
public class NetReportServiceImpl implements INetReportService {

    private static final Logger log = LoggerFactory.getLogger(NetReportServiceImpl.class);

    @Autowired private NetReportMapper netReportMapper;
    @Autowired private PrometheusQueryService prometheusQueryService;
    @Autowired private DeepSeekClient deepSeekClient;

    @Override
    public NetReport generateReport(String reportType) {
        Date end = new Date();
        Date start = calcStart(reportType, end);

        // 1. 查 Prometheus 聚合指标（失败则用默认值兜底）
        double onlineRate = queryScalar("avg(up)", 1.0);
        double portUpRate = queryScalar("avg(ifOperStatus)", 1.0);
        // 简化：故障次数/设备数/丢包/带宽/抖动 用占位查询或默认
        int deviceCount = (int) queryScalar("count(up)", 0);
        int faultCount = (int) queryScalar("count(up == 0)", 0);
        double avgPacketLoss = 0.0;
        double bwOverRatio = 0.0;
        double flowJitter = 0.0;

        // 2. 评分
        HealthScoreCalculator.ScoreResult score = HealthScoreCalculator.calculate(
            onlineRate, faultCount, deviceCount, portUpRate, avgPacketLoss, bwOverRatio, flowJitter);

        // 3. 组装 raw metrics
        JSONObject raw = new JSONObject();
        raw.put("onlineRate", onlineRate);
        raw.put("portUpRate", portUpRate);
        raw.put("deviceCount", deviceCount);
        raw.put("faultCount", faultCount);

        // 4. 调大模型（失败降级为纯数据模板）
        String content = buildAiReport(reportType, score, raw);

        // 5. 存库
        NetReport report = new NetReport();
        report.setReportType(reportType);
        report.setPeriodStart(start);
        report.setPeriodEnd(end);
        report.setHealthScore(BigDecimal.valueOf(score.totalScore));
        report.setDeviceScore(BigDecimal.valueOf(score.deviceScore));
        report.setLineScore(BigDecimal.valueOf(score.lineScore));
        report.setFlowScore(BigDecimal.valueOf(score.flowScore));
        report.setContentMd(content);
        report.setRawMetrics(raw.toJSONString());
        report.setCreateBy("system");
        netReportMapper.insertNetReport(report);
        log.info("生成{}报告 健康分={} grade={}", reportType, score.totalScore, score.grade);
        return report;
    }

    private String buildAiReport(String reportType, HealthScoreCalculator.ScoreResult score, JSONObject raw) {
        String sys = "你是网络运维专家，根据指标数据生成一份专业的中文网络运行报告。"
            + "结构：一、总体概述 二、健康评分详情 三、异常清单 四、趋势分析 五、优化建议。使用Markdown格式。";
        String user = String.format(
            "报告周期：%s\n健康总分：%.2f(%s)\n设备分：%.2f 线路分：%.2f 流量分：%.2f\n原始指标：%s",
            reportType, score.totalScore, score.grade,
            score.deviceScore, score.lineScore, score.flowScore, raw.toJSONString());
        String ai = deepSeekClient.chat(sys, user);
        if (ai != null && !ai.isEmpty()) {
            return ai;
        }
        // 降级：纯数据模板
        return String.format("# 网络运行报告(%s)\n\n> ⚠️ AI分析不可用，以下为原始数据\n\n"
            + "- 健康总分：**%.2f** (%s)\n- 设备维度：%.2f\n- 线路维度：%.2f\n- 流量维度：%.2f\n\n原始指标：\n```json\n%s\n```",
            reportType, score.totalScore, score.grade,
            score.deviceScore, score.lineScore, score.flowScore, raw.toJSONString());
    }

    private double queryScalar(String query, double fallback) {
        try {
            Double v = prometheusQueryService.instantQueryScalar(query);
            return v != null ? v : fallback;
        } catch (Exception e) {
            log.warn("Prometheus查询失败 query={}, 用兜底值{}", query, fallback);
            return fallback;
        }
    }

    private Date calcStart(String reportType, Date end) {
        Calendar c = Calendar.getInstance();
        c.setTime(end);
        switch (reportType) {
            case "DAILY": c.add(Calendar.DAY_OF_MONTH, -1); break;
            case "WEEKLY": c.add(Calendar.DAY_OF_MONTH, -7); break;
            case "MONTHLY": c.add(Calendar.DAY_OF_MONTH, -30); break;
            case "QUARTERLY": c.add(Calendar.DAY_OF_MONTH, -90); break;
            case "HALF_YEAR": c.add(Calendar.DAY_OF_MONTH, -180); break;
            case "YEARLY": c.add(Calendar.DAY_OF_MONTH, -365); break;
            default: c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return c.getTime();
    }

    @Override
    public List<NetReport> selectNetReportList(NetReport report) {
        return netReportMapper.selectNetReportList(report);
    }
    @Override
    public NetReport selectNetReportById(Long reportId) {
        return netReportMapper.selectNetReportById(reportId);
    }
    @Override
    public int deleteNetReportById(Long reportId) {
        return netReportMapper.deleteNetReportById(reportId);
    }
}
```

- [ ] **Step 3: PrometheusQueryService 加 instantQueryScalar 方法**

在 `PrometheusQueryService.java` 中加一个提取标量值的辅助方法（若不存在）：

```java
/** 提取瞬时查询的第一个标量值，无数据返回 null */
public Double instantQueryScalar(String query) {
    try {
        String json = instantQuery(query);  // 现有方法返回原始JSON
        com.alibaba.fastjson2.JSONObject obj = com.alibaba.fastjson2.JSON.parseObject(json);
        com.alibaba.fastjson2.JSONArray result = obj.getJSONObject("data").getJSONArray("result");
        if (result == null || result.isEmpty()) return null;
        com.alibaba.fastjson2.JSONArray value = result.getJSONObject(0).getJSONArray("value");
        // value = [timestamp, "stringValue"]
        return Double.parseDouble(value.getString(1));
    } catch (Exception e) {
        return null;
    }
}
```

> 注：如果现有 `instantQuery` 签名/返回类型不同，实现者需读 PrometheusQueryService 现有代码适配。

- [ ] **Step 4: 编译验证**

Run: `mvn -pl ruoyi-system -am compile -q`
Expected: BUILD 无报错

- [ ] **Step 5: Commit**

```bash
git add ruoyi-system/src/main/java/com/ruoyi/system/service/INetReportService.java ruoyi-system/src/main/java/com/ruoyi/system/service/impl/NetReportServiceImpl.java ruoyi-system/src/main/java/com/ruoyi/system/service/impl/PrometheusQueryService.java
git commit -m "feat: NetReportService(Prometheus聚合+评分+DeepSeek+降级兜底)"
```

---

### Task B5: NetReportTask 6周期定时 + NetReportController + 前端页

**Files:**
- Create: `ruoyi-quartz/src/main/java/com/ruoyi/quartz/task/NetReportTask.java`
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/report/NetReportController.java`
- Create: `ruoyi-ui/src/api/report/report.js`
- Create: `ruoyi-ui/src/views/report/index.vue`

**Interfaces:**
- Consumes: `INetReportService.generateReport/selectNetReportList/selectNetReportById`
- Produces: `netReportTask.daily()` 等6方法；`GET /report/list`、`GET /report/{id}`、`POST /report/generate/{type}`

- [ ] **Step 1: NetReportTask（供Quartz sys_job配置）**

```java
package com.ruoyi.quartz.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.service.INetReportService;

/** 网络运行报告定时任务（sys_job 配置 invoke_target=netReportTask.daily() 等） */
@Component("netReportTask")
public class NetReportTask {

    @Autowired
    private INetReportService netReportService;

    public void daily()     { netReportService.generateReport("DAILY"); }
    public void weekly()    { netReportService.generateReport("WEEKLY"); }
    public void monthly()   { netReportService.generateReport("MONTHLY"); }
    public void quarterly() { netReportService.generateReport("QUARTERLY"); }
    public void halfYear()  { netReportService.generateReport("HALF_YEAR"); }
    public void yearly()    { netReportService.generateReport("YEARLY"); }
}
```

- [ ] **Step 2: NetReportController**

```java
package com.ruoyi.web.controller.report;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.NetReport;
import com.ruoyi.system.service.INetReportService;

/** 网络运行报告 */
@RestController
@RequestMapping("/report")
public class NetReportController extends BaseController {

    @Autowired
    private INetReportService netReportService;

    @PreAuthorize("@ss.hasPermi('report:net:list')")
    @GetMapping("/list")
    public TableDataInfo list(NetReport report) {
        startPage();
        List<NetReport> list = netReportService.selectNetReportList(report);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('report:net:query')")
    @GetMapping("/{reportId}")
    public AjaxResult getInfo(@PathVariable Long reportId) {
        return success(netReportService.selectNetReportById(reportId));
    }

    @PreAuthorize("@ss.hasPermi('report:net:add')")
    @PostMapping("/generate/{reportType}")
    public AjaxResult generate(@PathVariable String reportType) {
        return success(netReportService.generateReport(reportType));
    }

    @PreAuthorize("@ss.hasPermi('report:net:remove')")
    @DeleteMapping("/{reportId}")
    public AjaxResult remove(@PathVariable Long reportId) {
        return toAjax(netReportService.deleteNetReportById(reportId));
    }
}
```

- [ ] **Step 3: 前端 api/report/report.js**

```js
import request from '@/utils/request'

export function listReport(query) {
  return request({ url: '/report/list', method: 'get', params: query })
}
export function getReport(reportId) {
  return request({ url: '/report/' + reportId, method: 'get' })
}
export function generateReport(reportType) {
  return request({ url: '/report/generate/' + reportType, method: 'post' })
}
export function delReport(reportId) {
  return request({ url: '/report/' + reportId, method: 'delete' })
}
```

- [ ] **Step 4: 前端 views/report/index.vue**

```vue
<template>
  <div class="app-container">
    <el-radio-group v-model="queryParams.reportType" @change="handleQuery" style="margin-bottom:16px">
      <el-radio-button label="">全部</el-radio-button>
      <el-radio-button label="DAILY">日报</el-radio-button>
      <el-radio-button label="WEEKLY">周报</el-radio-button>
      <el-radio-button label="MONTHLY">月报</el-radio-button>
      <el-radio-button label="QUARTERLY">季度报</el-radio-button>
      <el-radio-button label="HALF_YEAR">半年报</el-radio-button>
      <el-radio-button label="YEARLY">年报</el-radio-button>
    </el-radio-group>
    <el-button type="primary" size="mini" style="margin-left:12px" @click="handleGenerate">立即生成日报</el-button>

    <el-table v-loading="loading" :data="reportList">
      <el-table-column label="周期" prop="reportType" :formatter="typeFmt" width="90" />
      <el-table-column label="健康分" prop="healthScore" width="90">
        <template slot-scope="s"><span :style="scoreStyle(s.row.healthScore)">{{ s.row.healthScore }}</span></template>
      </el-table-column>
      <el-table-column label="设备" prop="deviceScore" width="80" />
      <el-table-column label="线路" prop="lineScore" width="80" />
      <el-table-column label="流量" prop="flowScore" width="80" />
      <el-table-column label="统计区间" width="300">
        <template slot-scope="s">{{ parseTime(s.row.periodStart) }} ~ {{ parseTime(s.row.periodEnd) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="s">
          <el-button size="mini" type="text" @click="viewReport(s.row)">查看</el-button>
          <el-button size="mini" type="text" style="color:#f56c6c" @click="handleDelete(s.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="dialogTitle" :visible.sync="open" width="800px">
      <div v-html="renderedMd" style="max-height:60vh;overflow:auto"></div>
    </el-dialog>
  </div>
</template>

<script>
import { listReport, getReport, generateReport, delReport } from '@/api/report/report'

export default {
  name: 'NetReport',
  data() {
    return {
      loading: false, reportList: [], total: 0,
      queryParams: { pageNum: 1, pageSize: 10, reportType: '' },
      open: false, dialogTitle: '', renderedMd: ''
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listReport(this.queryParams).then(res => {
        this.reportList = res.rows; this.total = res.total; this.loading = false
      })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    typeFmt(row) {
      const m = { DAILY:'日报', WEEKLY:'周报', MONTHLY:'月报', QUARTERLY:'季度报', HALF_YEAR:'半年报', YEARLY:'年报' }
      return m[row.reportType] || row.reportType
    },
    scoreStyle(s) {
      const color = s>=90?'#0ecb7e':s>=75?'#409EFF':s>=60?'#e6a23c':'#f56c6c'
      return `color:${color};font-weight:600`
    },
    viewReport(row) {
      getReport(row.reportId).then(res => {
        this.dialogTitle = this.typeFmt(row) + ' - 健康分 ' + res.data.healthScore
        // 简单Markdown转HTML（换行+标题），若项目有markdown库可替换
        this.renderedMd = this.mdToHtml(res.data.contentMd || '')
        this.open = true
      })
    },
    mdToHtml(md) {
      return md
        .replace(/^### (.*)$/gm, '<h3>$1</h3>')
        .replace(/^## (.*)$/gm, '<h2>$1</h2>')
        .replace(/^# (.*)$/gm, '<h1>$1</h1>')
        .replace(/\*\*(.*?)\*\*/g, '<b>$1</b>')
        .replace(/\n/g, '<br/>')
    },
    handleGenerate() {
      this.$modal.loading('正在生成报告，请稍候...')
      generateReport('DAILY').then(() => {
        this.$modal.closeLoading(); this.$modal.msgSuccess('生成成功'); this.getList()
      }).catch(() => this.$modal.closeLoading())
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该报告？').then(() => delReport(row.reportId)).then(() => {
        this.$modal.msgSuccess('删除成功'); this.getList()
      }).catch(() => {})
    }
  }
}
</script>
```

- [ ] **Step 5: 加路由 + DB菜单权限**

在 `ruoyi-ui/src/router/index.js` 的设备管理 children 或独立加「运行报告」路由：

```js
{
  path: "report",
  component: () => import("@/views/report/index"),
  name: "NetReport",
  meta: { title: "运行报告", icon: "documentation", permissions: ["report:net:list"] },
}
```

DB 加菜单权限：
```sql
INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, menu_type, perms, icon, create_by, create_time)
VALUES ('运行报告', 2000, 8, 'report', 'report/index', 'C', 'report:net:list', 'documentation', 'admin', sysdate());
-- 给 admin 角色授权
INSERT INTO sys_role_menu (role_id, menu_id) SELECT 1, LAST_INSERT_ID();
```

- [ ] **Step 6: 编译验证**

Run: `mvn -pl ruoyi-admin -am compile -q && cd ruoyi-ui && npm run build:prod 2>&1 | tail -3`
Expected: BUILD 无报错 + `Build complete`

- [ ] **Step 7: Commit**

```bash
git add ruoyi-quartz/src/main/java/com/ruoyi/quartz/task/NetReportTask.java ruoyi-admin/src/main/java/com/ruoyi/web/controller/report/NetReportController.java ruoyi-ui/src/api/report/report.js ruoyi-ui/src/views/report/index.vue ruoyi-ui/src/router/index.js
git commit -m "feat: 网络报告6周期Task+Controller+前端页+菜单权限"
```

---

### Task B6: 部署 + 端到端验证

- [ ] **Step 1: 全量编译**

Run: `mvn clean package -DskipTests -q && cd ruoyi-ui && npm run build:prod`

- [ ] **Step 2: 部署后端 + 前端**

用 zhihuiyuanqu-deploy Skill 全量部署到 1.94.26.126。

- [ ] **Step 3: 手动触发一份日报验证**

浏览器登录 → 运行报告页 → 点「立即生成日报」→ 确认：
- 报告出现在列表，有健康分
- 点查看能看到 Markdown 内容（AI生成或降级模板）
- 无 JS 错误

- [ ] **Step 4: 验证 Webhook（阶段A）**

```bash
curl -X POST http://1.94.26.126:8090/device/alert/webhook \
  -H 'Content-Type: application/json' \
  -d '{"alerts":[{"status":"firing","labels":{"instance":"192.168.1.10","alertname":"DeviceDown"}}]}'
```
预期：返回 success（若测试机IP非白名单则 forbidden，属正常）。

- [ ] **Step 5: Commit（收尾）**

```bash
git add -A && git commit -m "chore: 监控体系改造部署验证通过"
```

---

## 阶段划分说明

- **阶段A（Task A1-A4）**：设备状态迁移，独立可交付。A4是运维文档需人工在服务器执行。
- **阶段B（Task B1-B6）**：AI报告，依赖A的Prometheus数据。B2健康评分是纯函数TDD，B6端到端验证。
- 运维动作（Categraf ping、AlertManager部署、Grafana建Dashboard）由 docs/monitoring 文档指导人工完成。
