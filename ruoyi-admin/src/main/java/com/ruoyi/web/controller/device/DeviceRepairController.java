package com.ruoyi.web.controller.device;

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
import com.ruoyi.system.domain.DeviceRepair;
import com.ruoyi.system.service.IDeviceRepairService;
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
                smsUtil.sendSms("device_repair_transfer", phone,
                    "{\"device_name\":\"" + r.getDeviceName()
                    + "\",\"token\":\"" + (r.getCompleteToken() != null ? r.getCompleteToken() : "") + "\"}", 1, null);
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
        DeviceRepair repair = repairService.createRepairOrder(deviceId, getUsername());
        if (repair == null) return error("设备不存在或未设置负责人电话");
        return success(repair);
    }

    @DeleteMapping("/{repairId}")
    @PreAuthorize("@ss.hasPermi('device:repair:remove')")
    @Log(title = "设备维修", businessType = BusinessType.DELETE)
    public AjaxResult remove(@PathVariable Long repairId) {
        return toAjax(repairService.deleteRepairById(repairId));
    }

}
