package com.ruoyi.web.controller.visitor;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.service.IVisitorAppointmentService;
import com.ruoyi.system.service.IVisitorLogService;

/**
 * 访客通行码公开访问 Controller（无需登录）
 *
 * @author SmartIoT
 */
@RestController
@RequestMapping("/pass")
public class VisitorPassController extends BaseController
{
    @Autowired
    private IVisitorAppointmentService visitorAppointmentService;

    @Autowired
    private IVisitorLogService visitorLogService;

    /**
     * 根据通行码获取访客通行信息（公开接口，无需认证）
     * 验证同时更新预约状态为 VISITING（到访确认）
     */
    @GetMapping("/{passCode}")
    public AjaxResult getPassInfo(@PathVariable String passCode)
    {
        // 从预约表查找
        VisitorAppointment apt = visitorAppointmentService.selectAppointmentByPassCode(passCode);
        if (apt != null)
        {
            Map<String, Object> info = new HashMap<>();
            info.put("passCode", passCode);
            info.put("visitorName", apt.getVisitorName());
            info.put("visitorPhone", apt.getVisitorPhone());
            info.put("visitorCompany", apt.getVisitorCompany());
            info.put("hostName", apt.getHostName());
            info.put("hostDept", apt.getHostDept());
            info.put("visitTime", apt.getVisitTime());
            info.put("status", apt.getStatus());

            // 到访确认：更新状态为 VISITING
            visitorAppointmentService.checkInVisitor(passCode);

            return success(info);
        }

        // 从未访记录表查找
        VisitorLog log = visitorLogService.selectLogByPassCode(passCode);
        if (log != null)
        {
            Map<String, Object> info = new HashMap<>();
            info.put("passCode", passCode);
            info.put("visitorName", log.getVisitorName());
            info.put("visitorPhone", log.getVisitorPhone());
            info.put("visitorCompany", log.getVisitorCompany());
            info.put("hostName", log.getHostName());
            info.put("hostDept", log.getHostDept());
            info.put("entryTime", log.getEntryTime());
            info.put("status", "VISITING");
            return success(info);
        }

        return error("通行码无效或已过期");
    }
}
