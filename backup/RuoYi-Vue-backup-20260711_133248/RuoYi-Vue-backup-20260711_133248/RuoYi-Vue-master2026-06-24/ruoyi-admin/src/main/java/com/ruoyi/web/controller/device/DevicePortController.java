package com.ruoyi.web.controller.device;

import java.util.List;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.DevicePort;
import com.ruoyi.system.service.IDevicePortService;

/**
 * 设备端口管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/device/port")
public class DevicePortController extends BaseController
{
    @Autowired
    private IDevicePortService devicePortService;

    /**
     * 根据设备ID查询端口列表
     */
    @PreAuthorize("@ss.hasPermi('device:port:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) Long deviceId)
    {
        if (deviceId != null)
        {
            List<DevicePort> list = devicePortService.selectPortsByDeviceId(deviceId);
            return getDataTable(list);
        }
        startPage();
        List<DevicePort> list = devicePortService.selectPortList(new DevicePort());
        return getDataTable(list);
    }

    /**
     * 根据端口ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('device:port:query')")
    @GetMapping(value = "/{portId}")
    public AjaxResult getInfo(@PathVariable Long portId)
    {
        return success(devicePortService.selectPortById(portId));
    }

    /**
     * 新增设备端口
     */
    @PreAuthorize("@ss.hasPermi('device:port:add')")
    @Log(title = "设备端口管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody DevicePort port)
    {
        port.setCreateBy(getUsername());
        return toAjax(devicePortService.insertPort(port));
    }

    /**
     * 修改设备端口
     */
    @PreAuthorize("@ss.hasPermi('device:port:edit')")
    @Log(title = "设备端口管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody DevicePort port)
    {
        port.setUpdateBy(getUsername());
        return toAjax(devicePortService.updatePort(port));
    }

    /**
     * 删除设备端口
     */
    @PreAuthorize("@ss.hasPermi('device:port:remove')")
    @Log(title = "设备端口管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{portIds}")
    public AjaxResult remove(@PathVariable Long[] portIds)
    {
        int total = 0;
        for (Long portId : portIds)
        {
            total += devicePortService.deletePortById(portId);
        }
        return toAjax(total == portIds.length);
    }

    /**
     * 绑定两个端口
     */
    @PreAuthorize("@ss.hasPermi('device:port:edit')")
    @Log(title = "设备端口管理", businessType = BusinessType.UPDATE)
    @PostMapping("/bind")
    public AjaxResult bind(@RequestParam Long portId, @RequestParam Long connectedPortId)
    {
        return toAjax(devicePortService.bindPorts(portId, connectedPortId));
    }
}
