package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 部门访客登记二维码配置表 visitor_dept_qrcode
 *
 * @author ruoyi
 */
public class VisitorDeptQrcode extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @Excel(name = "主键ID", cellType = ColumnType.NUMERIC)
    private Long id;

    /** 部门ID */
    @Excel(name = "部门ID", cellType = ColumnType.NUMERIC)
    private Long deptId;

    /** 部门名称（联表冗余字段，非表字段） */
    @Excel(name = "部门名称")
    private String deptName;

    /** 登记码(URL标识,唯一) */
    @Excel(name = "登记码")
    private String registerCode;

    /** 是否启用(0停用 1启用) */
    @Excel(name = "是否启用", readConverterExp = "0=停用,1=启用")
    private String enabled;

    /** 删除标志（0存在 2删除） */
    private String delFlag;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getRegisterCode()
    {
        return registerCode;
    }

    public void setRegisterCode(String registerCode)
    {
        this.registerCode = registerCode;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("deptId", getDeptId())
            .append("deptName", getDeptName())
            .append("registerCode", getRegisterCode())
            .append("enabled", getEnabled())
            .append("delFlag", getDelFlag())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
