package com.ruoyi.system.sms;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 腾讯云短信驱动（待接入——当前为桩实现）
 *
 * @author ruoyi
 */
public class TencentSmsDriver implements SmsDriver
{
    private static final Logger log = LoggerFactory.getLogger(TencentSmsDriver.class);

    @Override
    public boolean send(Map<String, Object> channel, Map<String, Object> template, String phone, String params)
    {
        log.warn("Tencent SMS not yet configured: phone={}, channel={}",
                phone, channel != null ? channel.get("channel_name") : "null");
        return false;
    }
}
