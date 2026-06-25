package com.ruoyi.quartz.task;

import java.net.InetAddress;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.PingUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.DeviceHeartbeatLog;
import com.ruoyi.system.service.IDeviceHeartbeatLogService;
import com.ruoyi.system.service.IDeviceService;
import com.ruoyi.system.service.ISmsService;

/**
 * 设备心跳检测定时任务——覆盖设备管理+智能化管理全部设备
 */
@Component("deviceHeartbeatTask")
public class DeviceHeartbeatTask
{
    private static final Logger log = LoggerFactory.getLogger(DeviceHeartbeatTask.class);
    private static final int PING_TIMEOUT = 3000;
    private static final String ONLINE = "ONLINE";
    private static final String OFFLINE = "OFFLINE";

    @Autowired private IDeviceService deviceService;
    @Autowired private IDeviceHeartbeatLogService heartbeatLogService;
    @Autowired private ISmsService smsService;
    @Autowired private JdbcTemplate jdbc;

    /** 任务入口 */
    public void heartbeatCheck()
    {
        long start = System.currentTimeMillis();
        int[] counts = new int[4]; // [detected, online, offline, changed]

        // 1. 设备管理-设备列表巡检
        checkIotDevices(counts);

        // 2. 智能化管理-人员通行设备巡检
        checkSmartDevices("iot_person_access_device", "人员通行", counts);

        // 3. 智能化管理-车辆通行设备巡检
        checkSmartDevices("iot_vehicle_access_device", "车辆通行", counts);

        // 4. 智能化管理-监控设备巡检
        checkSmartDevices("iot_video_device", "视频监控", counts);

        long elapsed = System.currentTimeMillis() - start;
        log.info("设备巡检完成: 检测{}台 在线{}台 离线{}台 变更{}台 耗时{}ms",
            counts[0], counts[1], counts[2], counts[3], elapsed);
    }

    /** 巡检 iot_device 表设备 */
    private void checkIotDevices(int[] c)
    {
        Device query = new Device();
        List<Device> devices = deviceService.selectDeviceList(query);
        if (devices == null) return;

        for (Device d : devices)
        {
            try
            {
                String ip = d.getIpAddress();
                if (StringUtils.isEmpty(ip)) continue;

                String oldStatus = d.getStatus();
                if (StringUtils.isEmpty(oldStatus)) oldStatus = "UNKNOWN";

                PingUtil.PingResult pr = PingUtil.ping(ip, PING_TIMEOUT);
                String newStatus = pr.reachable ? ONLINE : OFFLINE;

                // 记录心跳日志
                DeviceHeartbeatLog hb = new DeviceHeartbeatLog();
                hb.setDeviceId(d.getDeviceId());
                hb.setDeviceName(d.getDeviceName());
                hb.setIpAddress(ip);
                hb.setPingResult(pr.reachable ? "SUCCESS" : "FAIL");
                hb.setPingLatency(pr.latency);
                hb.setDetectTime(new Date());
                hb.setCreateTime(new Date());
                heartbeatLogService.insertLog(hb);

                c[0]++; c[pr.reachable ? 1 : 2]++;

                if (!newStatus.equals(oldStatus))
                {
                    log.info("设备[{}]状态变更: {} -> {}", d.getDeviceName(), oldStatus, newStatus);
                    deviceService.updateDeviceStatus(d.getDeviceId(), newStatus);
                    // 设备离线时发送告警短信
                    if (OFFLINE.equals(newStatus)) {
                        try { smsService.sendDeviceOfflineAlert(d); }
                        catch (Exception ex) { log.error("发送离线告警短信失败: {}", d.getDeviceName(), ex); }
                    }
                    c[3]++;
                }
            }
            catch (Exception e) { log.error("设备[{}]巡检异常: {}", d.getDeviceName(), e.getMessage()); }
        }
    }

    /** 巡检智能化管理设备 */
    private void checkSmartDevices(String table, String label, int[] c)
    {
        List<java.util.Map<String, Object>> devices;
        try
        {
            devices = jdbc.queryForList(
                "SELECT device_id, device_name, ip_address, status, IFNULL(ping_interval,3) as ping_interval " +
                "FROM " + table + " WHERE ip_address IS NOT NULL AND ip_address != ''");
        }
        catch (Exception e) { return; }

        for (java.util.Map<String, Object> row : devices)
        {
            try
            {
                Long id = ((Number) row.get("device_id")).longValue();
                String name = String.valueOf(row.getOrDefault("device_name", ""));
                String ip = String.valueOf(row.getOrDefault("ip_address", ""));
                String oldStatus = String.valueOf(row.getOrDefault("status", "UNKNOWN"));
                int interval = ((Number) row.getOrDefault("ping_interval", 3)).intValue();

                // 按巡检周期判断是否需要检测（上次检测时间+周期 < 现在）
                // 简化处理：每次都检测，由Quartz调度频率控制
                PingUtil.PingResult pr = PingUtil.ping(ip, PING_TIMEOUT);
                String newStatus = pr.reachable ? ONLINE : OFFLINE;

                c[0]++; c[pr.reachable ? 1 : 2]++;

                if (!newStatus.equals(oldStatus))
                {
                    log.info("{}设备[{}]状态变更: {} -> {}", label, name, oldStatus, newStatus);
                    jdbc.update("UPDATE " + table + " SET status=? WHERE device_id=?", newStatus, id);
                    c[3]++;
                }
            }
            catch (Exception e) { log.error("{}设备巡检异常: {}", label, e.getMessage()); }
        }
    }
}
