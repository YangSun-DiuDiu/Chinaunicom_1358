package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 物联网设备表 iot_device
 *
 * @author ruoyi
 */
public class Device extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 设备ID */
    @Excel(name = "设备ID", cellType = ColumnType.NUMERIC)
    private Long deviceId;

    /** 设备名称 */
    @Excel(name = "设备名称")
    private String deviceName;

    /** 设备类型（NETWORK=网络设备 MONITOR=监控设备 OTHER=其他） */
    @Excel(name = "设备类型", readConverterExp = "NETWORK=网络设备,MONITOR=监控设备,OTHER=其他")
    private String deviceType;

    /** IP地址 */
    @Excel(name = "IP地址")
    private String ipAddress;

    /** MAC地址 */
    @Excel(name = "MAC地址")
    private String macAddress;

    /** 设备型号 */
    @Excel(name = "设备型号")
    private String model;

    /** 安装位置 */
    @Excel(name = "安装位置")
    private String location;

    /** 端口信息 */
    @Excel(name = "端口信息")
    private String portInfo;

    /** 设备状态（ONLINE=在线 OFFLINE=离线 UNKNOWN=未知） */
    @Excel(name = "设备状态", readConverterExp = "ONLINE=在线,OFFLINE=离线,UNKNOWN=未知")
    private String status;

    /** SNMP团体名 */
    @Excel(name = "SNMP团体名")
    private String snmpCommunity;

    /** SNMP端口 */
    @Excel(name = "SNMP端口")
    private Integer snmpPort;

    /** SNMP版本（v2c=v2c v3=v3） */
    @Excel(name = "SNMP版本", readConverterExp = "v2c=v2c,v3=v3")
    private String snmpVersion;

    /** SNMP配置文件名（dev_IP下划线.toml） */
    private String snmpConfigFile;

    /** 配置内容（完整toml文本） */
    private String configContent;

    /** 配置MD5（用于一致性校验） */
    private String configMd5;

    /** Ping检测间隔（分钟，1-10） */
    @Excel(name = "Ping间隔(分)")
    private Integer pingInterval;

    /** 负责人 */
    @Excel(name = "负责人")
    private String responsible;

    /** 负责人电话 */
    @Excel(name = "负责人电话")
    private String responsiblePhone;

    /** 租户ID */
    @Excel(name = "租户ID")
    private Long tenantId;

    /** 父设备ID */
    @Excel(name = "父设备ID")
    private Long parentId;

    /** 设备图标（topo-icons 图标 key：router/switch/firewall/ap/server/camera/nvr/other） */
    private String icon;

    /** 连接线缆类型（fiber=光纤 ethernet=双绞线 wireless=无线 coaxial=同轴） */
    private String cableType;

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

    public String getDeviceType()
    {
        return deviceType;
    }

    public void setDeviceType(String deviceType)
    {
        this.deviceType = deviceType;
    }

    public String getIpAddress()
    {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    public String getMacAddress()
    {
        return macAddress;
    }

    public void setMacAddress(String macAddress)
    {
        this.macAddress = macAddress;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public String getPortInfo()
    {
        return portInfo;
    }

    public void setPortInfo(String portInfo)
    {
        this.portInfo = portInfo;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getSnmpCommunity()
    {
        return snmpCommunity;
    }

    public void setSnmpCommunity(String snmpCommunity)
    {
        this.snmpCommunity = snmpCommunity;
    }

    public Integer getSnmpPort()
    {
        return snmpPort;
    }

    public void setSnmpPort(Integer snmpPort)
    {
        this.snmpPort = snmpPort;
    }

    public String getSnmpVersion()
    {
        return snmpVersion;
    }

    public void setSnmpVersion(String snmpVersion)
    {
        this.snmpVersion = snmpVersion;
    }

    public String getSnmpConfigFile() { return snmpConfigFile; }
    public void setSnmpConfigFile(String snmpConfigFile) { this.snmpConfigFile = snmpConfigFile; }
    public String getConfigContent() { return configContent; }
    public void setConfigContent(String configContent) { this.configContent = configContent; }
    public String getConfigMd5() { return configMd5; }
    public void setConfigMd5(String configMd5) { this.configMd5 = configMd5; }

    public Integer getPingInterval()
    {
        return pingInterval;
    }

    public void setPingInterval(Integer pingInterval)
    {
        this.pingInterval = pingInterval;
    }

    public String getResponsible()
    {
        return responsible;
    }

    public void setResponsible(String responsible)
    {
        this.responsible = responsible;
    }

    public String getResponsiblePhone()
    {
        return responsiblePhone;
    }

    public void setResponsiblePhone(String responsiblePhone)
    {
        this.responsiblePhone = responsiblePhone;
    }

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public String getCableType()
    {
        return cableType;
    }

    public void setCableType(String cableType)
    {
        this.cableType = cableType;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("deviceId", getDeviceId())
            .append("deviceName", getDeviceName())
            .append("deviceType", getDeviceType())
            .append("ipAddress", getIpAddress())
            .append("macAddress", getMacAddress())
            .append("model", getModel())
            .append("location", getLocation())
            .append("portInfo", getPortInfo())
            .append("status", getStatus())
            .append("snmpCommunity", getSnmpCommunity())
            .append("snmpPort", getSnmpPort())
            .append("snmpVersion", getSnmpVersion())
            .append("pingInterval", getPingInterval())
            .append("responsible", getResponsible())
            .append("responsiblePhone", getResponsiblePhone())
            .append("tenantId", getTenantId())
            .append("parentId", getParentId())
            .append("icon", getIcon())
            .append("cableType", getCableType())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
