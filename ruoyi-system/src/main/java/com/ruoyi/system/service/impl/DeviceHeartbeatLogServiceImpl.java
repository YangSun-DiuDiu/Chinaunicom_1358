package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.DeviceHeartbeatLog;
import com.ruoyi.system.mapper.DeviceHeartbeatLogMapper;
import com.ruoyi.system.service.IDeviceHeartbeatLogService;

/**
 * 设备心跳检测日志 服务层处理
 *
 * @author ruoyi
 */
@Service
public class DeviceHeartbeatLogServiceImpl implements IDeviceHeartbeatLogService
{
    @Autowired
    private DeviceHeartbeatLogMapper deviceHeartbeatLogMapper;

    /**
     * 根据条件分页查询设备心跳日志列表
     *
     * @param log 设备心跳日志信息
     * @return 设备心跳日志集合
     */
    @Override
    public List<DeviceHeartbeatLog> selectLogList(DeviceHeartbeatLog log)
    {
        return deviceHeartbeatLogMapper.selectLogList(log);
    }

    /**
     * 通过日志ID查询设备心跳日志
     *
     * @param logId 日志ID
     * @return 设备心跳日志对象信息
     */
    @Override
    public DeviceHeartbeatLog selectLogById(Long logId)
    {
        return deviceHeartbeatLogMapper.selectLogById(logId);
    }

    /**
     * 新增设备心跳日志
     *
     * @param log 设备心跳日志信息
     * @return 结果
     */
    @Override
    public int insertLog(DeviceHeartbeatLog log)
    {
        return deviceHeartbeatLogMapper.insertLog(log);
    }

    /**
     * 通过设备ID删除设备心跳日志
     *
     * @param deviceId 设备ID
     * @return 结果
     */
    @Override
    public int deleteLogByDeviceId(Long deviceId)
    {
        return deviceHeartbeatLogMapper.deleteLogByDeviceId(deviceId);
    }
}
