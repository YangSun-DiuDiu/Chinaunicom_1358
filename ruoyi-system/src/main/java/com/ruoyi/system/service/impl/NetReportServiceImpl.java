package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.system.domain.NetReport;
import com.ruoyi.system.mapper.NetReportMapper;
import com.ruoyi.system.service.INetReportService;

/**
 * 网络运行报告 Service 实现
 * 业务编排：Prometheus聚合查询 → 三维健康评分 → DeepSeek生成报告(格式B) → 降级兜底 → 存库
 *
 * @author ruoyi
 */
@Service
public class NetReportServiceImpl implements INetReportService
{
    private static final Logger log = LoggerFactory.getLogger(NetReportServiceImpl.class);

    @Autowired
    private NetReportMapper netReportMapper;

    @Autowired
    private PrometheusQueryService prometheusQueryService;

    @Autowired
    private DeepSeekClient deepSeekClient;

    @Override
    public NetReport generateReport(String reportType)
    {
        Date end = new Date();
        Date start = calcStart(reportType, end);

        // 1. 查 Prometheus 聚合指标（失败用兜底默认值，不阻塞报告生成）
        double onlineRate = queryScalar("avg(up)", 1.0);
        double portUpRate = queryScalar("avg(ifOperStatus)", 1.0);
        int deviceCount = (int) queryScalar("count(up)", 0);
        int faultCount = (int) queryScalar("count(up == 0)", 0);
        double avgPacketLoss = 0.0;
        double bwOverRatio = 0.0;
        double flowJitter = 0.0;

        // 2. 三维健康评分
        HealthScoreCalculator.ScoreResult score = HealthScoreCalculator.calculate(
            onlineRate, faultCount, deviceCount, portUpRate, avgPacketLoss, bwOverRatio, flowJitter);

        // 3. 原始指标快照
        JSONObject raw = new JSONObject();
        raw.put("onlineRate", onlineRate);
        raw.put("portUpRate", portUpRate);
        raw.put("deviceCount", deviceCount);
        raw.put("faultCount", faultCount);

        // 4. 调大模型生成报告（格式B），失败降级为纯数据模板
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
        log.info("生成{}报告 健康分={} 评级={}", reportType, score.totalScore, score.grade);
        return report;
    }

    /**
     * 组装格式B分级评估型报告。AI 失败时降级为纯数据模板。
     */
    private String buildAiReport(String reportType, HealthScoreCalculator.ScoreResult score, JSONObject raw)
    {
        String sys = "你是资深网络运维专家。根据提供的指标数据生成一份专业的中文网络运行报告，"
            + "严格采用【分级评估型】结构，使用Markdown格式，包含以下部分：\n"
            + "【健康评级】给出总分与评级(优/良/中/差)及一句话结论\n"
            + "【设备维度】评分+设备在线情况分析\n"
            + "【线路维度】评分+端口与线路质量分析\n"
            + "【流量维度】评分+带宽与流量分析\n"
            + "【风险预警】列出需要关注的风险点\n"
            + "【优化建议】给出3-5条可执行的改进建议";
        String user = String.format(
            "报告周期：%s\n健康总分：%.2f（%s）\n设备维度分：%.2f\n线路维度分：%.2f\n流量维度分：%.2f\n原始指标：%s",
            reportTypeName(reportType), score.totalScore, score.grade,
            score.deviceScore, score.lineScore, score.flowScore, raw.toJSONString());
        String ai = deepSeekClient.chat(sys, user);
        if (ai != null && !ai.isEmpty())
        {
            return ai;
        }
        // 降级：纯数据模板（AI 不可用时不阻塞）
        return String.format(
            "# 网络运行报告（%s）\n\n> ⚠️ AI分析暂不可用，以下为原始评分数据\n\n"
            + "## 健康评级\n\n**总分 %.2f（%s）**\n\n"
            + "## 三维评分\n\n| 维度 | 得分 |\n|------|------|\n"
            + "| 设备维度 | %.2f |\n| 线路维度 | %.2f |\n| 流量维度 | %.2f |\n\n"
            + "## 原始指标\n\n```json\n%s\n```\n",
            reportTypeName(reportType), score.totalScore, score.grade,
            score.deviceScore, score.lineScore, score.flowScore, raw.toJSONString());
    }

    /**
     * 从 Prometheus 即时查询提取第一个标量值。
     * instantQuery 返回 data 对象（含 result 数组），每项 value=[timestamp, "strValue"]。
     * 查询失败或无数据返回 fallback。
     */
    @SuppressWarnings("unchecked")
    private double queryScalar(String query, double fallback)
    {
        try
        {
            Map<String, Object> data = prometheusQueryService.instantQuery(query);
            if (data == null) return fallback;
            Object resultObj = data.get("result");
            if (!(resultObj instanceof List)) return fallback;
            List<Object> results = (List<Object>) resultObj;
            if (results.isEmpty()) return fallback;
            Object first = results.get(0);
            if (!(first instanceof Map)) return fallback;
            Object valueObj = ((Map<String, Object>) first).get("value");
            if (!(valueObj instanceof List)) return fallback;
            List<Object> value = (List<Object>) valueObj;
            if (value.size() < 2) return fallback;
            return Double.parseDouble(String.valueOf(value.get(1)));
        }
        catch (Exception e)
        {
            log.warn("Prometheus查询失败 query={}, 使用兜底值{}", query, fallback);
            return fallback;
        }
    }

    private Date calcStart(String reportType, Date end)
    {
        Calendar c = Calendar.getInstance();
        c.setTime(end);
        switch (reportType)
        {
            case "DAILY":     c.add(Calendar.DAY_OF_MONTH, -1);   break;
            case "WEEKLY":    c.add(Calendar.DAY_OF_MONTH, -7);   break;
            case "MONTHLY":   c.add(Calendar.DAY_OF_MONTH, -30);  break;
            case "QUARTERLY": c.add(Calendar.DAY_OF_MONTH, -90);  break;
            case "HALF_YEAR": c.add(Calendar.DAY_OF_MONTH, -180); break;
            case "YEARLY":    c.add(Calendar.DAY_OF_MONTH, -365); break;
            default:          c.add(Calendar.DAY_OF_MONTH, -1);
        }
        return c.getTime();
    }

    private String reportTypeName(String reportType)
    {
        switch (reportType)
        {
            case "DAILY":     return "日报";
            case "WEEKLY":    return "周报";
            case "MONTHLY":   return "月报";
            case "QUARTERLY": return "季度报";
            case "HALF_YEAR": return "半年报";
            case "YEARLY":    return "年报";
            default:          return reportType;
        }
    }

    @Override
    public List<NetReport> selectNetReportList(NetReport report)
    {
        return netReportMapper.selectNetReportList(report);
    }

    @Override
    public NetReport selectNetReportById(Long reportId)
    {
        return netReportMapper.selectNetReportById(reportId);
    }

    @Override
    public int deleteNetReportById(Long reportId)
    {
        return netReportMapper.deleteNetReportById(reportId);
    }
}
