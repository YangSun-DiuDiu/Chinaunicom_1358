package com.ruoyi.web.controller.visitor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.domain.vo.PassInfoDTO;
import com.ruoyi.system.service.IVisitorAppointmentService;
import com.ruoyi.system.service.IVisitorLogService;

/**
 * 访客通行码公开访问 Controller（无需登录）
 *
 * <p>读写分离：
 *   GET  /pass/{passCode}            —— 纯读，返回通行信息（不改变任何状态）
 *   POST /pass/{passCode}/checkin    —— 显式到访确认（状态 → VISITING）</p>
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
     * 根据通行码获取访客通行信息（公开接口，无需认证，纯读）
     */
    @GetMapping("/{passCode}")
    public AjaxResult getPassInfo(@PathVariable String passCode)
    {
        PassInfoDTO info = buildPassInfo(passCode);
        if (info == null)
        {
            return error("通行码无效或已过期");
        }
        return success(info);
    }

    /**
     * 按访客手机号查询预约概要列表（公开接口，无需认证，纯读）
     *
     * <p>供访客在门卫处出示通行码：输入手机号 → 列出该手机号的预约 →
     * 选择某条预约展示二维码。仅返回展示字段，不含证件信息。</p>
     */
    @GetMapping("/list")
    public AjaxResult listByPhone(String phone)
    {
        if (phone == null || phone.trim().isEmpty())
        {
            return error("请输入访客手机号");
        }
        return success(visitorAppointmentService.selectAppointmentBriefByPhone(phone.trim()));
    }

    /**
     * 到访确认（保安核验）：状态 → VISITING
     */
    @PostMapping("/{passCode}/checkin")
    public AjaxResult checkIn(@PathVariable String passCode)
    {
        VisitorAppointment appointment = visitorAppointmentService.checkInVisitor(passCode);
        if (appointment == null)
        {
            return error("通行码无效或已过期");
        }
        PassInfoDTO info = buildPassInfo(passCode);
        if (info == null)
        {
            info = new PassInfoDTO();
            info.setPassCode(passCode);
            info.setVisitorName(appointment.getVisitorName());
            info.setVisitorPhone(appointment.getVisitorPhone());
            info.setVisitorCompany(appointment.getVisitorCompany());
            info.setHostName(appointment.getHostName());
            info.setHostDept(appointment.getHostDept());
            info.setVisitTime(appointment.getVisitTime());
        }
        info.setStatus(appointment.getStatus());
        return success(info);
    }

    /**
     * 构建通行信息：预约表优先，其次来访记录表
     */
    private PassInfoDTO buildPassInfo(String passCode)
    {
        // 从预约表查找
        VisitorAppointment apt = visitorAppointmentService.selectAppointmentByPassCode(passCode);
        if (apt != null)
        {
            PassInfoDTO info = new PassInfoDTO();
            info.setPassCode(passCode);
            info.setVisitorName(apt.getVisitorName());
            info.setVisitorPhone(apt.getVisitorPhone());
            info.setVisitorCompany(apt.getVisitorCompany());
            info.setHostName(apt.getHostName());
            info.setHostDept(apt.getHostDept());
            info.setVisitTime(apt.getVisitTime());
            info.setEntryTime(null);
            info.setStatus(apt.getStatus());
            return info;
        }

        // 从未访记录表查找
        VisitorLog log = visitorLogService.selectLogByPassCode(passCode);
        if (log != null)
        {
            PassInfoDTO info = new PassInfoDTO();
            info.setPassCode(passCode);
            info.setVisitorName(log.getVisitorName());
            info.setVisitorPhone(log.getVisitorPhone());
            info.setVisitorCompany(log.getVisitorCompany());
            info.setHostName(log.getHostName());
            info.setHostDept(log.getHostDept());
            info.setVisitTime(null);
            info.setEntryTime(log.getEntryTime());
            info.setStatus(log.getExitTime() != null ? "COMPLETED" : "VISITING");
            return info;
        }

        return null;
    }
}
