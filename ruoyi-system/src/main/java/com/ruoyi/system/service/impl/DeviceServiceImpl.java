package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.DeviceRepair;
import com.ruoyi.system.domain.DeviceStatusLog;
import com.ruoyi.system.mapper.DeviceMapper;
import com.ruoyi.system.mapper.DeviceStatusLogMapper;
import com.ruoyi.system.service.IDeviceRepairService;
import com.ruoyi.system.service.IDeviceService;
import com.ruoyi.system.sms.SmsUtil;

/**
 * 设备 业务层处理
 *
 * @author ruoyi
 */
@Service
public class DeviceServiceImpl implements IDeviceService
{
    private static final Logger log = LoggerFactory.getLogger(DeviceServiceImpl.class);

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceStatusLogMapper deviceStatusLogMapper;

    @Autowired
    private SmsUtil smsUtil;

    @Lazy
    @Autowired
    private IDeviceRepairService repairService;

    /**
     * 根据条件分页查询设备列表
     *
     * @param device 设备信息
     * @return 设备信息集合信息
     */
    @Override
    public List<Device> selectDeviceList(Device device)
    {
        return deviceMapper.selectDeviceList(device);
    }

    /**
     * 通过设备ID查询设备
     *
     * @param deviceId 设备ID
     * @return 设备对象信息
     */
    @Override
    public Device selectDeviceById(Long deviceId)
    {
        return deviceMapper.selectDeviceById(deviceId);
    }

    /**
     * 新增设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertDevice(Device device)
    {
        return deviceMapper.insertDevice(device);
    }

    /**
     * 修改设备信息
     *
     * @param device 设备信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateDevice(Device device)
    {
        return deviceMapper.updateDevice(device);
    }

    /**
     * 批量删除设备信息
     *
     * @param deviceIds 需要删除的设备ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteDeviceByIds(Long[] deviceIds)
    {
        return deviceMapper.deleteDeviceByIds(deviceIds);
    }

    /**
     * 修改设备状态
     *
     * @param deviceId 设备ID
     * @param status 设备状态
     * @return 结果
     */
    @Override
    @Transactional
    public int updateDeviceStatus(Long deviceId, String status)
    {
        // 查询设备信息
        Device device = deviceMapper.selectDeviceById(deviceId);
        if (StringUtils.isNull(device))
        {
            log.warn("设备不存在，设备ID：{}", deviceId);
            return 0;
        }

        String oldStatus = device.getStatus();

        // 状态未发生变化，无需更新
        if (status.equals(oldStatus))
        {
            return 0;
        }

        // 创建设备状态变更日志
        DeviceStatusLog statusLog = new DeviceStatusLog();
        statusLog.setDeviceId(deviceId);
        statusLog.setDeviceName(device.getDeviceName());
        statusLog.setOldStatus(oldStatus);
        statusLog.setNewStatus(status);
        statusLog.setChangeTime(new Date());

        // 根据新状态确定变更类型
        if ("ONLINE".equals(status))
        {
            statusLog.setChangeType("ONLINE");
        }
        else if ("OFFLINE".equals(status))
        {
            statusLog.setChangeType("OFFLINE");
        }
        else
        {
            statusLog.setChangeType(status);
        }

        // 检测是否需要发送短信通知：离线->上线 或 上线->离线
        boolean needSms = ("OFFLINE".equals(oldStatus) && "ONLINE".equals(status))
                || ("ONLINE".equals(oldStatus) && "OFFLINE".equals(status));

        if (needSms && StringUtils.isNotEmpty(device.getResponsiblePhone()))
        {
            statusLog.setSmsSent("Y");
            statusLog.setSmsRecipient(device.getResponsiblePhone());
            log.info("设备状态变更，发送短信通知：设备【{}】状态从【{}】变为【{}】，通知电话【{}】",
                    device.getDeviceName(), oldStatus, status, device.getResponsiblePhone());
            // 调用短信服务发送通知
            if ("OFFLINE".equals(status))
            {
                // 自动创建维修工单(含短信发送)
                DeviceRepair repair = repairService.createRepairOrder(device.getDeviceId(), "SYSTEM");
                if (repair != null) {
                    smsUtil.sendSms("device_offline_alert", device.getResponsiblePhone(),
                        "{\"device_name\":\"" + device.getDeviceName()
                        + "\",\"token\":\"" + repair.getCompleteToken() + "\"}", 1, null);
                }
            }
            else if ("ONLINE".equals(status))
            {
                smsUtil.sendSms("device_online_notify", device.getResponsiblePhone(),
                    "{\"device_name\":\"" + device.getDeviceName() + "\"}", 1, null);
            }
        }
        else
        {
            statusLog.setSmsSent("N");
        }

        deviceStatusLogMapper.insertLog(statusLog);

        // 更新设备状态
        device.setStatus(status);
        return deviceMapper.updateDevice(device);
    }

    /**
     * 查询在线设备数量
     *
     * @return 结果
     */
    @Override
    public int selectOnlineCount()
    {
        return deviceMapper.selectOnlineCount();
    }

    /**
     * 查询离线设备数量
     *
     * @return 结果
     */
    @Override
    public int selectOfflineCount()
    {
        return deviceMapper.selectOfflineCount();
    }

    /**
     * 查询设备类型统计
     *
     * @return 设备类型统计信息
     */
    @Override
    public List<Map<String, Object>> selectDeviceTypeStats()
    {
        return deviceMapper.selectDeviceTypeStats();
    }

}
