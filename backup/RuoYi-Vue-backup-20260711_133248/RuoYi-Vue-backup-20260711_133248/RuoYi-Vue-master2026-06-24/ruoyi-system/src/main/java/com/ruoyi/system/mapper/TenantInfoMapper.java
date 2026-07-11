package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.TenantInfo;

/**
 * 租客信息 数据层
 *
 * @author ruoyi
 */
public interface TenantInfoMapper
{
    public List<TenantInfo> selectTenantInfoList(TenantInfo tenantInfo);
    public TenantInfo selectTenantInfoById(Long tenantId);
    public int insertTenantInfo(TenantInfo tenantInfo);
    public int updateTenantInfo(TenantInfo tenantInfo);
    public int deleteTenantInfoById(Long tenantId);
    public int deleteTenantInfoByIds(Long[] tenantIds);
}
