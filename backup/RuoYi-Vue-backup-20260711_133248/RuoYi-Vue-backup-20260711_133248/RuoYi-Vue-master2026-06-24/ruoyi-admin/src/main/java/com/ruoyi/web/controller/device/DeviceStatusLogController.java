package com.ruoyi.web.controller.device;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.DeviceStatusLog;
import com.ruoyi.system.service.IDeviceStatusLogService;

/**
 * 设备状态变更日志管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/device/statuslog")
public class DeviceStatusLogController extends BaseController
{
    @Autowired
    private IDeviceStatusLogService deviceStatusLogService;

    /**
     * 根据设备ID分页查询设备状态日志列表
     */
    @PreAuthorize("@ss.hasPermi('device:statuslog:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) Long deviceId)
    {
        startPage();
        DeviceStatusLog log = new DeviceStatusLog();
        log.setDeviceId(deviceId);
        List<DeviceStatusLog> list = deviceStatusLogService.selectLogList(log);
        return getDataTable(list);
    }

    /**
     * 根据日志ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('device:statuslog:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable Long logId)
    {
        return success(deviceStatusLogService.selectLogById(logId));
    }
}
