package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * room_info table entity
 *
 * @author ruoyi
 */
public class RoomInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "房间ID", cellType = ColumnType.NUMERIC)
    private Long roomId;

    @Excel(name = "房间编号")
    private String roomCode;

    @Excel(name = "公寓ID", cellType = ColumnType.NUMERIC)
    private Long apartmentId;

    @Excel(name = "楼栋")
    private String building;

    @Excel(name = "楼层")
    private String floor;

    @Excel(name = "户型")
    private String unitType;

    @Excel(name = "面积")
    private BigDecimal area;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "租期开始", width = 30, dateFormat = "yyyy-MM-dd")
    private Date rentStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "租期结束", width = 30, dateFormat = "yyyy-MM-dd")
    private Date rentEnd;

    @Excel(name = "租客人数", cellType = ColumnType.NUMERIC)
    private Integer tenantCount;

    @Excel(name = "门锁类型")
    private String doorType;

    @Excel(name = "设备状态")
    private String deviceStatus;

    @Excel(name = "状态", readConverterExp = "0=空闲,1=已租,2=维修中")
    private String status;

    public Long getRoomId()
    {
        return roomId;
    }

    public void setRoomId(Long roomId)
    {
        this.roomId = roomId;
    }

    @NotBlank(message = "房间编号不能为空")
    @Size(min = 0, max = 50, message = "房间编号长度不能超过50个字符")
    public String getRoomCode()
    {
        return roomCode;
    }

    public void setRoomCode(String roomCode)
    {
        this.roomCode = roomCode;
    }

    public Long getApartmentId()
    {
        return apartmentId;
    }

    public void setApartmentId(Long apartmentId)
    {
        this.apartmentId = apartmentId;
    }

    @Size(min = 0, max = 50, message = "楼栋长度不能超过50个字符")
    public String getBuilding()
    {
        return building;
    }

    public void setBuilding(String building)
    {
        this.building = building;
    }

    @Size(min = 0, max = 50, message = "楼层长度不能超过50个字符")
    public String getFloor()
    {
        return floor;
    }

    public void setFloor(String floor)
    {
        this.floor = floor;
    }

    @Size(min = 0, max = 50, message = "户型长度不能超过50个字符")
    public String getUnitType()
    {
        return unitType;
    }

    public void setUnitType(String unitType)
    {
        this.unitType = unitType;
    }

    public BigDecimal getArea()
    {
        return area;
    }

    public void setArea(BigDecimal area)
    {
        this.area = area;
    }

    public Date getRentStart()
    {
        return rentStart;
    }

    public void setRentStart(Date rentStart)
    {
        this.rentStart = rentStart;
    }

    public Date getRentEnd()
    {
        return rentEnd;
    }

    public void setRentEnd(Date rentEnd)
    {
        this.rentEnd = rentEnd;
    }

    public Integer getTenantCount()
    {
        return tenantCount;
    }

    public void setTenantCount(Integer tenantCount)
    {
        this.tenantCount = tenantCount;
    }

    @Size(min = 0, max = 50, message = "门锁类型长度不能超过50个字符")
    public String getDoorType()
    {
        return doorType;
    }

    public void setDoorType(String doorType)
    {
        this.doorType = doorType;
    }

    @Size(min = 0, max = 50, message = "设备状态长度不能超过50个字符")
    public String getDeviceStatus()
    {
        return deviceStatus;
    }

    public void setDeviceStatus(String deviceStatus)
    {
        this.deviceStatus = deviceStatus;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("roomId", getRoomId())
            .append("roomCode", getRoomCode())
            .append("apartmentId", getApartmentId())
            .append("building", getBuilding())
            .append("floor", getFloor())
            .append("unitType", getUnitType())
            .append("area", getArea())
            .append("rentStart", getRentStart())
            .append("rentEnd", getRentEnd())
            .append("tenantCount", getTenantCount())
            .append("doorType", getDoorType())
            .append("deviceStatus", getDeviceStatus())
            .append("status", getStatus())
            .append("deptId", getDeptId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
