package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.DevicePort;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备端口表 数据层
 *
 * @author ruoyi
 */
@Mapper
public interface DevicePortMapper
{
    /**
     * 根据条件分页查询设备端口列表
     *
     * @param port 设备端口信息
     * @return 设备端口信息集合信息
     */
    public List<DevicePort> selectPortList(DevicePort port);

    /**
     * 通过端口ID查询设备端口
     *
     * @param portId 端口ID
     * @return 设备端口对象信息
     */
    public DevicePort selectPortById(Long portId);

    /**
     * 通过设备ID查询设备端口列表
     *
     * @param deviceId 设备ID
     * @return 设备端口集合
     */
    public List<DevicePort> selectPortsByDeviceId(Long deviceId);

    /**
     * 新增设备端口信息
     *
     * @param port 设备端口信息
     * @return 结果
     */
    public int insertPort(DevicePort port);

    /**
     * 修改设备端口信息
     *
     * @param port 设备端口信息
     * @return 结果
     */
    public int updatePort(DevicePort port);

    /**
     * 通过端口ID删除设备端口
     *
     * @param portId 端口ID
     * @return 结果
     */
    public int deletePortById(Long portId);

    /**
     * 通过设备ID删除设备端口
     *
     * @param deviceId 设备ID
     * @return 结果
     */
    public int deletePortByDeviceId(Long deviceId);
}
