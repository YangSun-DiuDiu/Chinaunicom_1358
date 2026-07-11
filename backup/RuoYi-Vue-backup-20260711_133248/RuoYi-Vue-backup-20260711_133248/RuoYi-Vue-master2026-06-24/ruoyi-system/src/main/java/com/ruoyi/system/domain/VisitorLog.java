package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 访客记录表 visitor_log
 *
 * @author ruoyi
 */
public class VisitorLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @Excel(name = "记录ID", cellType = ColumnType.NUMERIC)
    private Long logId;

    /** 预约ID */
    @Excel(name = "预约ID", cellType = ColumnType.NUMERIC)
    private Long appointmentId;

    /** 访客姓名 */
    @Excel(name = "访客姓名")
    private String visitorName;

    /** 访客电话 */
    @Excel(name = "访客电话")
    private String visitorPhone;

    /** 访客身份证号 */
    @Excel(name = "访客身份证号")
    private String visitorIdCard;

    /** 访客单位 */
    @Excel(name = "访客单位")
    private String visitorCompany;

    /** 来访事由 */
    @Excel(name = "来访事由")
    private String visitReason;

    /** 被访人姓名 */
    @Excel(name = "被访人姓名")
    private String hostName;

    /** 被访人部门 */
    @Excel(name = "被访人部门")
    private String hostDept;

    /** 进入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "进入时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date entryTime;

    /** 离开时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "离开时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date exitTime;

    /** 登记类型（APPOINTMENT=预约 WALKIN=散客） */
    @Excel(name = "登记类型", readConverterExp = "APPOINTMENT=预约,WALKIN=散客")
    private String registerType;

    /** 访客通行码 */
    private String passCode;

    /** 是否开车（0=否 1=是） */
    private String hasCar;

    /** 车牌号 */
    private String carPlate;

    /** 是否携带物资（0=否 1=是） */
    private String hasGoods;

    /** 携带物资说明 */
    private String goodsDesc;

    /** 租户ID */
    @Excel(name = "租户ID", cellType = ColumnType.NUMERIC)
    private Long tenantId;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getAppointmentId()
    {
        return appointmentId;
    }

    public void setAppointmentId(Long appointmentId)
    {
        this.appointmentId = appointmentId;
    }

    public String getVisitorName()
    {
        return visitorName;
    }

    public void setVisitorName(String visitorName)
    {
        this.visitorName = visitorName;
    }

    public String getVisitorPhone()
    {
        return visitorPhone;
    }

    public void setVisitorPhone(String visitorPhone)
    {
        this.visitorPhone = visitorPhone;
    }

    public String getVisitorIdCard()
    {
        return visitorIdCard;
    }

    public void setVisitorIdCard(String visitorIdCard)
    {
        this.visitorIdCard = visitorIdCard;
    }

    public String getVisitorCompany()
    {
        return visitorCompany;
    }

    public void setVisitorCompany(String visitorCompany)
    {
        this.visitorCompany = visitorCompany;
    }

    public String getVisitReason()
    {
        return visitReason;
    }

    public void setVisitReason(String visitReason)
    {
        this.visitReason = visitReason;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    public String getHostDept()
    {
        return hostDept;
    }

    public void setHostDept(String hostDept)
    {
        this.hostDept = hostDept;
    }

    public Date getEntryTime()
    {
        return entryTime;
    }

    public void setEntryTime(Date entryTime)
    {
        this.entryTime = entryTime;
    }

    public Date getExitTime()
    {
        return exitTime;
    }

    public void setExitTime(Date exitTime)
    {
        this.exitTime = exitTime;
    }

    public String getRegisterType()
    {
        return registerType;
    }

    public void setRegisterType(String registerType)
    {
        this.registerType = registerType;
    }

    public String getPassCode() { return passCode; }
    public void setPassCode(String passCode) { this.passCode = passCode; }

    public String getHasCar() { return hasCar; }
    public void setHasCar(String hasCar) { this.hasCar = hasCar; }
    public String getCarPlate() { return carPlate; }
    public void setCarPlate(String carPlate) { this.carPlate = carPlate; }
    public String getHasGoods() { return hasGoods; }
    public void setHasGoods(String hasGoods) { this.hasGoods = hasGoods; }
    public String getGoodsDesc() { return goodsDesc; }
    public void setGoodsDesc(String goodsDesc) { this.goodsDesc = goodsDesc; }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("appointmentId", getAppointmentId())
            .append("visitorName", getVisitorName())
            .append("visitorPhone", getVisitorPhone())
            .append("visitorIdCard", getVisitorIdCard())
            .append("visitorCompany", getVisitorCompany())
            .append("visitReason", getVisitReason())
            .append("hostName", getHostName())
            .append("hostDept", getHostDept())
            .append("entryTime", getEntryTime())
            .append("exitTime", getExitTime())
            .append("registerType", getRegisterType())
            .append("tenantId", getTenantId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
