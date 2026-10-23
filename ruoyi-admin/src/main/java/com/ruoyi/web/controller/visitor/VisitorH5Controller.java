package com.ruoyi.web.controller.visitor;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorDeptQrcode;
import com.ruoyi.system.service.IVisitorAppointmentService;
import com.ruoyi.system.service.IVisitorDeptQrcodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 访客H5自助登记——公开访问，无需登录
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/visitor/h5")
public class VisitorH5Controller extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(VisitorH5Controller.class);

    @Autowired
    private IVisitorAppointmentService visitorAppointmentService;

    @Autowired
    private IVisitorDeptQrcodeService visitorDeptQrcodeService;

    /**
     * 访客自主提交预约登记（公开接口）
     * 业务逻辑已下沉至 ServiceImpl.registerWalkin()
     * 支持带部门登记码（registerCode）限定被访部门范围
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

        // 部门登记码模式：解析登记码对应的部门，用于提交时限定被访部门范围
        Long qrDeptId = null;
        String registerCode = appointment.getRegisterCode();
        if (StringUtils.isNotEmpty(registerCode))
        {
            VisitorDeptQrcode qrcode = visitorDeptQrcodeService.selectByCode(registerCode);
            if (qrcode == null)
            {
                return error("登记码无效或已停用，请联系管理员");
            }
            qrDeptId = qrcode.getDeptId();
        }

        // 委托 Service 完成登记（事务中创建预约+来访记录）
        Map<String, Object> resultMap = visitorAppointmentService.registerWalkin(
                appointment, appointment.getVisitorName(), qrDeptId);

        AjaxResult result = success(resultMap.get("log"));
        result.put("passCode", resultMap.get("passCode"));
        result.put("appointmentId", resultMap.get("appointmentId"));
        return result;
    }

    /**
     * 查询部门登记码对应部门信息（公开接口，H5页面加载时调用）
     *
     * @param code 部门登记码
     * @return deptId/deptName，无效或停用返回错误
     */
    @GetMapping("/info/{code}")
    public AjaxResult info(@PathVariable String code)
    {
        VisitorDeptQrcode qrcode = visitorDeptQrcodeService.selectByCode(code);
        if (qrcode == null)
        {
            return error("登记码无效或已停用，请联系管理员");
        }
        return success(qrcode);
    }

    /**
     * 查询被访人列表（公开接口，供访客选择）
     * 带 code 时按部门返回真实员工；不带 code（旧版二维码）时按历史预约记录去重
     */    @GetMapping("/hosts")
    public AjaxResult hosts(@RequestParam(required = false) String keyword, @RequestParam(required = false) String code)
    {
        // 部门登记码模式：返回该部门(含子部门)的真实在职员工
        if (StringUtils.isNotEmpty(code))
        {
            VisitorDeptQrcode qrcode = visitorDeptQrcodeService.selectByCode(code);
            if (qrcode == null)
            {
                return error("登记码无效或已停用，请联系管理员");
            }
            List<Map<String, Object>> users = visitorDeptQrcodeService.selectHostUsersByDept(qrcode.getDeptId());
            return success(users);
        }

        // 兼容旧版二维码：从已有的预约记录中获取不同的被访人信息
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

    /**
     * 保安核验摄像头扫码错误上报（公开接口，仅记录日志用于远程诊断）
     */
    @PostMapping("/camlog")
    public AjaxResult camlog(@RequestBody Map<String, Object> body)
    {
        try
        {
            log.error("[CAM-ERR] ua={} name={} msg={} stack={}",
                    body.get("ua"), body.get("name"), body.get("message"), body.get("stack"));
        }
        catch (Exception ignore) {}
        return success();
    }
}
