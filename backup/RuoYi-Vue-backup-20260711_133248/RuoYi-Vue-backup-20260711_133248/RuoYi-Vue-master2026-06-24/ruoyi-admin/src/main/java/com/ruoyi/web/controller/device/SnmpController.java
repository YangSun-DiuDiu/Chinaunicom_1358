package com.ruoyi.web.controller.device;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.service.IDeviceService;
import com.ruoyi.system.service.impl.SnmpWalkService;

/**
 * SNMP设备管理 Controller — 基于系统 snmpwalk 命令
 */
@RestController
@RequestMapping("/device/snmp")
public class SnmpController extends BaseController
{
    @Autowired
    private SnmpWalkService snmpWalk;

    @Autowired
    private IDeviceService deviceService;

    private int port(Device d) { return d.getSnmpPort() != null ? d.getSnmpPort() : 161; }

    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @GetMapping("/info/{deviceId}")
    public AjaxResult getDeviceSnmpInfo(@PathVariable Long deviceId)
    {
        Device device = deviceService.selectDeviceById(deviceId);
        if (device == null) return error("设备不存在");
        try
        {
            Map<String, String> info = snmpWalk.getDeviceInfo(device.getIpAddress(), port(device), device.getSnmpCommunity());
            if (info.values().stream().allMatch(v -> v == null || v.isEmpty()))
                return error("无法获取SNMP信息，请检查SNMP配置或设备连通性");
            return success(info);
        }
        catch (Exception e)
        {
            return error("SNMP请求失败: " + e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @GetMapping("/ports/{deviceId}")
    public AjaxResult getDevicePortStatus(@PathVariable Long deviceId)
    {
        Device device = deviceService.selectDeviceById(deviceId);
        if (device == null) return error("设备不存在");
        try
        {
            return success(snmpWalk.getPorts(device.getIpAddress(), port(device), device.getSnmpCommunity()));
        }
        catch (Exception e)
        {
            return error("SNMP端口查询失败: " + e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @GetMapping("/nvrStorage/{deviceId}")
    public AjaxResult getNvrStorageInfo(@PathVariable Long deviceId)
    {
        Device device = deviceService.selectDeviceById(deviceId);
        if (device == null) return error("设备不存在");
        try
        {
            return success(snmpWalk.getStorageInfo(device.getIpAddress(), port(device), device.getSnmpCommunity()));
        }
        catch (Exception e)
        {
            return error("NVR存储查询失败: " + e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @GetMapping("/get/{deviceId}")
    public AjaxResult customCommand(@PathVariable Long deviceId, @RequestParam String oid)
    {
        Device device = deviceService.selectDeviceById(deviceId);
        if (device == null) return error("设备不存在");
        try
        {
            String value = snmpWalk.get(device.getIpAddress(), port(device), device.getSnmpCommunity(), oid);
            Map<String, String> result = new HashMap<>();
            result.put("oid", oid);
            result.put("value", value != null ? value : "(null)");
            return success(result);
        }
        catch (Exception e)
        {
            return error("SNMP指令执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取SNMP服务状态（当前使用的实现模式）
     */
    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @GetMapping("/status")
    public AjaxResult getSnmpStatus()
    {
        Map<String, Object> status = new HashMap<>();
        status.put("mode", snmpWalk.getModeDescription());
        status.put("snmpwalkAvailable", snmpWalk.isSnmpWalkAvailable());
        return success(status);
    }

    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @GetMapping("/walk/{deviceId}")
    public AjaxResult snmpWalk(@PathVariable Long deviceId, @RequestParam String oid)
    {
        Device device = deviceService.selectDeviceById(deviceId);
        if (device == null) return error("设备不存在");
        try
        {
            return success(snmpWalk.walk(device.getIpAddress(), port(device), device.getSnmpCommunity(), oid));
        }
        catch (Exception e)
        {
            return error("snmpwalk 执行失败: " + e.getMessage());
        }
    }
}
