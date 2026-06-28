package com.ruoyi.web.controller.sms;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.sms.SmsUtil;

/**
 * 短信日志 Controller
 *
 * @author SmartIoT
 */
@RestController
@RequestMapping("/sms/log")
public class SmsLogController extends BaseController
{
    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SmsUtil smsUtil;

    /**
     * 查询短信日志列表
     */
    @PreAuthorize("@ss.hasPermi('sms:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String bizCode,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String taskStatus)
    {
        startPage();
        String sql = "SELECT * FROM sys_sms_log WHERE 1=1 ";
        java.util.List<Object> params = new java.util.ArrayList<>();
        if (bizCode != null && !bizCode.isEmpty())
        {
            sql += "AND biz_code=? ";
            params.add(bizCode);
        }
        if (phone != null && !phone.isEmpty())
        {
            sql += "AND phone LIKE ? ";
            params.add("%" + phone + "%");
        }
        if (taskStatus != null && !taskStatus.isEmpty())
        {
            sql += "AND task_status=? ";
            params.add(taskStatus);
        }
        sql += "ORDER BY create_time DESC";
        return getDataTable(jdbc.queryForList(sql, params.toArray()));
    }

    /**
     * 根据日志ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('sms:log:query')")
    @GetMapping("/{logId}")
    public AjaxResult getInfo(@PathVariable Long logId)
    {
        List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM sys_sms_log WHERE log_id=?", logId);
        if (list != null && !list.isEmpty())
        {
            return success(list.get(0));
        }
        return error("短信日志不存在");
    }

    /**
     * 发送短信
     */
    @PreAuthorize("@ss.hasPermi('sms:log:send')")
    @PostMapping("/send")
    public AjaxResult send(@RequestBody Map<String, Object> data)
    {
        String bizCode = data.get("bizCode") != null ? data.get("bizCode").toString() : null;
        String phone = data.get("phone") != null ? data.get("phone").toString() : null;
        String params = data.get("params") != null ? data.get("params").toString() : "{}";
        Integer sendMode = data.get("sendMode") != null
                ? Integer.parseInt(data.get("sendMode").toString()) : 1;
        String sendTime = data.get("sendTime") != null ? data.get("sendTime").toString() : null;

        if (bizCode == null || bizCode.isEmpty())
        {
            return error("业务编码不能为空");
        }
        if (phone == null || phone.isEmpty())
        {
            return error("手机号不能为空");
        }

        boolean success = smsUtil.sendSms(bizCode, phone, params, sendMode, sendTime);
        return success ? success("短信发送成功") : error("短信发送失败，请检查配置");
    }

    /**
     * 重发短信
     */
    @PreAuthorize("@ss.hasPermi('sms:log:retry')")
    @PostMapping("/retry/{logId}")
    public AjaxResult retry(@PathVariable Long logId)
    {
        boolean success = smsUtil.retry(logId);
        return success ? success("重发成功") : error("重发失败，请检查短信日志状态");
    }

    /**
     * 撤销定时短信任务
     */
    @PreAuthorize("@ss.hasPermi('sms:log:cancel')")
    @PostMapping("/cancel/{logId}")
    public AjaxResult cancel(@PathVariable Long logId)
    {
        boolean success = smsUtil.cancelScheduled(logId);
        return success ? success("撤销成功") : error("撤销失败，该任务可能已执行或不存在");
    }
}
