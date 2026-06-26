package com.ruoyi.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * meeting_room table entity
 *
 * @author ruoyi
 */
public class MeetingRoom extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "会议室ID", cellType = ColumnType.NUMERIC)
    private Long roomId;

    @Excel(name = "会议室名称")
    private String roomName;

    @Excel(name = "位置")
    private String location;

    @Excel(name = "容纳人数", cellType = ColumnType.NUMERIC)
    private Integer capacity;

    @Excel(name = "设备清单")
    private String equipment;

    @Excel(name = "状态", readConverterExp = "0=启用,1=停用")
    private String status;

    public Long getRoomId()
    {
        return roomId;
    }

    public void setRoomId(Long roomId)
    {
        this.roomId = roomId;
    }

    @NotBlank(message = "会议室名称不能为空")
    @Size(min = 0, max = 100, message = "会议室名称长度不能超过100个字符")
    public String getRoomName()
    {
        return roomName;
    }

    public void setRoomName(String roomName)
    {
        this.roomName = roomName;
    }

    @Size(min = 0, max = 200, message = "位置长度不能超过200个字符")
    public String getLocation()
    {
        return location;
    }

    public void setLocation(String location)
    {
        this.location = location;
    }

    public Integer getCapacity()
    {
        return capacity;
    }

    public void setCapacity(Integer capacity)
    {
        this.capacity = capacity;
    }

    @Size(min = 0, max = 500, message = "设备清单长度不能超过500个字符")
    public String getEquipment()
    {
        return equipment;
    }

    public void setEquipment(String equipment)
    {
        this.equipment = equipment;
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
            .append("roomName", getRoomName())
            .append("location", getLocation())
            .append("capacity", getCapacity())
            .append("equipment", getEquipment())
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
