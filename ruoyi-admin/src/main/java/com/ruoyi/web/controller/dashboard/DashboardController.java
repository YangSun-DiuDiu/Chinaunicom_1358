package com.ruoyi.web.controller.dashboard;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.IDashboardService;

/**
 * 仪表盘
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController extends BaseController
{
    @Autowired
    private IDashboardService dashboardService;

    /**
     * 获取设备仪表盘数据
     */
    @PreAuthorize("@ss.hasPermi('dashboard:device:query')")
    @GetMapping("/device")
    public AjaxResult device()
    {
        Map<String, Object> data = dashboardService.getDeviceDashboard();
        return success(data);
    }

    /**
     * 获取访客仪表盘数据
     */
    @PreAuthorize("@ss.hasPermi('dashboard:visitor:query')")
    @GetMapping("/visitor")
    public AjaxResult visitor()
    {
        Map<String, Object> data = dashboardService.getVisitorDashboard();
        return success(data);
    }

    /**
     * 获取会议仪表盘数据
     */
    @PreAuthorize("@ss.hasPermi('dashboard:meeting:query')")
    @GetMapping("/meeting")
    public AjaxResult meeting()
    {
        Map<String, Object> data = dashboardService.getMeetingDashboard();
        return success(data);
    }

    /**
     * 获取公寓仪表盘数据
     */
    @PreAuthorize("@ss.hasPermi('dashboard:apartment:query')")
    @GetMapping("/apartment")
    public AjaxResult apartment()
    {
        Map<String, Object> data = dashboardService.getApartmentDashboard();
        return success(data);
    }
}
