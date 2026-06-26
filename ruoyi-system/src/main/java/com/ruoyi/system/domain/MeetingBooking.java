package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * meeting_booking table entity
 *
 * @author ruoyi
 */
public class MeetingBooking extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "预约ID", cellType = ColumnType.NUMERIC)
    private Long bookingId;

    @Excel(name = "会议室ID", cellType = ColumnType.NUMERIC)
    private Long roomId;

    @Excel(name = "会议室名称")
    private String roomName;

    @Excel(name = "会议主题")
    private String title;

    @Excel(name = "主持人")
    private String hostName;

    @Excel(name = "联系电话")
    private String hostPhone;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    @Excel(name = "参会人数", cellType = ColumnType.NUMERIC)
    private Integer attendees;

    @Excel(name = "状态", readConverterExp = "PENDING=待审批,APPROVED=已通过,REJECTED=已拒绝,CANCELLED=已取消")
    private String status;

    public Long getBookingId()
    {
        return bookingId;
    }

    public void setBookingId(Long bookingId)
    {
        this.bookingId = bookingId;
    }

    @NotNull(message = "会议室不能为空")
    public Long getRoomId()
    {
        return roomId;
    }

    public void setRoomId(Long roomId)
    {
        this.roomId = roomId;
    }

    public String getRoomName()
    {
        return roomName;
    }

    public void setRoomName(String roomName)
    {
        this.roomName = roomName;
    }

    @NotBlank(message = "会议主题不能为空")
    @Size(min = 0, max = 200, message = "会议主题长度不能超过200个字符")
    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    @Size(min = 0, max = 64, message = "主持人名称长度不能超过64个字符")
    public String getHostName()
    {
        return hostName;
    }

    public void setHostName(String hostName)
    {
        this.hostName = hostName;
    }

    @Size(min = 0, max = 20, message = "联系电话长度不能超过20个字符")
    public String getHostPhone()
    {
        return hostPhone;
    }

    public void setHostPhone(String hostPhone)
    {
        this.hostPhone = hostPhone;
    }

    @NotNull(message = "开始时间不能为空")
    public Date getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    @NotNull(message = "结束时间不能为空")
    public Date getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public Integer getAttendees()
    {
        return attendees;
    }

    public void setAttendees(Integer attendees)
    {
        this.attendees = attendees;
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
            .append("bookingId", getBookingId())
            .append("roomId", getRoomId())
            .append("title", getTitle())
            .append("hostName", getHostName())
            .append("hostPhone", getHostPhone())
            .append("startTime", getStartTime())
            .append("endTime", getEndTime())
            .append("attendees", getAttendees())
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
