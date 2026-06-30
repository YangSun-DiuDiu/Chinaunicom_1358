package com.ruoyi.system.service;

import java.util.Map;

/**
 * 仪表盘 业务层
 *
 * @author ruoyi
 */
public interface IDashboardService
{
    /**
     * 获取设备仪表盘数据
     *
     * @return 设备仪表盘数据
     */
    public Map<String, Object> getDeviceDashboard();

    /**
     * 获取访客仪表盘数据
     *
     * @return 访客仪表盘数据
     */
    public Map<String, Object> getVisitorDashboard();

    /**
     * 获取会议仪表盘数据
     *
     * @return 会议仪表盘数据
     */
    public Map<String, Object> getMeetingDashboard();

    /**
     * 获取公寓仪表盘数据
     *
     * @return 公寓仪表盘数据
     */
    public Map<String, Object> getApartmentDashboard();
}
