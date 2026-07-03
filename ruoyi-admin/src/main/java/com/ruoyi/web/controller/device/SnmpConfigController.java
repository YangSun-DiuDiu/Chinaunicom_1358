package com.ruoyi.web.controller.device;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.mapper.DeviceMapper;
import com.ruoyi.system.service.ISnmpConfigService;
import com.ruoyi.system.service.impl.PrometheusQueryService;

/**
 * SNMP配置管理与指标查询
 */
@RestController
@RequestMapping("/device/snmp")
public class SnmpConfigController extends BaseController
{
    @Autowired
    private ISnmpConfigService snmpConfigService;

    @Autowired
    private PrometheusQueryService prometheusQueryService;

    @Autowired
    private DeviceMapper deviceMapper;

    /** 上传/部署设备SNMP配置文件 */
    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @PostMapping("/config/upload")
    public AjaxResult uploadConfig(@RequestParam("deviceId") Long deviceId,
                                    @RequestParam(value = "file", required = false) MultipartFile file,
                                    @RequestParam(value = "templateType", required = false) String templateType)
    {
        try
        {
            Device device = deviceMapper.selectDeviceById(deviceId);
            if (device == null) return error("设备不存在");
            Map<String, Object> result = snmpConfigService.deployConfig(device, file, templateType);
            if (Boolean.TRUE.equals(result.get("success")))
            {
                // 更新数据库中的SNMP配置字段
                device.setSnmpConfigFile((String) result.get("fileName"));
                device.setConfigContent((String) result.get("content"));
                device.setConfigMd5((String) result.get("md5"));
                deviceMapper.updateDevice(device);
                return success(result);
            }
            return error((String) result.get("msg"));
        }
        catch (Exception e)
        {
            return error("部署失败: " + e.getMessage());
        }
    }

    /** 删除设备SNMP配置文件 */
    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @DeleteMapping("/config/{deviceId}")
    public AjaxResult deleteConfig(@PathVariable Long deviceId)
    {
        try
        {
            Device device = deviceMapper.selectDeviceById(deviceId);
            if (device == null) return error("设备不存在");
            snmpConfigService.removeConfig(device);
            device.setSnmpConfigFile(null);
            device.setConfigContent(null);
            device.setConfigMd5(null);
            deviceMapper.updateDevice(device);
            return success();
        }
        catch (Exception e)
        {
            return error("删除失败: " + e.getMessage());
        }
    }

    /** 查询设备实时SNMP指标（从Prometheus） */
    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @GetMapping("/metrics/{deviceId}")
    public AjaxResult getMetrics(@PathVariable Long deviceId)
    {
        Device device = deviceMapper.selectDeviceById(deviceId);
        if (device == null) return error("设备不存在");
        List<Map<String, Object>> metrics = prometheusQueryService.queryDeviceMetrics(device.getIpAddress());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceId", deviceId);
        result.put("deviceName", device.getDeviceName());
        result.put("ipAddress", device.getIpAddress());
        result.put("metrics", metrics);
        return success(result);
    }

    /** Prometheus范围查询（ECharts时序曲线） */
    @PreAuthorize("@ss.hasPermi('device:snmp:query')")
    @GetMapping("/metrics/range")
    public AjaxResult rangeQuery(@RequestParam String query,
                                  @RequestParam String start,
                                  @RequestParam String end,
                                  @RequestParam(defaultValue = "60s") String step)
    {
        try
        {
            Map<String, Object> data = prometheusQueryService.rangeQuery(query, start, end, step);
            return success(data);
        }
        catch (Exception e)
        {
            return error("查询失败: " + e.getMessage());
        }
    }
}
