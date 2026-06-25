package com.ruoyi.web.controller.sms;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SmsLog;
import com.ruoyi.system.mapper.SmsLogMapper;

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
    private SmsLogMapper smsLogMapper;

    /**
     * 查询短信日志列表
     */
    @PreAuthorize("@ss.hasPermi('sms:log:list')")
    @GetMapping("/list")
    public TableDataInfo list(SmsLog smsLog)
    {
        startPage();
        List<SmsLog> list = smsLogMapper.selectSmsLogList(smsLog);
        return getDataTable(list);
    }

    /**
     * 根据短信ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('sms:log:query')")
    @GetMapping("/{smsId}")
    public AjaxResult getInfo(@PathVariable Long smsId)
    {
        return success(smsLogMapper.selectSmsLogById(smsId));
    }
}
