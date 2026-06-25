package com.ruoyi.system.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.DevicePort;
import com.ruoyi.system.mapper.DevicePortMapper;
import com.ruoyi.system.service.IDevicePortService;

/**
 * 设备端口 业务层处理
 *
 * @author ruoyi
 */
@Service
public class DevicePortServiceImpl implements IDevicePortService
{
    private static final Logger log = LoggerFactory.getLogger(DevicePortServiceImpl.class);

    @Autowired
    private DevicePortMapper devicePortMapper;

    /**
     * 根据条件分页查询设备端口列表
     *
     * @param port 设备端口信息
     * @return 设备端口信息集合信息
     */
    @Override
    public List<DevicePort> selectPortList(DevicePort port)
    {
        return devicePortMapper.selectPortList(port);
    }

    /**
     * 通过端口ID查询设备端口
     *
     * @param portId 端口ID
     * @return 设备端口对象信息
     */
    @Override
    public DevicePort selectPortById(Long portId)
    {
        return devicePortMapper.selectPortById(portId);
    }

    /**
     * 通过设备ID查询设备端口列表
     *
     * @param deviceId 设备ID
     * @return 设备端口集合
     */
    @Override
    public List<DevicePort> selectPortsByDeviceId(Long deviceId)
    {
        return devicePortMapper.selectPortsByDeviceId(deviceId);
    }

    /**
     * 新增设备端口信息
     *
     * @param port 设备端口信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertPort(DevicePort port)
    {
        return devicePortMapper.insertPort(port);
    }

    /**
     * 修改设备端口信息
     *
     * @param port 设备端口信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updatePort(DevicePort port)
    {
        return devicePortMapper.updatePort(port);
    }

    /**
     * 通过端口ID删除设备端口
     *
     * @param portId 端口ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deletePortById(Long portId)
    {
        return devicePortMapper.deletePortById(portId);
    }

    /**
     * 绑定两个端口
     *
     * @param portId 本端端口ID
     * @param connectedPortId 对端端口ID
     * @return 结果
     */
    @Override
    @Transactional
    public int bindPorts(Long portId, Long connectedPortId)
    {
        // 查询本端端口
        DevicePort localPort = devicePortMapper.selectPortById(portId);
        if (StringUtils.isNull(localPort))
        {
            log.warn("绑定端口失败，本端端口不存在，端口ID：{}", portId);
            return 0;
        }

        // 查询对端端口
        DevicePort remotePort = devicePortMapper.selectPortById(connectedPortId);
        if (StringUtils.isNull(remotePort))
        {
            log.warn("绑定端口失败，对端端口不存在，端口ID：{}", connectedPortId);
            return 0;
        }

        // 更新本端端口：设置连接设备ID和连接端口ID
        localPort.setConnectedDeviceId(remotePort.getDeviceId());
        localPort.setConnectedPortId(remotePort.getPortId());
        devicePortMapper.updatePort(localPort);

        // 更新对端端口：设置连接设备ID和连接端口ID
        remotePort.setConnectedDeviceId(localPort.getDeviceId());
        remotePort.setConnectedPortId(localPort.getPortId());
        devicePortMapper.updatePort(remotePort);

        log.info("端口绑定成功：端口【{}】<-> 端口【{}】", portId, connectedPortId);
        return 1;
    }
}
