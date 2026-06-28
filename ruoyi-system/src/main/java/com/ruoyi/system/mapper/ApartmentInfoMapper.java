package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.ApartmentInfo;

/**
 * 公寓信息 数据层
 *
 * @author ruoyi
 */
public interface ApartmentInfoMapper
{
    /**
     * 查询公寓列表
     *
     * @param apartmentInfo 公寓信息
     * @return 公寓集合
     */
    public List<ApartmentInfo> selectApartmentInfoList(ApartmentInfo apartmentInfo);

    /**
     * 根据公寓ID查询信息
     *
     * @param apartmentId 公寓ID
     * @return 公寓信息
     */
    public ApartmentInfo selectApartmentInfoById(Long apartmentId);

    /**
     * 新增公寓
     *
     * @param apartmentInfo 公寓信息
     * @return 结果
     */
    public int insertApartmentInfo(ApartmentInfo apartmentInfo);

    /**
     * 修改公寓
     *
     * @param apartmentInfo 公寓信息
     * @return 结果
     */
    public int updateApartmentInfo(ApartmentInfo apartmentInfo);

    /**
     * 删除公寓
     *
     * @param apartmentId 公寓ID
     * @return 结果
     */
    public int deleteApartmentInfoById(Long apartmentId);

    /**
     * 批量删除公寓
     *
     * @param apartmentIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteApartmentInfoByIds(Long[] apartmentIds);
}
