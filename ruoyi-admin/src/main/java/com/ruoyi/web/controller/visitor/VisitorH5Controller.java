package com.ruoyi.web.controller.visitor;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.system.service.IVisitorAppointmentService;

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

    /**
     * 访客自主提交预约登记（公开接口）
     * 业务逻辑已下沉至 ServiceImpl.registerWalkin()
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

        // 委托 Service 完成登记（事务中创建预约+来访记录）
        Map<String, Object> resultMap = visitorAppointmentService.registerWalkin(
                appointment, appointment.getVisitorName());

        AjaxResult result = success(resultMap.get("log"));
        result.put("passCode", resultMap.get("passCode"));
        result.put("appointmentId", resultMap.get("appointmentId"));
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
