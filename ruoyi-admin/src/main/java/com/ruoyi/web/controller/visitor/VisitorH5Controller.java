package com.ruoyi.web.controller.visitor;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.service.IVisitorAppointmentService;
import com.ruoyi.system.service.IVisitorLogService;

/**
 * 访客H5自助登记——公开访问，无需登录
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/visitor/h5")
public class VisitorH5Controller extends BaseController
{
    @Autowired
    private IVisitorAppointmentService visitorAppointmentService;

    @Autowired
    private IVisitorLogService visitorLogService;

    /**
     * 访客自主提交预约登记（公开接口）
     */
    @PostMapping("/submit")
    public AjaxResult submit(@Validated @RequestBody VisitorAppointment appointment)
    {
        // 校验必填字段
        if (StringUtils.isEmpty(appointment.getVisitorName()))
        {
            return error("访客姓名不能为空");
        }
        if (StringUtils.isEmpty(appointment.getVisitorPhone()))
        {
            return error("访客电话不能为空");
        }
        if (StringUtils.isEmpty(appointment.getHostName()))
        {
            return error("被访人姓名不能为空");
        }

        // 自动生成通行码
        String passCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 设置预约信息
        appointment.setPassCode(passCode);
        appointment.setStatus("PENDING");
        appointment.setCreateBy(appointment.getVisitorName());
        appointment.setCreateTime(new Date());

        // 如果未填来访时间，默认当天
        if (appointment.getVisitTime() == null)
        {
            appointment.setVisitTime(new Date());
        }

        visitorAppointmentService.insertAppointment(appointment);

        // 同时创建来访记录（自助登记即进入）
        VisitorLog log = new VisitorLog();
        log.setAppointmentId(appointment.getAppointmentId());
        log.setPassCode(passCode);
        log.setVisitorName(appointment.getVisitorName());
        log.setVisitorPhone(appointment.getVisitorPhone());
        log.setVisitorIdCard(appointment.getVisitorIdCard());
        log.setVisitorCompany(appointment.getVisitorCompany());
        log.setVisitReason(appointment.getVisitReason());
        log.setHostName(appointment.getHostName());
        log.setHostDept(appointment.getHostDept());
        log.setHasCar(appointment.getHasCar());
        log.setCarPlate(appointment.getCarPlate());
        log.setHasGoods(appointment.getHasGoods());
        log.setGoodsDesc(appointment.getGoodsDesc());
        log.setEntryTime(new Date());
        log.setRegisterType("WALKIN");
        log.setCreateBy(appointment.getVisitorName());
        log.setCreateTime(new Date());
        visitorLogService.insertLog(log);

        AjaxResult result = success(log);
        result.put("passCode", passCode);
        result.put("appointmentId", appointment.getAppointmentId());
        return result;
    }

    /**
     * 查询被访人列表（公开接口，供访客选择）
     */
    @GetMapping("/hosts")
    public AjaxResult hosts(@RequestParam(required = false) String keyword)
    {
        // 从已有的预约记录中获取不同的被访人信息
        VisitorAppointment query = new VisitorAppointment();
        List<VisitorAppointment> list = visitorAppointmentService.selectAppointmentList(query);
        // 去重后返回被访人列表
        java.util.Set<String> seen = new java.util.HashSet<>();
        java.util.List<java.util.Map<String, String>> hosts = new java.util.ArrayList<>();
        if (list != null)
        {
            for (VisitorAppointment appt : list)
            {
                String key = (appt.getHostName() != null ? appt.getHostName() : "") + "|" +
                            (appt.getHostDept() != null ? appt.getHostDept() : "");
                if (!seen.contains(key) && StringUtils.isNotEmpty(appt.getHostName()))
                {
                    seen.add(key);
                    java.util.Map<String, String> host = new java.util.HashMap<>();
                    host.put("hostName", appt.getHostName());
                    host.put("hostDept", appt.getHostDept() != null ? appt.getHostDept() : "");
                    host.put("hostPhone", appt.getHostPhone() != null ? appt.getHostPhone() : "");
                    hosts.add(host);
                }
            }
        }
        return success(hosts);
    }
}
