package com.ruoyi.web.controller.visitor;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.service.IVisitorAppointmentService;
import com.ruoyi.system.service.IVisitorLogService;

/**
 * 访客出入记录管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/visitor/log")
public class VisitorLogController extends BaseController
{
    @Autowired
    private IVisitorLogService visitorLogService;

    @Autowired
    private IVisitorAppointmentService visitorAppointmentService;

    /**
     * 获取访客记录列表（分页）
     */
    @PreAuthorize("@ss.hasPermi('visitor:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(VisitorLog log)
    {
        startPage();
        List<VisitorLog> list = visitorLogService.selectLogList(log);
        return getDataTable(list);
    }

    /**
     * 根据记录ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('visitor:log:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable Long logId)
    {
        return success(visitorLogService.selectLogById(logId));
    }

    /**
     * 现场登记：先创建访客预约(PENDING)，审批通过后自动生成来访记录
     */
    @PreAuthorize("@ss.hasPermi('visitor:log:add')")
    @Log(title = "访客现场登记", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VisitorLog log)
    {
        // 生成通行码
        String passCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 创建访客预约记录（状态PENDING，需审批）
        VisitorAppointment appointment = new VisitorAppointment();
        appointment.setVisitorName(log.getVisitorName());
        appointment.setVisitorPhone(log.getVisitorPhone());
        appointment.setVisitorIdCard(log.getVisitorIdCard());
        appointment.setVisitorCompany(log.getVisitorCompany());
        appointment.setVisitReason(log.getVisitReason());
        appointment.setHostName(log.getHostName());
        appointment.setHostDept(log.getHostDept());
        appointment.setVisitTime(log.getEntryTime());
        appointment.setStatus("PENDING");
        appointment.setPassCode(passCode);
        appointment.setCreateBy(getUsername());
        visitorAppointmentService.insertAppointment(appointment);

        // 同时创建来访记录（登记即进入，含通行码）
        log.setPassCode(passCode);
        log.setCreateBy(getUsername());
        log.setRegisterType("WALKIN");
        visitorLogService.insertLog(log);
        return success(log);
    }

    /**
     * 记录访客离开时间
     */
    @PreAuthorize("@ss.hasPermi('visitor:log:edit')")
    @Log(title = "访客记录", businessType = BusinessType.UPDATE)
    @PutMapping("/exit/{logId}")
    public AjaxResult exit(@PathVariable Long logId)
    {
        VisitorLog log = new VisitorLog();
        log.setLogId(logId);
        log.setUpdateBy(getUsername());
        return toAjax(visitorLogService.updateLog(log));
    }

    /**
     * 导出访客记录列表
     */
    @PreAuthorize("@ss.hasPermi('visitor:log:export')")
    @Log(title = "访客记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, VisitorLog log)
    {
        List<VisitorLog> list = visitorLogService.selectLogList(log);
        ExcelUtil<VisitorLog> util = new ExcelUtil<VisitorLog>(VisitorLog.class);
        util.exportExcel(response, list, "访客记录数据");
    }
}
