package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.DeviceHeartbeatLog;

/**
 * 设备心跳检测日志 服务层
 *
 * @author ruoyi
 */
public interface IDeviceHeartbeatLogService
{
    /**
     * 根据条件分页查询设备心跳日志列表
     *
     * @param log 设备心跳日志信息
     * @return 设备心跳日志集合
     */
    public List<DeviceHeartbeatLog> selectLogList(DeviceHeartbeatLog log);

    /**
     * 通过日志ID查询设备心跳日志
     *
     * @param logId 日志ID
     * @return 设备心跳日志对象信息
     */
    public DeviceHeartbeatLog selectLogById(Long logId);

    /**
     * 新增设备心跳日志
     *
     * @param log 设备心跳日志信息
     * @return 结果
     */
    public int insertLog(DeviceHeartbeatLog log);

    /**
     * 通过设备ID删除设备心跳日志
     *
     * @param deviceId 设备ID
     * @return 结果
     */
    public int deleteLogByDeviceId(Long deviceId);
}
