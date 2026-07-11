package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.TenantInfo;
import com.ruoyi.system.mapper.TenantInfoMapper;
import com.ruoyi.system.service.ITenantInfoService;

/**
 * 租客信息 服务层实现
 *
 * @author ruoyi
 */
@Service
public class TenantInfoServiceImpl implements ITenantInfoService
{
    @Autowired
    private TenantInfoMapper tenantInfoMapper;

    @Override
    public List<TenantInfo> selectTenantInfoList(TenantInfo tenantInfo)
    {
        return tenantInfoMapper.selectTenantInfoList(tenantInfo);
    }

    @Override
    public TenantInfo selectTenantInfoById(Long tenantId)
    {
        return tenantInfoMapper.selectTenantInfoById(tenantId);
    }

    @Override
    public int insertTenantInfo(TenantInfo tenantInfo)
    {
        return tenantInfoMapper.insertTenantInfo(tenantInfo);
    }

    @Override
    public int updateTenantInfo(TenantInfo tenantInfo)
    {
        return tenantInfoMapper.updateTenantInfo(tenantInfo);
    }

    @Override
    public void deleteTenantInfoByIds(Long[] tenantIds)
    {
        for (Long tenantId : tenantIds)
        {
            tenantInfoMapper.deleteTenantInfoById(tenantId);
        }
    }
}
