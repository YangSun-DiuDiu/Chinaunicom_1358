package com.ruoyi.web.controller.device;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.DeviceRepair;
import com.ruoyi.system.domain.DeviceStatusLog;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.IDeviceRepairService;
import com.ruoyi.system.service.IDeviceService;
import com.ruoyi.system.service.IDeviceStatusLogService;
import com.ruoyi.system.sms.SmsUtil;

/**
 * 设备故障预警 Controller
 * 展示设备离线告警记录，支持手动发送报修短信
 *
 * @author SmartIoT
 */
@RestController
@RequestMapping("/device/fault")
public class DeviceFaultController extends BaseController
{
    @Autowired
    private IDeviceStatusLogService deviceStatusLogService;

    @Autowired
    private IDeviceService deviceService;

    @Autowired
    private SmsUtil smsUtil;

    @Autowired
    private IDeviceRepairService repairService;

    @Autowired
    private SysConfigMapper configMapper;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    /**
     * 故障预警列表（展示所有离线状态变更记录）
     */
    @PreAuthorize("@ss.hasPermi('device:fault:list')")
    @GetMapping("/list")
    public TableDataInfo list(DeviceStatusLog log)
    {
        startPage();
        log.setChangeType("OFFLINE");
        List<DeviceStatusLog> list = deviceStatusLogService.selectLogList(log);
        return getDataTable(list);
    }

    /**
     * 故障统计（当前离线设备数、今日离线告警数、未处理故障数）
     */
    @PreAuthorize("@ss.hasPermi('device:fault:list')")
    @GetMapping("/stats")
    public AjaxResult stats()
    {
        Map<String, Object> result = new HashMap<>();

        Device query = new Device();
        query.setStatus("OFFLINE");
        List<Device> offlineDevices = deviceService.selectDeviceList(query);
        result.put("currentOffline", offlineDevices != null ? offlineDevices.size() : 0);

        DeviceStatusLog logQuery = new DeviceStatusLog();
        logQuery.setChangeType("OFFLINE");
        List<DeviceStatusLog> offlineLogs = deviceStatusLogService.selectLogList(logQuery);
        result.put("totalFaults", offlineLogs != null ? offlineLogs.size() : 0);

        List<Map<String, Object>> faultList = new ArrayList<>();
        if (offlineDevices != null)
        {
            for (Device d : offlineDevices)
            {
                Map<String, Object> item = new HashMap<>();
                item.put("deviceId", d.getDeviceId());
                item.put("deviceName", d.getDeviceName());
                item.put("deviceType", d.getDeviceType());
                item.put("ipAddress", d.getIpAddress());
                item.put("responsible", d.getResponsible());
                item.put("responsiblePhone", d.getResponsiblePhone());
                faultList.add(item);
            }
        }
        result.put("faultList", faultList);

        return success(result);
    }

    /**
     * 手动发送报修短信（创建维修工单 + 发送带反馈链接的短信）
     */
    @PreAuthorize("@ss.hasPermi('device:fault:repair')")
    @PostMapping("/repair/{deviceId}")
    public AjaxResult sendRepairSms(@PathVariable Long deviceId)
    {
        Device device = deviceService.selectDeviceById(deviceId);
        if (device == null)
        {
            return error("设备不存在");
        }
        if (StringUtils.isEmpty(device.getResponsiblePhone()))
        {
            return error("该设备未设置负责人电话");
        }

        // 获取服务器回调地址
        String callbackUrl = getRepairCallbackBase() + "/repair-complete";

        // 创建维修工单并生成token
        DeviceRepair repair = new DeviceRepair();
        repair.setDeviceId(deviceId);
        repair.setDeviceName(device.getDeviceName());
        repair.setDeviceIp(device.getIpAddress());
        repair.setOriginalResponsible(device.getResponsible());
        repair.setOriginalPhone(device.getResponsiblePhone());
        repair.setCurrentResponsible(device.getResponsible());
        repair.setCurrentPhone(device.getResponsiblePhone());
        repair.setFaultDescription("设备离线故障，请及时维修");
        repair.setStatus("PENDING");
        repair.setCreateBy(getUsername());
        repair.setCreateTime(new Date());
        repair.setCompleteToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        repair.setRepairNo(generateRepairNo());
        repairService.insertRepair(repair);

        // 发送带反馈链接的维修短信
        String completeUrl = callbackUrl + "?token=" + repair.getCompleteToken();
        smsUtil.sendSms("device_fault_repair", device.getResponsiblePhone(),
            "{\"device_name\":\"" + device.getDeviceName()
            + "\",\"token\":\"" + repair.getCompleteToken() + "\"}", 1, null);

        return success("报修短信已发送至 " + device.getResponsiblePhone() + "，维修工单已创建");
    }

    private String generateRepairNo() {
        String today = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        Integer count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM iot_device_repair WHERE DATE(create_time)=CURDATE()", Integer.class);
        int seq = (count != null ? count : 0) + 1;
        return today + String.format("%03d", seq);
    }

    private String getRepairCallbackBase()
    {
        try {
            SysConfig c = new SysConfig();
            c.setConfigKey("sms.repair.callback.url");
            List<SysConfig> list = configMapper.selectConfigList(c);
            if (list != null && !list.isEmpty() && StringUtils.isNotEmpty(list.get(0).getConfigValue()))
            {
                return list.get(0).getConfigValue();
            }
        } catch (Exception e) {}
        return "http://192.168.1.60";
    }

    /** @deprecated use getRepairCallbackUrl() in DeviceRepairController */
    private String getRepairCallbackUrl_DEPRECATED() { return getRepairCallbackBase() + "/repair-complete"; }
}
