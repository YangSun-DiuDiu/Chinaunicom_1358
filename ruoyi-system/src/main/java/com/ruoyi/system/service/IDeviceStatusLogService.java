package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.DeviceStatusLog;

/**
 * 设备状态变更日志 服务层
 *
 * @author ruoyi
 */
public interface IDeviceStatusLogService
{
    /**
     * 根据条件分页查询设备状态日志列表
     *
     * @param log 设备状态日志信息
     * @return 设备状态日志集合
     */
    public List<DeviceStatusLog> selectLogList(DeviceStatusLog log);

    /**
     * 通过日志ID查询设备状态日志
     *
     * @param logId 日志ID
     * @return 设备状态日志对象信息
     */
    public DeviceStatusLog selectLogById(Long logId);

    /**
     * 新增设备状态日志
     *
     * @param log 设备状态日志信息
     * @return 结果
     */
    public int insertLog(DeviceStatusLog log);

    /**
     * 通过设备ID查询设备状态日志列表
     *
     * @param deviceId 设备ID
     * @return 设备状态日志集合
     */
    public List<DeviceStatusLog> selectLogByDeviceId(Long deviceId);
}
