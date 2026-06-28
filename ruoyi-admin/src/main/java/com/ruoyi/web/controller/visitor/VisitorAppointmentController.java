package com.ruoyi.web.controller.visitor;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.service.IVisitorAppointmentService;
import com.ruoyi.system.service.IVisitorLogService;
import com.ruoyi.system.sms.SmsUtil;

/**
 * 访客预约管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/visitor/appointment")
public class VisitorAppointmentController extends BaseController
{
    @Autowired
    private IVisitorAppointmentService visitorAppointmentService;

    @Autowired
    private IVisitorLogService visitorLogService;

    @Autowired
    private SmsUtil smsUtil;

    /**
     * 获取预约列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:list')")
    @GetMapping("/list")
    public TableDataInfo list(VisitorAppointment appointment)
    {
        startPage();
        List<VisitorAppointment> list = visitorAppointmentService.selectAppointmentList(appointment);
        return getDataTable(list);
    }

    /**
     * 获取待审批预约列表（供审批人查看）
     */
    @PreAuthorize("@ss.hasPermi('visitor:approval:list')")
    @GetMapping("/pending")
    public AjaxResult pending()
    {
        List<VisitorAppointment> list = visitorAppointmentService.selectPendingList();
        return success(list);
    }

    /**
     * 根据预约ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:query')")
    @GetMapping(value = "/{appointmentId}")
    public AjaxResult getInfo(@PathVariable Long appointmentId)
    {
        return success(visitorAppointmentService.selectAppointmentById(appointmentId));
    }

    /**
     * 新增预约
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:add')")
    @Log(title = "访客预约", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VisitorAppointment appointment)
    {
        appointment.setCreateBy(getUsername());
        // 自动生成通行码
        if (appointment.getPassCode() == null || appointment.getPassCode().isEmpty())
        {
            appointment.setPassCode(java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        return toAjax(visitorAppointmentService.insertAppointment(appointment));
    }

    /**
     * 修改预约
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:edit')")
    @Log(title = "访客预约", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VisitorAppointment appointment)
    {
        appointment.setUpdateBy(getUsername());
        return toAjax(visitorAppointmentService.updateAppointment(appointment));
    }

    /**
     * 删除预约
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:remove')")
    @Log(title = "访客预约", businessType = BusinessType.DELETE)
    @DeleteMapping("/{appointmentIds}")
    public AjaxResult remove(@PathVariable Long[] appointmentIds)
    {
        return toAjax(visitorAppointmentService.deleteAppointmentByIds(appointmentIds));
    }

    /**
     * 审批预约（通过/拒绝，通过时发送短信通知访客）
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:approve')")
    @Log(title = "访客预约", businessType = BusinessType.UPDATE)
    @PutMapping("/approve")
    public AjaxResult approve(@RequestBody VisitorAppointment appointment)
    {
        Long appointmentId = appointment.getAppointmentId();
        String status = appointment.getStatus();
        String remark = appointment.getApproveRemark();

        if (StringUtils.isNull(appointmentId))
        {
            return error("预约ID不能为空");
        }
        if (StringUtils.isEmpty(status))
        {
            return error("审批状态不能为空");
        }

        int rows = visitorAppointmentService.approveAppointment(appointmentId, status, remark);

        // 审批通过后：创建来访记录并发送短信通知访客
        if (rows > 0 && "APPROVED".equals(status))
        {
            VisitorAppointment approved = visitorAppointmentService.selectAppointmentById(appointmentId);
            if (StringUtils.isNotNull(approved))
            {
                // 使用现有通行码，没有才生成新的（避免和创建时生成的通行码不一致）
                String passCode = approved.getPassCode();
                if (StringUtils.isEmpty(passCode))
                {
                    passCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                }

                // 检查是否已有来访记录，避免重复创建
                VisitorLog existLog = new VisitorLog();
                existLog.setAppointmentId(appointmentId);
                List<VisitorLog> existLogs = visitorLogService.selectLogList(existLog);
                boolean hasLog = (existLogs != null && !existLogs.isEmpty());

                if (!hasLog)
                {
                    // 首次审批：创建来访记录
                    VisitorLog log = new VisitorLog();
                    log.setAppointmentId(appointmentId);
                    log.setPassCode(passCode);
                    log.setVisitorName(approved.getVisitorName());
                    log.setVisitorPhone(approved.getVisitorPhone());
                    log.setVisitorIdCard(approved.getVisitorIdCard());
                    log.setVisitorCompany(approved.getVisitorCompany());
                    log.setVisitReason(approved.getVisitReason());
                    log.setHostName(approved.getHostName());
                    log.setHostDept(approved.getHostDept());
                    log.setHasCar(approved.getHasCar());
                    log.setCarPlate(approved.getCarPlate());
                    log.setHasGoods(approved.getHasGoods());
                    log.setGoodsDesc(approved.getGoodsDesc());
                    log.setEntryTime(approved.getVisitTime());
                    log.setRegisterType("APPOINTMENT");
                    if (approved.getCreateBy() != null) {
                        log.setCreateBy(approved.getCreateBy());
                    }
                    visitorLogService.insertLog(log);
                }

                // 如果通行码有变化则更新预约
                if (!passCode.equals(approved.getPassCode()))
                {
                    approved.setPassCode(passCode);
                    visitorAppointmentService.updateAppointment(approved);
                }

                // 发送短信通知（含通行码链接），仅发送一次
                if (StringUtils.isNotEmpty(approved.getVisitorPhone()))
                {
                    smsUtil.sendVisitorApprovalSms(approved);
                }
            }
        }

        return toAjax(rows);
    }

    /**
     * 取消预约
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:cancel')")
    @Log(title = "访客预约", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{appointmentId}")
    public AjaxResult cancel(@PathVariable Long appointmentId)
    {
        return toAjax(visitorAppointmentService.cancelAppointment(appointmentId));
    }

    /**
     * 完成来访（访客离开）
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:complete')")
    @Log(title = "访客预约", businessType = BusinessType.UPDATE)
    @PutMapping("/complete/{appointmentId}")
    public AjaxResult complete(@PathVariable Long appointmentId)
    {
        return toAjax(visitorAppointmentService.completeAppointment(appointmentId));
    }

    /**
     * 导出预约列表
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:export')")
    @Log(title = "访客预约", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VisitorAppointment appointment)
    {
        List<VisitorAppointment> list = visitorAppointmentService.selectAppointmentList(appointment);
        ExcelUtil<VisitorAppointment> util = new ExcelUtil<VisitorAppointment>(VisitorAppointment.class);
        util.exportExcel(response, list, "访客预约数据");
    }
}
