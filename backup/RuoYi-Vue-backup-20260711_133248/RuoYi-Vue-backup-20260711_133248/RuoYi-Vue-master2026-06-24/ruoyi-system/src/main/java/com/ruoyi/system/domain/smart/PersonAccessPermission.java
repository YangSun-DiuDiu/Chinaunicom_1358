package com.ruoyi.system.domain.smart;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

public class PersonAccessPermission extends BaseEntity {
    private Long permId;
    private String targetType;
    private Long targetId;
    private String targetName;
    private String deviceIds;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    private String status;
    public Long getPermId() { return permId; }
    public void setPermId(Long permId) { this.permId = permId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getDeviceIds() { return deviceIds; }
    public void setDeviceIds(String deviceIds) { this.deviceIds = deviceIds; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
