package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.ApartmentInfo;
import com.ruoyi.system.mapper.ApartmentInfoMapper;
import com.ruoyi.system.service.IApartmentInfoService;

/**
 * 公寓信息 服务层实现
 *
 * @author ruoyi
 */
@Service
public class ApartmentInfoServiceImpl implements IApartmentInfoService
{
    @Autowired
    private ApartmentInfoMapper apartmentInfoMapper;

    /**
     * 查询公寓列表
     *
     * @param apartmentInfo 公寓信息
     * @return 公寓集合
     */
    @Override
    public List<ApartmentInfo> selectApartmentInfoList(ApartmentInfo apartmentInfo)
    {
        return apartmentInfoMapper.selectApartmentInfoList(apartmentInfo);
    }

    /**
     * 根据公寓ID查询信息
     *
     * @param apartmentId 公寓ID
     * @return 公寓信息
     */
    @Override
    public ApartmentInfo selectApartmentInfoById(Long apartmentId)
    {
        return apartmentInfoMapper.selectApartmentInfoById(apartmentId);
    }

    /**
     * 新增公寓
     *
     * @param apartmentInfo 公寓信息
     * @return 结果
     */
    @Override
    public int insertApartmentInfo(ApartmentInfo apartmentInfo)
    {
        return apartmentInfoMapper.insertApartmentInfo(apartmentInfo);
    }

    /**
     * 修改公寓
     *
     * @param apartmentInfo 公寓信息
     * @return 结果
     */
    @Override
    public int updateApartmentInfo(ApartmentInfo apartmentInfo)
    {
        return apartmentInfoMapper.updateApartmentInfo(apartmentInfo);
    }

    /**
     * 批量删除公寓
     *
     * @param apartmentIds 需要删除的公寓ID
     */
    @Override
    public void deleteApartmentInfoByIds(Long[] apartmentIds)
    {
        for (Long apartmentId : apartmentIds)
        {
            apartmentInfoMapper.deleteApartmentInfoById(apartmentId);
        }
    }
}
