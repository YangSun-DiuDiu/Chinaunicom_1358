package com.ruoyi.system.domain;

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
 * tenant_info table entity
 *
 * @author ruoyi
 */
public class TenantInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "租客ID", cellType = ColumnType.NUMERIC)
    private Long tenantId;

    @Excel(name = "姓名")
    private String tenantName;

    @Excel(name = "身份证号")
    private String idCard;

    @Excel(name = "手机号")
    private String phone;

    @Excel(name = "性别", readConverterExp = "M=男,F=女")
    private String gender;

    @Excel(name = "房间ID", cellType = ColumnType.NUMERIC)
    private Long roomId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "入住日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date checkInDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "退租日期", width = 30, dateFormat = "yyyy-MM-dd")
    private Date checkOutDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "租期开始", width = 30, dateFormat = "yyyy-MM-dd")
    private Date rentStart;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "租期结束", width = 30, dateFormat = "yyyy-MM-dd")
    private Date rentEnd;

    @Excel(name = "状态", readConverterExp = "NORMAL=在租,CHECKED_OUT=已退租")
    private String status;

    @Excel(name = "微信openid")
    private String openid;

    @Excel(name = "推送开关", readConverterExp = "0=关闭,1=开启")
    private String pushEnabled;

    public Long getTenantId()
    {
        return tenantId;
    }

    public void setTenantId(Long tenantId)
    {
        this.tenantId = tenantId;
    }

    @NotBlank(message = "姓名不能为空")
    @Size(min = 0, max = 64, message = "姓名长度不能超过64个字符")
    public String getTenantName()
    {
        return tenantName;
    }

    public void setTenantName(String tenantName)
    {
        this.tenantName = tenantName;
    }

    @Size(min = 0, max = 18, message = "身份证号长度不能超过18个字符")
    public String getIdCard()
    {
        return idCard;
    }

    public void setIdCard(String idCard)
    {
        this.idCard = idCard;
    }

    @Size(min = 0, max = 20, message = "手机号长度不能超过20个字符")
    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public Long getRoomId()
    {
        return roomId;
    }

    public void setRoomId(Long roomId)
    {
        this.roomId = roomId;
    }

    public Date getCheckInDate()
    {
        return checkInDate;
    }

    public void setCheckInDate(Date checkInDate)
    {
        this.checkInDate = checkInDate;
    }

    public Date getCheckOutDate()
    {
        return checkOutDate;
    }

    public void setCheckOutDate(Date checkOutDate)
    {
        this.checkOutDate = checkOutDate;
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

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getOpenid()
    {
        return openid;
    }

    public void setOpenid(String openid)
    {
        this.openid = openid;
    }

    public String getPushEnabled()
    {
        return pushEnabled;
    }

    public void setPushEnabled(String pushEnabled)
    {
        this.pushEnabled = pushEnabled;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("tenantId", getTenantId())
            .append("tenantName", getTenantName())
            .append("idCard", getIdCard())
            .append("phone", getPhone())
            .append("gender", getGender())
            .append("roomId", getRoomId())
            .append("checkInDate", getCheckInDate())
            .append("checkOutDate", getCheckOutDate())
            .append("rentStart", getRentStart())
            .append("rentEnd", getRentEnd())
            .append("status", getStatus())
            .append("openid", getOpenid())
            .append("pushEnabled", getPushEnabled())
            .append("deptId", getDeptId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
