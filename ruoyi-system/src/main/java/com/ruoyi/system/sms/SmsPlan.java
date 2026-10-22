package com.ruoyi.system.sms;

import java.util.Map;

/**
 * 短信发送计划 —— SmsResolver 解析 biz_code 的产物（发送/定时/重发共用的深接口返回值）
 *
 * <p>包含已解析的业务绑定、签名模板、渠道与驱动实例；
 * 解析失败时 {@link #isOk()} 为 false，{@link #getErrorMsg()} 为失败原因。</p>
 */
public class SmsPlan {

    /** 业务绑定行（sys_sms_biz） */
    private final Map<String, Object> biz;
    /** 签名模板行（sys_sms_sign_template） */
    private final Map<String, Object> st;
    /** 渠道行（sys_sms_channel） */
    private final Map<String, Object> channel;
    /** 渠道类型（ALIYUN/TENCENT/API） */
    private final String channelType;
    /** 已解析驱动 */
    private final SmsDriver driver;
    /** 解析失败原因；null 表示成功 */
    private final String errorMsg;

    public SmsPlan(Map<String, Object> biz, Map<String, Object> st, Map<String, Object> channel,
            String channelType, SmsDriver driver, String errorMsg) {
        this.biz = biz;
        this.st = st;
        this.channel = channel;
        this.channelType = channelType;
        this.driver = driver;
        this.errorMsg = errorMsg;
    }

    public static SmsPlan failed(String errorMsg) {
        return new SmsPlan(null, null, null, null, null, errorMsg);
    }

    public boolean isOk() {
        return errorMsg == null;
    }

    public Map<String, Object> getBiz() {
        return biz;
    }

    public Map<String, Object> getSt() {
        return st;
    }

    public Map<String, Object> getChannel() {
        return channel;
    }

    public String getChannelType() {
        return channelType;
    }

    public SmsDriver getDriver() {
        return driver;
    }

    public String getErrorMsg() {
        return errorMsg;
    }
}
