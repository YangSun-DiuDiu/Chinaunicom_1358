package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.Device;
import org.apache.ibatis.annotations.Mapper;

/**
 * 物联网设备表 数据层
 *
 * @author ruoyi
 */
@Mapper
public interface DeviceMapper
{
    /**
     * 根据条件分页查询设备列表
     *
     * @param device 设备信息
     * @return 设备信息集合信息
     */
    public List<Device> selectDeviceList(Device device);

    /**
     * 通过设备ID查询设备
     *
     * @param deviceId 设备ID
     * @return 设备对象信息
     */
    public Device selectDeviceById(Long deviceId);

    /**
     * 新增设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    public int insertDevice(Device device);

    /**
     * 修改设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    public int updateDevice(Device device);

    /**
     * 通过设备ID删除设备
     *
     * @param deviceId 设备ID
     * @return 结果
     */
    public int deleteDeviceById(Long deviceId);

    /**
     * 批量删除设备信息
     *
     * @param deviceIds 需要删除的设备ID
     * @return 结果
     */
    public int deleteDeviceByIds(Long[] deviceIds);

    /**
     * 通过IP地址查询设备
     *
     * @param ipAddress IP地址
     * @return 设备对象信息
     */
    public Device selectDeviceByIp(String ipAddress);

    /**
     * 查询在线设备数量
     *
     * @return 在线设备数量
     */
    public int selectOnlineCount();

    /**
     * 查询离线设备数量
     *
     * @return 离线设备数量
     */
    public int selectOfflineCount();

    /**
     * 查询设备类型统计
     *
     * @return 设备类型统计列表
     */
    public List<Map<String, Object>> selectDeviceTypeStats();
}
