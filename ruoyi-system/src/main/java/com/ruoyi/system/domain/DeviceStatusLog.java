package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 设备状态变更日志表 iot_device_status_log
 *
 * @author ruoyi
 */
public class DeviceStatusLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    @Excel(name = "日志ID", cellType = ColumnType.NUMERIC)
    private Long logId;

    /** 设备ID */
    @Excel(name = "设备ID", cellType = ColumnType.NUMERIC)
    private Long deviceId;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String deviceName;

    /** 变更前状态 */
    @Excel(name = "变更前状态")
    private String oldStatus;

    /** 变更后状态 */
    @Excel(name = "变更后状态")
    private String newStatus;

    /** 变更类型（OFFLINE=离线 ONLINE=上线） */
    @Excel(name = "变更类型", readConverterExp = "OFFLINE=离线,ONLINE=上线")
    private String changeType;

    /** 变更时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "变更时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date changeTime;

    /** 是否发送短信（Y=是 N=否） */
    @Excel(name = "是否发送短信", readConverterExp = "Y=是,N=否")
    private String smsSent;

    /** 短信接收人 */
    @Excel(name = "短信接收人")
    private String smsRecipient;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceId(Long deviceId)
    {
        this.deviceId = deviceId;
    }

    public String getDeviceName()
    {
        return deviceName;
    }

    public void setDeviceName(String deviceName)
    {
        this.deviceName = deviceName;
    }

    public String getOldStatus()
    {
        return oldStatus;
    }

    public void setOldStatus(String oldStatus)
    {
        this.oldStatus = oldStatus;
    }

    public String getNewStatus()
    {
        return newStatus;
    }

    public void setNewStatus(String newStatus)
    {
        this.newStatus = newStatus;
    }

    public String getChangeType()
    {
        return changeType;
    }

    public void setChangeType(String changeType)
    {
        this.changeType = changeType;
    }

    public Date getChangeTime()
    {
        return changeTime;
    }

    public void setChangeTime(Date changeTime)
    {
        this.changeTime = changeTime;
    }

    public String getSmsSent()
    {
        return smsSent;
    }

    public void setSmsSent(String smsSent)
    {
        this.smsSent = smsSent;
    }

    public String getSmsRecipient()
    {
        return smsRecipient;
    }

    public void setSmsRecipient(String smsRecipient)
    {
        this.smsRecipient = smsRecipient;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("deviceId", getDeviceId())
            .append("deviceName", getDeviceName())
            .append("oldStatus", getOldStatus())
            .append("newStatus", getNewStatus())
            .append("changeType", getChangeType())
            .append("changeTime", getChangeTime())
            .append("smsSent", getSmsSent())
            .append("smsRecipient", getSmsRecipient())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
