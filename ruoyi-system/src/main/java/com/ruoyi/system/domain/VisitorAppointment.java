package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 访客预约表 visitor_appointment
 *
 * @author ruoyi
 */
public class VisitorAppointment extends BaseEntity
{
    private static final long serialVersionUID = 1L;

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

    /** 被访人电话 */
    @Excel(name = "被访人电话")
    private String hostPhone;

    /** 来访时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "来访时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date visitTime;

    /** 离开时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "离开时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date leaveTime;

    /** 预约状态（PENDING=待审批 APPROVED=已通过 REJECTED=已拒绝 CANCELLED=已取消 VISITING=访客中 COMPLETED=已完成） */
    @Excel(name = "预约状态", readConverterExp = "PENDING=待审批,APPROVED=已通过,REJECTED=已拒绝,CANCELLED=已取消,VISITING=访客中,COMPLETED=已完成")
    private String status;

    /** 审批人ID */
    @Excel(name = "审批人ID", cellType = ColumnType.NUMERIC)
    private Long approverId;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "审批时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date approveTime;

    /** 审批备注 */
    @Excel(name = "审批备注")
    private String approveRemark;

    /** 短信发送状态（Y=已发送 N=未发送） */
    @Excel(name = "短信发送状态", readConverterExp = "Y=已发送,N=未发送")
    private String smsSent;

    /** 短信内容 */
    @Excel(name = "短信内容")
    private String smsContent;

    /** 访客通行码 */
    private String passCode;

    /** 是否开车（0=否 1=是） */
    @Excel(name = "是否开车", readConverterExp = "0=否,1=是")
    private String hasCar;

    /** 车牌号 */
    @Excel(name = "车牌号")
    private String carPlate;

    /** 是否携带物资（0=否 1=是） */
    @Excel(name = "是否携带物资", readConverterExp = "0=否,1=是")
    private String hasGoods;

    /** 携带物资说明 */
    @Excel(name = "携带物资说明")
    private String goodsDesc;

    /** 租户ID */
    @Excel(name = "租户ID", cellType = ColumnType.NUMERIC)
    private Long tenantId;

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

    public String getHostPhone()
    {
        return hostPhone;
    }

    public void setHostPhone(String hostPhone)
    {
        this.hostPhone = hostPhone;
    }

    public Date getVisitTime()
    {
        return visitTime;
    }

    public void setVisitTime(Date visitTime)
    {
        this.visitTime = visitTime;
    }

    public Date getLeaveTime()
    {
        return leaveTime;
    }

    public void setLeaveTime(Date leaveTime)
    {
        this.leaveTime = leaveTime;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Long getApproverId()
    {
        return approverId;
    }

    public void setApproverId(Long approverId)
    {
        this.approverId = approverId;
    }

    public Date getApproveTime()
    {
        return approveTime;
    }

    public void setApproveTime(Date approveTime)
    {
        this.approveTime = approveTime;
    }

    public String getApproveRemark()
    {
        return approveRemark;
    }

    public void setApproveRemark(String approveRemark)
    {
        this.approveRemark = approveRemark;
    }

    public String getSmsSent()
    {
        return smsSent;
    }

    public void setSmsSent(String smsSent)
    {
        this.smsSent = smsSent;
    }

    public String getSmsContent()
    {
        return smsContent;
    }

    public void setSmsContent(String smsContent)
    {
        this.smsContent = smsContent;
    }

    public String getPassCode()
    {
        return passCode;
    }

    public void setPassCode(String passCode)
    {
        this.passCode = passCode;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public String getHasCar() { return hasCar; }
    public void setHasCar(String hasCar) { this.hasCar = hasCar; }
    public String getCarPlate() { return carPlate; }
    public void setCarPlate(String carPlate) { this.carPlate = carPlate; }
    public String getHasGoods() { return hasGoods; }
    public void setHasGoods(String hasGoods) { this.hasGoods = hasGoods; }
    public String getGoodsDesc() { return goodsDesc; }
    public void setGoodsDesc(String goodsDesc) { this.goodsDesc = goodsDesc; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("appointmentId", getAppointmentId())
            .append("visitorName", getVisitorName())
            .append("visitorPhone", getVisitorPhone())
            .append("visitorIdCard", getVisitorIdCard())
            .append("visitorCompany", getVisitorCompany())
            .append("visitReason", getVisitReason())
            .append("hostName", getHostName())
            .append("hostDept", getHostDept())
            .append("hostPhone", getHostPhone())
            .append("visitTime", getVisitTime())
            .append("leaveTime", getLeaveTime())
            .append("status", getStatus())
            .append("approverId", getApproverId())
            .append("approveTime", getApproveTime())
            .append("approveRemark", getApproveRemark())
            .append("smsSent", getSmsSent())
            .append("smsContent", getSmsContent())
            .append("tenantId", getTenantId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
