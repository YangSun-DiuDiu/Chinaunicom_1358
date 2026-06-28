package com.ruoyi.system.sms;

import java.util.Map;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.utils.StringUtils;

/**
 * 阿里云短信驱动——基于 aliyun-java-sdk-dysmsapi
 *
 * @author ruoyi
 */
public class AliyunSmsDriver implements SmsDriver
{
    private static final Logger log = LoggerFactory.getLogger(AliyunSmsDriver.class);

    private final String accessKeyId;
    private final String accessKeySecret;

    public AliyunSmsDriver(String accessKeyId, String accessKeySecret)
    {
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
    }

    @Override
    public boolean send(Map<String, Object> channel, Map<String, Object> template, String phone, String params)
    {
        String signName = getString(template, "sign_name");
        String templateCode = getString(template, "template_code");
        String regionId = "cn-hangzhou";

        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)
                || StringUtils.isEmpty(signName) || StringUtils.isEmpty(templateCode))
        {
            log.warn("阿里云短信配置不完整: accessKeyId={}, signName={}, templateCode={}",
                    accessKeyId != null && !accessKeyId.isEmpty(), signName, templateCode);
            return false;
        }

        try
        {
            DefaultProfile profile = DefaultProfile.getProfile(regionId, accessKeyId, accessKeySecret);
            IAcsClient client = new DefaultAcsClient(profile);

            SendSmsRequest request = new SendSmsRequest();
            request.setPhoneNumbers(phone);
            request.setSignName(signName);
            request.setTemplateCode(templateCode);
            request.setTemplateParam(params);

            SendSmsResponse response = client.getAcsResponse(request);
            log.info("阿里云短信发送: phone={}, code={}, message={}, bizId={}",
                    phone, response.getCode(), response.getMessage(), response.getBizId());

            if (!"OK".equals(response.getCode()))
            {
                log.error("阿里云短信发送失败: code={}, message={}", response.getCode(), response.getMessage());
                return false;
            }
            return true;
        }
        catch (ClientException e)
        {
            log.error("阿里云短信SDK调用异常: phone={}", phone, e);
            return false;
        }
    }

    private String getString(Map<String, Object> map, String key)
    {
        Object val = map != null ? map.get(key) : null;
        return val != null ? val.toString() : "";
    }
}
