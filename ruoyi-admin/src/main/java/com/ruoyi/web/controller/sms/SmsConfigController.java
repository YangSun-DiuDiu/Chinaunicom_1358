package com.ruoyi.web.controller.sms;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.ISmsService;

/**
 * 短信配置管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/sms/config")
public class SmsConfigController extends BaseController
{
    @Autowired
    private ISmsService smsService;

    /**
     * 测试短信发送
     */
    @PreAuthorize("@ss.hasPermi('sms:config:edit')")
    @PostMapping("/testSend")
    public AjaxResult testSend(@RequestBody Map<String, String> params)
    {
        String phoneNumber = params.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.trim().isEmpty())
        {
            return error("手机号码不能为空");
        }
        try
        {
            smsService.sendTestSms(phoneNumber.trim());
            return success("测试短信发送成功");
        }
        catch (Exception e)
        {
            return error("测试短信发送失败：" + e.getMessage());
        }
    }
}
