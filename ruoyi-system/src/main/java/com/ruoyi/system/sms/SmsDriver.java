package com.ruoyi.system.sms;

import java.util.Map;

/**
 * 短信驱动接口——多厂商SMS统一抽象
 *
 * @author ruoyi
 */
public interface SmsDriver
{
    /**
     * 发送短信
     *
     * @param channel  渠道配置（sys_sms_channel 行数据）
     * @param template 签名模板（sys_sms_sign_template 行数据）
     * @param phone    手机号码
     * @param params   模板参数（JSON字符串）
     * @return true成功 false失败
     */
    boolean send(Map<String, Object> channel, Map<String, Object> template, String phone, String params);
}
