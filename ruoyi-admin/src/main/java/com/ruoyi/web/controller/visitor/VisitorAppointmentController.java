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
     * 园区管理员（roleKey=smartparkAdmin）：可见本部门及以下全部访客数据。
     * 部门范围由 DeptScopeInterceptor 统一强制（本部门+子部门），
     * 此处仅对园区管理员放开「仅本人审批人」的限制。
     */
    private boolean isParkAdmin()
    {
        return SecurityUtils.hasRole("smartparkAdmin");
    }

    /**
     * 获取预约列表（分页）
     * 非管理员（园区管理员除外）仅返回本人为审批人的预约记录
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:list')")
    @GetMapping("/list")
    public TableDataInfo list(VisitorAppointment appointment)
    {
        startPage();
        // 非管理员（园区管理员除外）只能查看自己是审批人（被访人）的预约
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()) && !isParkAdmin())
        {
            appointment.setApproverId(SecurityUtils.getUserId());
        }
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
        // 非admin角色（园区管理员除外）按审批人过滤
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()) && !isParkAdmin())
        {
            approverId = SecurityUtils.getUserId();
        }
        List<VisitorAppointment> list = visitorAppointmentService.selectPendingList(approverId);
        return success(list);
    }

    /**
     * 根据预约ID获取详细信息
     * 非管理员仅能查看本人为审批人的预约
     */
    @PreAuthorize("@ss.hasPermi('visitor:appointment:query')")
    @GetMapping(value = "/{appointmentId}")
    public AjaxResult getInfo(@PathVariable Long appointmentId)
    {
        VisitorAppointment info = visitorAppointmentService.selectAppointmentById(appointmentId);
        if (info == null)
        {
            return error("预约信息不存在");
        }
        // 非管理员校验：园区管理员可按部门范围查看本部门及以下预约，其余只能查看自己是审批人的预约
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()) && !isParkAdmin()
                && (info.getApproverId() == null || !info.getApproverId().equals(SecurityUtils.getUserId())))
        {
            return error("无权查看该预约");
        }
        return success(info);
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
        // 通行码不在新增时生成：预约提交后为待审批状态，审批通过时由 approveAppointment 统一生成
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
        // 非管理员（园区管理员除外）只能导出本人为审批人的预约
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()) && !isParkAdmin())
        {
            appointment.setApproverId(SecurityUtils.getUserId());
        }
        List<VisitorAppointment> list = visitorAppointmentService.selectAppointmentList(appointment);
        ExcelUtil<VisitorAppointment> util = new ExcelUtil<VisitorAppointment>(VisitorAppointment.class);
        util.exportExcel(response, list, "访客预约数据");
    }
}
