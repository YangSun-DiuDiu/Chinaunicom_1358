package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.DeviceStatusLog;
import com.ruoyi.system.mapper.DeviceStatusLogMapper;
import com.ruoyi.system.service.IDeviceStatusLogService;

/**
 * 设备状态变更日志 服务层处理
 *
 * @author ruoyi
 */
@Service
public class DeviceStatusLogServiceImpl implements IDeviceStatusLogService
{
    @Autowired
    private DeviceStatusLogMapper deviceStatusLogMapper;

    /**
     * 根据条件分页查询设备状态日志列表
     *
     * @param log 设备状态日志信息
     * @return 设备状态日志集合
     */
    @Override
    public List<DeviceStatusLog> selectLogList(DeviceStatusLog log)
    {
        return deviceStatusLogMapper.selectLogList(log);
    }

    /**
     * 通过日志ID查询设备状态日志
     *
     * @param logId 日志ID
     * @return 设备状态日志对象信息
     */
    @Override
    public DeviceStatusLog selectLogById(Long logId)
    {
        return deviceStatusLogMapper.selectLogById(logId);
    }

    /**
     * 新增设备状态日志
     *
     * @param log 设备状态日志信息
     * @return 结果
     */
    @Override
    public int insertLog(DeviceStatusLog log)
    {
        return deviceStatusLogMapper.insertLog(log);
    }

    /**
     * 通过设备ID查询设备状态日志列表
     *
     * @param deviceId 设备ID
     * @return 设备状态日志集合
     */
    @Override
    public List<DeviceStatusLog> selectLogByDeviceId(Long deviceId)
    {
        return deviceStatusLogMapper.selectLogByDeviceId(deviceId);
    }
}
