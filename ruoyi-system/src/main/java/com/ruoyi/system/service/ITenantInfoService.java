package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.TenantInfo;

/**
 * 租客信息 服务层
 *
 * @author ruoyi
 */
public interface ITenantInfoService
{
    public List<TenantInfo> selectTenantInfoList(TenantInfo tenantInfo);
    public TenantInfo selectTenantInfoById(Long tenantId);
    public int insertTenantInfo(TenantInfo tenantInfo);
    public int updateTenantInfo(TenantInfo tenantInfo);
    public void deleteTenantInfoByIds(Long[] tenantIds);
}
