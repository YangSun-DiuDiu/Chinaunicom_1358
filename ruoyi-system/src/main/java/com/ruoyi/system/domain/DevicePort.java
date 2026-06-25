package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 设备端口表 iot_device_port
 *
 * @author ruoyi
 */
public class DevicePort extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 端口ID */
    @Excel(name = "端口ID", cellType = ColumnType.NUMERIC)
    private Long portId;

    /** 设备ID */
    @Excel(name = "设备ID", cellType = ColumnType.NUMERIC)
    private Long deviceId;

    /** 端口名称 */
    @Excel(name = "端口名称")
    private String portName;

    /** 端口类型 */
    @Excel(name = "端口类型")
    private String portType;

    /** 端口状态 */
    @Excel(name = "端口状态")
    private String portStatus;

    /** 连接设备ID */
    @Excel(name = "连接设备ID", cellType = ColumnType.NUMERIC)
    private Long connectedDeviceId;

    /** 连接端口ID */
    @Excel(name = "连接端口ID", cellType = ColumnType.NUMERIC)
    private Long connectedPortId;

    public Long getPortId()
    {
        return portId;
    }

    public void setPortId(Long portId)
    {
        this.portId = portId;
    }

    public Long getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceId(Long deviceId)
    {
        this.deviceId = deviceId;
    }

    public String getPortName()
    {
        return portName;
    }

    public void setPortName(String portName)
    {
        this.portName = portName;
    }

    public String getPortType()
    {
        return portType;
    }

    public void setPortType(String portType)
    {
        this.portType = portType;
    }

    public String getPortStatus()
    {
        return portStatus;
    }

    public void setPortStatus(String portStatus)
    {
        this.portStatus = portStatus;
    }

    public Long getConnectedDeviceId()
    {
        return connectedDeviceId;
    }

    public void setConnectedDeviceId(Long connectedDeviceId)
    {
        this.connectedDeviceId = connectedDeviceId;
    }

    public Long getConnectedPortId()
    {
        return connectedPortId;
    }

    public void setConnectedPortId(Long connectedPortId)
    {
        this.connectedPortId = connectedPortId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("portId", getPortId())
            .append("deviceId", getDeviceId())
            .append("portName", getPortName())
            .append("portType", getPortType())
            .append("portStatus", getPortStatus())
            .append("connectedDeviceId", getConnectedDeviceId())
            .append("connectedPortId", getConnectedPortId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
