package com.ruoyi.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * apartment_info table entity
 *
 * @author ruoyi
 */
public class ApartmentInfo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "公寓ID", cellType = ColumnType.NUMERIC)
    private Long apartmentId;

    @Excel(name = "公寓名称")
    private String apartmentName;

    @Excel(name = "地址")
    private String address;

    @Excel(name = "联系电话")
    private String contactPhone;

    @Excel(name = "区域描述")
    private String areaDesc;

    public Long getApartmentId()
    {
        return apartmentId;
    }

    public void setApartmentId(Long apartmentId)
    {
        this.apartmentId = apartmentId;
    }

    @NotBlank(message = "公寓名称不能为空")
    @Size(min = 0, max = 100, message = "公寓名称长度不能超过100个字符")
    public String getApartmentName()
    {
        return apartmentName;
    }

    public void setApartmentName(String apartmentName)
    {
        this.apartmentName = apartmentName;
    }

    @Size(min = 0, max = 200, message = "地址长度不能超过200个字符")
    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    @Size(min = 0, max = 50, message = "联系电话长度不能超过50个字符")
    public String getContactPhone()
    {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone)
    {
        this.contactPhone = contactPhone;
    }

    @Size(min = 0, max = 500, message = "区域描述长度不能超过500个字符")
    public String getAreaDesc()
    {
        return areaDesc;
    }

    public void setAreaDesc(String areaDesc)
    {
        this.areaDesc = areaDesc;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("apartmentId", getApartmentId())
            .append("apartmentName", getApartmentName())
            .append("address", getAddress())
            .append("contactPhone", getContactPhone())
            .append("areaDesc", getAreaDesc())
            .append("deptId", getDeptId())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
