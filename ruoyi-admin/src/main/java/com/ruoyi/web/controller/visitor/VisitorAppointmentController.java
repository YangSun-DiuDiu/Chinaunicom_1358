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
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.service.IVisitorAppointmentService;

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
     * 非管理员角色只返回指定审批人为当前用户的记录
     */
    @PreAuthorize("@ss.hasPermi('visitor:approval:list')")
    @GetMapping("/pending")
    public AjaxResult pending()
    {
        Long approverId = null;
        // 非admin角色按审批人过滤
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()))
        {
            approverId = SecurityUtils.getUserId();
        }
        List<VisitorAppointment> list = visitorAppointmentService.selectPendingList(approverId);
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
     * 审批预约（通过/拒绝）
     * 审批后处理（创建来访记录、发送短信）已下沉至 ServiceImpl.approveAppointment()
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

        return toAjax(visitorAppointmentService.approveAppointment(appointmentId, status, remark));
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
