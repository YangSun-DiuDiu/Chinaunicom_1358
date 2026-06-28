package com.ruoyi.web.controller.device;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.DeviceRepair;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.service.IDeviceRepairService;
import com.ruoyi.system.service.IDeviceService;
import com.ruoyi.system.sms.SmsUtil;

/**
 * 设备维修工单管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/device/repair")
public class DeviceRepairController extends BaseController
{
    @Autowired
    private IDeviceRepairService repairService;
    @Autowired
    private SmsUtil smsUtil;
    @Autowired
    private IDeviceService deviceService;
    @Autowired
    private SysConfigMapper configMapper;
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('device:repair:list')")
    public TableDataInfo list(DeviceRepair repair) {
        startPage();
        return getDataTable(repairService.selectRepairList(repair));
    }

    @GetMapping("/{repairId}")
    @PreAuthorize("@ss.hasPermi('device:repair:query')")
    public AjaxResult getInfo(@PathVariable Long repairId) {
        return success(repairService.selectRepairById(repairId));
    }

    /** 转派工单 */
    @PostMapping("/transfer/{repairId}")
    @PreAuthorize("@ss.hasPermi('device:repair:transfer')")
    @Log(title = "设备维修", businessType = BusinessType.UPDATE)
    public AjaxResult transfer(@PathVariable Long repairId, @RequestBody Map<String, String> params) {
        try {
            String to = params.get("transferTo");
            String phone = params.get("transferToPhone");
            String reason = params.get("reason");
            if (StringUtils.isEmpty(to)) return error("被转派人不能为空");
            repairService.transferRepair(repairId, to, phone, reason, getUsername());
            DeviceRepair r = repairService.selectRepairById(repairId);
            if (r != null && StringUtils.isNotEmpty(phone)) {
                smsUtil.sendSms(to, phone,
                    "设备离线告警，设备：" + r.getDeviceName()
                    + "，已离线，请及时处理。设备登录码：" + (r.getCompleteToken() != null ? r.getCompleteToken() : ""),
                    "REPAIR", repairId);
            }
            return success();
        } catch (Exception e) { return error(e.getMessage()); }
    }

    /** 接收转派 */
    @PostMapping("/accept/{repairId}")
    @PreAuthorize("@ss.hasPermi('device:repair:accept')")
    @Log(title = "设备维修", businessType = BusinessType.UPDATE)
    public AjaxResult accept(@PathVariable Long repairId) {
        try { repairService.acceptRepair(repairId, getUsername()); return success(); }
        catch (Exception e) { return error(e.getMessage()); }
    }

    /** 拒绝转派 */
    @PostMapping("/reject/{repairId}")
    @PreAuthorize("@ss.hasPermi('device:repair:reject')")
    @Log(title = "设备维修", businessType = BusinessType.UPDATE)
    public AjaxResult reject(@PathVariable Long repairId, @RequestBody Map<String, String> params) {
        try { repairService.rejectRepair(repairId, params.getOrDefault("reason", ""), getUsername()); return success(); }
        catch (Exception e) { return error(e.getMessage()); }
    }

    /** 工作量统计 */
    @GetMapping("/workload")
    @PreAuthorize("@ss.hasPermi('device:repair:list')")
    public AjaxResult workload() {
        return success(repairService.selectWorkloadStats());
    }

    /** 创建设备维修工单并发送短信 */
    @PostMapping("/create/{deviceId}")
    @PreAuthorize("@ss.hasPermi('device:repair')")
    @Log(title = "设备维修", businessType = BusinessType.INSERT)
    public AjaxResult createRepair(@PathVariable Long deviceId) {
        Device device = deviceService.selectDeviceById(deviceId);
        if (device == null) return error("设备不存在");
        if (StringUtils.isEmpty(device.getResponsiblePhone())) return error("该设备未设置负责人电话");

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

        String repairToken = repair.getCompleteToken();
        smsUtil.sendSms(device.getResponsible(), device.getResponsiblePhone(),
            "设备离线告警，设备：" + device.getDeviceName()
            + "，已离线，请及时处理。设备登录码：" + repairToken,
            "REPAIR", repair.getRepairId());
        return success(repair);
    }

    @DeleteMapping("/{repairId}")
    @PreAuthorize("@ss.hasPermi('device:repair:remove')")
    @Log(title = "设备维修", businessType = BusinessType.DELETE)
    public AjaxResult remove(@PathVariable Long repairId) {
        return toAjax(repairService.deleteRepairById(repairId));
    }

    /** 生成工单编号: YYYYMMDD + 3位序号 */
    private String generateRepairNo() {
        String today = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
        // 查询今天已有工单数
        String sql = "SELECT COUNT(*) FROM iot_device_repair WHERE DATE(create_time)=CURDATE()";
        Integer count = jdbc.queryForObject(sql, Integer.class);
        int seq = (count != null ? count : 0) + 1;
        return today + String.format("%03d", seq);
    }

    private String getRepairCallbackUrl() {
        try {
            SysConfig c = new SysConfig();
            c.setConfigKey("sms.repair.callback.url");
            List<SysConfig> list = configMapper.selectConfigList(c);
            if (list != null && !list.isEmpty() && StringUtils.isNotEmpty(list.get(0).getConfigValue())) {
                String url = list.get(0).getConfigValue();
                if (!url.endsWith("/repair-complete")) url += "/repair-complete";
                return url;
            }
        } catch (Exception e) {}
        return "http://192.168.1.60/repair-complete";
    }
}
