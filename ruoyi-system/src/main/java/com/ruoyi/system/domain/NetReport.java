package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 网络运行报告对象 net_report
 *
 * @author system
 */
public class NetReport extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 报告ID */
    private Long reportId;

    /** 报告类型(DAILY/WEEKLY/MONTHLY/QUARTERLY/HALF_YEAR/YEARLY) */
    private String reportType;

    /** 统计开始时间 */
    private Date periodStart;

    /** 统计结束时间 */
    private Date periodEnd;

    /** 总健康分 */
    private BigDecimal healthScore;

    /** 设备维度分 */
    private BigDecimal deviceScore;

    /** 线路维度分 */
    private BigDecimal lineScore;

    /** 流量维度分 */
    private BigDecimal flowScore;

    /** 大模型生成正文 */
    private String contentMd;

    /** 原始指标快照 */
    private String rawMetrics;

    /** 删除标志(0正常1删除) */
    private String delFlag;

    public Long getReportId()
    {
        return reportId;
    }

    public void setReportId(Long reportId)
    {
        this.reportId = reportId;
    }

    public String getReportType()
    {
        return reportType;
    }

    public void setReportType(String reportType)
    {
        this.reportType = reportType;
    }

    public Date getPeriodStart()
    {
        return periodStart;
    }

    public void setPeriodStart(Date periodStart)
    {
        this.periodStart = periodStart;
    }

    public Date getPeriodEnd()
    {
        return periodEnd;
    }

    public void setPeriodEnd(Date periodEnd)
    {
        this.periodEnd = periodEnd;
    }

    public BigDecimal getHealthScore()
    {
        return healthScore;
    }

    public void setHealthScore(BigDecimal healthScore)
    {
        this.healthScore = healthScore;
    }

    public BigDecimal getDeviceScore()
    {
        return deviceScore;
    }

    public void setDeviceScore(BigDecimal deviceScore)
    {
        this.deviceScore = deviceScore;
    }

    public BigDecimal getLineScore()
    {
        return lineScore;
    }

    public void setLineScore(BigDecimal lineScore)
    {
        this.lineScore = lineScore;
    }

    public BigDecimal getFlowScore()
    {
        return flowScore;
    }

    public void setFlowScore(BigDecimal flowScore)
    {
        this.flowScore = flowScore;
    }

    public String getContentMd()
    {
        return contentMd;
    }

    public void setContentMd(String contentMd)
    {
        this.contentMd = contentMd;
    }

    public String getRawMetrics()
    {
        return rawMetrics;
    }

    public void setRawMetrics(String rawMetrics)
    {
        this.rawMetrics = rawMetrics;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }
}
