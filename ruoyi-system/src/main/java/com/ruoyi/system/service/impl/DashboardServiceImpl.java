package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.DeviceHeartbeatLog;
import com.ruoyi.system.domain.DeviceStatusLog;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.mapper.DeviceHeartbeatLogMapper;
import com.ruoyi.system.mapper.DeviceMapper;
import com.ruoyi.system.mapper.DeviceStatusLogMapper;
import com.ruoyi.system.mapper.VisitorAppointmentMapper;
import com.ruoyi.system.mapper.VisitorLogMapper;
import com.ruoyi.system.service.IDashboardService;

/**
 * 仪表盘 业务层处理
 *
 * @author ruoyi
 */
@Service
public class DashboardServiceImpl implements IDashboardService
{
    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    @Autowired
    private DeviceMapper deviceMapper;

    @Autowired
    private DeviceStatusLogMapper deviceStatusLogMapper;

    @Autowired
    private DeviceHeartbeatLogMapper deviceHeartbeatLogMapper;

    @Autowired
    private VisitorAppointmentMapper visitorAppointmentMapper;

    @Autowired
    private VisitorLogMapper visitorLogMapper;

    /**
     * 获取设备仪表盘数据
     *
     * @return 设备仪表盘数据
     */
    @Override
    public Map<String, Object> getDeviceDashboard()
    {
        Map<String, Object> result = new LinkedHashMap<>();

        // 查询所有设备
        List<Device> allDevices = deviceMapper.selectDeviceList(new Device());

        // 统计在线/离线/总数
        long totalCount = allDevices.size();
        long onlineCount = allDevices.stream().filter(d -> "ONLINE".equals(d.getStatus())).count();
        long offlineCount = allDevices.stream().filter(d -> "OFFLINE".equals(d.getStatus())).count();
        BigDecimal onlineRate = BigDecimal.ZERO;
        if (totalCount > 0)
        {
            onlineRate = BigDecimal.valueOf(onlineCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCount), 2, RoundingMode.HALF_UP);
        }

        result.put("totalCount", totalCount);
        result.put("onlineCount", onlineCount);
        result.put("offlineCount", offlineCount);
        result.put("onlineRate", onlineRate);

        // 设备类型统计
        Map<String, Long> typeCountMap = allDevices.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDeviceType() != null ? d.getDeviceType() : "UNKNOWN",
                        Collectors.counting()
                ));
        List<Map<String, Object>> deviceTypeStats = new ArrayList<>();
        for (Map.Entry<String, Long> entry : typeCountMap.entrySet())
        {
            Map<String, Object> typeStat = new HashMap<>();
            typeStat.put("type", entry.getKey());
            typeStat.put("count", entry.getValue());
            deviceTypeStats.add(typeStat);
        }
        result.put("deviceTypeStats", deviceTypeStats);

        // 最新10条状态变更日志
        List<DeviceStatusLog> allStatusLogs = deviceStatusLogMapper.selectLogList(new DeviceStatusLog());
        List<DeviceStatusLog> recentAlerts = allStatusLogs.stream()
                .limit(10)
                .collect(Collectors.toList());
        result.put("recentAlerts", recentAlerts);

        // 最新10条心跳日志
        List<DeviceHeartbeatLog> allHeartbeatLogs = deviceHeartbeatLogMapper.selectLogList(new DeviceHeartbeatLog());
        List<DeviceHeartbeatLog> recentHeartbeats = allHeartbeatLogs.stream()
                .limit(10)
                .collect(Collectors.toList());
        result.put("recentHeartbeats", recentHeartbeats);

        return result;
    }

    /**
     * 获取访客仪表盘数据
     *
     * @return 访客仪表盘数据
     */
    @Override
    public Map<String, Object> getVisitorDashboard()
    {
        Map<String, Object> result = new LinkedHashMap<>();

        // 今日开始和结束时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String todayStr = sdf.format(new Date());
        String todayBegin = todayStr + " 00:00:00";
        String todayEnd = todayStr + " 23:59:59";

        // 查询今日访客记录
        VisitorLog todayQuery = new VisitorLog();
        todayQuery.getParams().put("beginTime", todayBegin);
        todayQuery.getParams().put("endTime", todayEnd);
        List<VisitorLog> todayLogs = visitorLogMapper.selectLogList(todayQuery);
        long todayVisitors = todayLogs.size();

        // 今日进入数量（有entryTime且为今天的记录）
        long todayEntryCount = todayLogs.stream()
                .filter(l -> l.getEntryTime() != null)
                .count();

        result.put("todayVisitors", todayVisitors);
        result.put("todayEntryCount", todayEntryCount);

        // 待审批预约数
        List<VisitorAppointment> pendingList = visitorAppointmentMapper.selectPendingList();
        long pendingApprovals = pendingList.size();
        result.put("pendingApprovals", pendingApprovals);

        // 预约总数
        List<VisitorAppointment> allAppointments = visitorAppointmentMapper.selectAppointmentList(new VisitorAppointment());
        long totalAppointments = allAppointments.size();
        result.put("totalAppointments", totalAppointments);

        // 最新10条访客记录
        List<VisitorLog> allLogs = visitorLogMapper.selectLogList(new VisitorLog());
        List<VisitorLog> recentVisits = allLogs.stream()
                .limit(10)
                .collect(Collectors.toList());
        result.put("recentVisits", recentVisits);

        // 按预约状态统计
        Map<String, Long> statusCountMap = allAppointments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus() != null ? a.getStatus() : "UNKNOWN",
                        Collectors.counting()
                ));
        List<Map<String, Object>> visitStats = new ArrayList<>();
        for (Map.Entry<String, Long> entry : statusCountMap.entrySet())
        {
            Map<String, Object> stat = new HashMap<>();
            stat.put("status", entry.getKey());
            stat.put("count", entry.getValue());
            visitStats.add(stat);
        }
        result.put("visitStats", visitStats);

        return result;
    }
}
