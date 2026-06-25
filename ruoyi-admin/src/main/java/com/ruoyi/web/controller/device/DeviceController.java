package com.ruoyi.web.controller.device;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.service.IDeviceService;
import com.ruoyi.system.service.ISmsService;

/**
 * 设备管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/device")
public class DeviceController extends BaseController
{
    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private ISmsService smsService;

    /**
     * 获取设备列表
     */
    @PreAuthorize("@ss.hasPermi('device:list')")
    @GetMapping("/list")
    public TableDataInfo list(Device device)
    {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }

    /**
     * 获取在线设备数量
     */
    @PreAuthorize("@ss.hasPermi('device:query')")
    @GetMapping("/onlineCount")
    public AjaxResult onlineCount()
    {
        return success(deviceService.selectOnlineCount());
    }

    /**
     * 获取离线设备数量
     */
    @PreAuthorize("@ss.hasPermi('device:query')")
    @GetMapping("/offlineCount")
    public AjaxResult offlineCount()
    {
        return success(deviceService.selectOfflineCount());
    }

    /**
     * 获取设备类型统计
     */
    @PreAuthorize("@ss.hasPermi('device:query')")
    @GetMapping("/deviceTypeStats")
    public AjaxResult deviceTypeStats()
    {
        return success(deviceService.selectDeviceTypeStats());
    }

    /**
     * 根据设备编号获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('device:query')")
    @GetMapping(value = "/{deviceId}")
    public AjaxResult getInfo(@PathVariable Long deviceId)
    {
        return success(deviceService.selectDeviceById(deviceId));
    }

    /**
     * 新增设备
     */
    @PreAuthorize("@ss.hasPermi('device:add')")
    @Log(title = "设备管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Device device)
    {
        device.setCreateBy(getUsername());
        return toAjax(deviceService.insertDevice(device));
    }

    /**
     * 修改设备
     */
    @PreAuthorize("@ss.hasPermi('device:edit')")
    @Log(title = "设备管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody Device device)
    {
        device.setUpdateBy(getUsername());
        return toAjax(deviceService.updateDevice(device));
    }

    /**
     * 删除设备
     */
    @PreAuthorize("@ss.hasPermi('device:remove')")
    @Log(title = "设备管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deviceIds}")
    public AjaxResult remove(@PathVariable Long[] deviceIds)
    {
        return toAjax(deviceService.deleteDeviceByIds(deviceIds));
    }

    /**
     * 导出设备
     */
    @PreAuthorize("@ss.hasPermi('device:export')")
    @Log(title = "设备管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, Device device)
    {
        List<Device> list = deviceService.selectDeviceList(device);
        ExcelUtil<Device> util = new ExcelUtil<Device>(Device.class);
        util.exportExcel(response, list, "设备数据");
    }

    /**
     * 报修-发送短信
     */
    @PreAuthorize("@ss.hasPermi('device:repair')")
    @Log(title = "设备管理", businessType = BusinessType.OTHER)
    @PostMapping("/repair/{deviceId}")
    public AjaxResult repair(@PathVariable Long deviceId)
    {
        Device device = deviceService.selectDeviceById(deviceId);
        if (StringUtils.isNull(device))
        {
            return error("设备不存在");
        }
        if (StringUtils.isEmpty(device.getResponsiblePhone()))
        {
            return error("设备负责人电话为空，无法发送维修短信");
        }
        smsService.sendRepairAlert(device);
        return success();
    }

    /**
     * 获取设备拓扑树
     */
    @PreAuthorize("@ss.hasPermi('device:topology:query')")
    @GetMapping("/topology")
    public AjaxResult topology()
    {
        List<Device> allDevices = deviceService.selectDeviceList(new Device());
        List<Map<String, Object>> tree = buildTopologyTree(allDevices, 0L);
        return success(tree);
    }

    /**
     * 递归构建设备拓扑树结构
     *
     * @param devices  所有设备列表
     * @param parentId 父设备ID
     * @return 拓扑树节点列表
     */
    private List<Map<String, Object>> buildTopologyTree(List<Device> devices, Long parentId)
    {
        List<Map<String, Object>> tree = new ArrayList<>();
        for (Device device : devices)
        {
            Long deviceParentId = device.getParentId() == null ? 0L : device.getParentId();
            if (deviceParentId.equals(parentId))
            {
                Map<String, Object> node = new HashMap<>();
                node.put("id", device.getDeviceId());
                node.put("label", device.getDeviceName());
                node.put("deviceType", device.getDeviceType());
                node.put("status", device.getStatus());
                node.put("ipAddress", device.getIpAddress());
                List<Map<String, Object>> children = buildTopologyTree(devices, device.getDeviceId());
                node.put("children", children);
                tree.add(node);
            }
        }
        return tree;
    }
}
