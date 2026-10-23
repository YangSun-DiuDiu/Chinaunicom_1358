package com.ruoyi.web.controller.visitor;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.common.utils.SecurityUtils;
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
     * 园区管理员（roleKey=smartparkAdmin）：可见本部门及以下全部访客记录。
     * 部门范围由 DeptScopeInterceptor 统一强制（本部门+子部门），
     * 此处仅对园区管理员放开「仅本人被访人」的限制。
     */
    private boolean isParkAdmin()
    {
        return SecurityUtils.hasRole("smartparkAdmin");
    }

    /**
     * 现场登记：创建预约(PENDING)+来访记录(WALKIN)，含通行码
     * 业务逻辑已下沉至 ServiceImpl.registerWalkin()
     */
    @PreAuthorize("@ss.hasAnyPermi('visitor:register:add,visitor:log:add')")
    @Log(title = "访客现场登记", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VisitorLog log)
    {
        // 构建预约对象
        VisitorAppointment appointment = new VisitorAppointment();
        appointment.setVisitorName(log.getVisitorName());
        appointment.setVisitorPhone(log.getVisitorPhone());
        appointment.setVisitorIdCard(log.getVisitorIdCard());
        appointment.setVisitorCompany(log.getVisitorCompany());
        appointment.setVisitReason(log.getVisitReason());
        appointment.setHostName(log.getHostName());
        appointment.setHostDept(log.getHostDept());
        appointment.setVisitTime(log.getEntryTime());
        appointment.setHasCar(log.getHasCar());
        appointment.setCarPlate(log.getCarPlate());
        appointment.setHasGoods(log.getHasGoods());
        appointment.setGoodsDesc(log.getGoodsDesc());

        // 委托 Service 完成登记
        Map<String, Object> resultMap = visitorAppointmentService.registerWalkin(
                appointment, getUsername());

        return success(resultMap.get("log"));
    }

    /**
     * 获取访客记录列表（分页）
     * 非管理员仅返回本人为被访人的记录
     */
    @PreAuthorize("@ss.hasPermi('visitor:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(VisitorLog log)
    {
        startPage();
        // 非管理员（园区管理员除外）只能查看本人是被访人的记录
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()) && !isParkAdmin())
        {
            String nickName = SecurityUtils.getLoginUser().getUser().getNickName();
            log.getParams().put("securityHostName", nickName != null ? nickName : SecurityUtils.getUsername());
        }
        List<VisitorLog> list = visitorLogService.selectLogList(log);
        return getDataTable(list);
    }

    /**
     * 根据记录ID获取详细信息
     * 非管理员仅能查看本人为被访人的记录
     */
    @PreAuthorize("@ss.hasPermi('visitor:log:query')")
    @GetMapping(value = "/{logId}")
    public AjaxResult getInfo(@PathVariable Long logId)
    {
        VisitorLog info = visitorLogService.selectLogById(logId);
        if (info == null)
        {
            return error("记录信息不存在");
        }
        // 非管理员校验：园区管理员可按部门范围查看本部门及以下记录，其余只能查看本人是被访人的记录
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()) && !isParkAdmin())
        {
            String nickName = SecurityUtils.getLoginUser().getUser().getNickName();
            String selfName = nickName != null ? nickName : SecurityUtils.getUsername();
            if (info.getHostName() == null || !info.getHostName().equals(selfName))
            {
                return error("无权查看该记录");
            }
        }
        return success(info);
    }

    /**
     * 记录访客离开时间
     */
    @PreAuthorize("@ss.hasPermi('visitor:log:exit')")
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
        // 非管理员（园区管理员除外）只能导出本人是被访人的记录
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()) && !isParkAdmin())
        {
            String nickName = SecurityUtils.getLoginUser().getUser().getNickName();
            log.getParams().put("securityHostName", nickName != null ? nickName : SecurityUtils.getUsername());
        }
        List<VisitorLog> list = visitorLogService.selectLogList(log);
        ExcelUtil<VisitorLog> util = new ExcelUtil<VisitorLog>(VisitorLog.class);
        util.exportExcel(response, list, "访客记录数据");
    }
}
