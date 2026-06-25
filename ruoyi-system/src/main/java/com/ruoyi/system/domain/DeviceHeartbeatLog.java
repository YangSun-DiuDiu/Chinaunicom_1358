package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 设备心跳检测日志表 iot_device_heartbeat_log
 *
 * @author ruoyi
 */
public class DeviceHeartbeatLog extends BaseEntity
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

    /** IP地址 */
    @Excel(name = "IP地址")
    private String ipAddress;

    /** Ping结果（SUCCESS=成功 FAIL=失败） */
    @Excel(name = "Ping结果", readConverterExp = "SUCCESS=成功,FAIL=失败")
    private String pingResult;

    /** Ping延迟(ms) */
    @Excel(name = "Ping延迟(ms)", cellType = ColumnType.NUMERIC)
    private Long pingLatency;

    /** 检测时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "检测时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date detectTime;

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

    public String getIpAddress()
    {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    public String getPingResult()
    {
        return pingResult;
    }

    public void setPingResult(String pingResult)
    {
        this.pingResult = pingResult;
    }

    public Long getPingLatency()
    {
        return pingLatency;
    }

    public void setPingLatency(Long pingLatency)
    {
        this.pingLatency = pingLatency;
    }

    public Date getDetectTime()
    {
        return detectTime;
    }

    public void setDetectTime(Date detectTime)
    {
        this.detectTime = detectTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("deviceId", getDeviceId())
            .append("deviceName", getDeviceName())
            .append("ipAddress", getIpAddress())
            .append("pingResult", getPingResult())
            .append("pingLatency", getPingLatency())
            .append("detectTime", getDetectTime())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
