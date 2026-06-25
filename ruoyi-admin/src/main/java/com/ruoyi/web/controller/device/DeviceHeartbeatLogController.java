package com.ruoyi.web.controller.device;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.DeviceHeartbeatLog;
import com.ruoyi.system.service.IDeviceHeartbeatLogService;

/**
 * 设备心跳检测日志管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/device/heartbeat")
public class DeviceHeartbeatLogController extends BaseController
{
    @Autowired
    private IDeviceHeartbeatLogService deviceHeartbeatLogService;

    /**
     * 根据设备ID分页查询设备心跳日志列表
     */
    @PreAuthorize("@ss.hasPermi('device:heartbeat:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) Long deviceId)
    {
        startPage();
        DeviceHeartbeatLog log = new DeviceHeartbeatLog();
        log.setDeviceId(deviceId);
        List<DeviceHeartbeatLog> list = deviceHeartbeatLogService.selectLogList(log);
        return getDataTable(list);
    }

    /**
     * 根据日志ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('device:heartbeat:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable Long logId)
    {
        return success(deviceHeartbeatLogService.selectLogById(logId));
    }

    /**
     * 导出心跳日志
     */
    @PreAuthorize("@ss.hasPermi('device:heartbeat:export')")
    @Log(title = "心跳日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, DeviceHeartbeatLog log)
    {
        List<DeviceHeartbeatLog> list = deviceHeartbeatLogService.selectLogList(log);
        ExcelUtil<DeviceHeartbeatLog> util = new ExcelUtil<DeviceHeartbeatLog>(DeviceHeartbeatLog.class);
        util.exportExcel(response, list, "心跳日志数据");
    }
}
