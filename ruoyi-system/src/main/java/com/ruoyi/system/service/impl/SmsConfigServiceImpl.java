package com.ruoyi.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.SysConfigMapper;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISmsConfigService;

/**
 * 短信配置读取服务实现——从 sys_config 表读取阿里云短信配置
 *
 * @author ruoyi
 */
@Service
public class SmsConfigServiceImpl implements ISmsConfigService
{
    private static final Logger log = LoggerFactory.getLogger(SmsConfigServiceImpl.class);

    @Autowired
    private SysConfigMapper configMapper;

    /** 配置键常量 */
    private static final String KEY_SMS_CONFIG             = "sms.config";
    private static final String KEY_SMS_ENABLED            = "sms.enabled";
    private static final String KEY_TPL_DEVICE_OFFLINE     = "sms.tpl.deviceOffline";
    private static final String KEY_TPL_DEVICE_ONLINE      = "sms.tpl.deviceOnline";
    private static final String KEY_TPL_REPAIR             = "sms.tpl.repair";
    private static final String KEY_TPL_VISITOR_APPROVAL   = "sms.tpl.visitorApproval";

    /** JSON配置缓存 */
    private volatile Map<String, Object> cachedJsonConfig;
    private volatile long lastCacheTime = 0;
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5分钟缓存

    @Override
    public boolean isSmsEnabled()
    {
        String val = getConfigValue(KEY_SMS_ENABLED);
        return !"false".equals(val);
    }

    @Override
    public String getAccessKeyId()
    {
        return getJsonConfigValue("accessKeyId");
    }

    @Override
    public String getAccessKeySecret()
    {
        return getJsonConfigValue("accessKeySecret");
    }

    @Override
    public String getSignName()
    {
        return getJsonConfigValue("signName");
    }

    @Override
    public String getRegionId()
    {
        String v = getJsonConfigValue("regionId");
        return v != null && !v.isEmpty() ? v : "cn-hangzhou";
    }

    @Override
    public String getSmsEndpoint()
    {
        String v = getJsonConfigValue("smsEndpoint");
        return v != null && !v.isEmpty() ? v : "dysmsapi.aliyuncs.com";
    }

    @Override
    public String getTemplateDeviceOffline()
    {
        return getConfigValue(KEY_TPL_DEVICE_OFFLINE);
    }

    @Override
    public String getTemplateDeviceOnline()
    {
        return getConfigValue(KEY_TPL_DEVICE_ONLINE);
    }

    @Override
    public String getTemplateRepair()
    {
        return getConfigValue(KEY_TPL_REPAIR);
    }

    @Override
    public String getTemplateVisitorApproval()
    {
        return getConfigValue(KEY_TPL_VISITOR_APPROVAL);
    }

    /**
     * 读取字符串配置值
     */
    private String getConfigValue(String configKey)
    {
        try
        {
            SysConfig config = new SysConfig();
            config.setConfigKey(configKey);
            java.util.List<SysConfig> list = configMapper.selectConfigList(config);
            if (list != null && !list.isEmpty())
            {
                return list.get(0).getConfigValue();
            }
        }
        catch (Exception e)
        {
            log.warn("读取配置失败: key={}", configKey, e);
        }
        return "";
    }

    /**
     * 读取JSON配置中的指定字段
     */
    private String getJsonConfigValue(String fieldName)
    {
        Map<String, Object> jsonCfg = getJsonConfig();
        if (jsonCfg != null)
        {
            Object val = jsonCfg.get(fieldName);
            return val != null ? val.toString() : "";
        }
        return "";
    }

    /**
     * 读取并缓存 sms.config JSON配置
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getJsonConfig()
    {
        long now = System.currentTimeMillis();
        if (cachedJsonConfig != null && (now - lastCacheTime) < CACHE_TTL)
        {
            return cachedJsonConfig;
        }
        try
        {
            String jsonStr = getConfigValue(KEY_SMS_CONFIG);
            if (jsonStr != null && !jsonStr.isEmpty() && !"null".equals(jsonStr))
            {
                ObjectMapper mapper = new ObjectMapper();
                cachedJsonConfig = mapper.readValue(jsonStr, Map.class);
            }
        }
        catch (Exception e)
        {
            log.warn("解析短信JSON配置失败", e);
            cachedJsonConfig = null;
        }
        lastCacheTime = now;
        return cachedJsonConfig;
    }
}
