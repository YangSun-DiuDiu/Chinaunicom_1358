package com.ruoyi.system.service.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.SmsLog;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.mapper.SmsLogMapper;
import com.ruoyi.system.service.ISmsConfigService;
import com.ruoyi.system.service.ISmsService;

/**
 * 短信服务层实现——支持阿里云短信服务
 *
 * @author ruoyi
 */
@Service
public class SmsServiceImpl implements ISmsService
{
    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    @Autowired
    private SmsLogMapper smsLogMapper;

    @Autowired
    private ISmsConfigService smsConfigService;

    /** 业务类型常量 */
    private static final String BIZ_TYPE_DEVICE_OFFLINE = "DEVICE_OFFLINE";
    private static final String BIZ_TYPE_DEVICE_ONLINE  = "DEVICE_ONLINE";
    private static final String BIZ_TYPE_REPAIR         = "REPAIR";
    private static final String BIZ_TYPE_VISITOR_APPROVE = "VISITOR_APPROVE";
    private static final String BIZ_TYPE_TEST           = "TEST";

    /** 发送结果常量 */
    private static final String SEND_RESULT_SUCCESS = "SUCCESS";
    private static final String SEND_RESULT_FAIL    = "FAIL";

    /** 阿里云短信API域名 */
    private static final String ALIYUN_SMS_ENDPOINT = "dysmsapi.aliyuncs.com";
    private static final String ALIYUN_SMS_VERSION = "2017-05-25";
    private static final String ALIYUN_SMS_ACTION = "SendSms";

    /**
     * 发送短信并记录到短信日志表
     */
    @Override
    public void sendSms(String recipient, String phoneNumber, String content, String bizType, Long bizId)
    {
        SmsLog smsLog = new SmsLog();
        smsLog.setRecipient(recipient);
        smsLog.setPhoneNumber(phoneNumber);
        smsLog.setContent(content);
        smsLog.setBizType(bizType);
        smsLog.setBizId(bizId);
        smsLog.setSendTime(new Date());

        try
        {
            // 尝试阿里云短信发送
            if (smsConfigService.isSmsEnabled())
            {
                callAliyunSmsApi(phoneNumber, content, bizType);
            }
            smsLog.setSendResult(SEND_RESULT_SUCCESS);
        }
        catch (Exception e)
        {
            smsLog.setSendResult(SEND_RESULT_FAIL);
            log.error("短信发送失败: recipient={}, phone={}, bizType={}", recipient, phoneNumber, bizType, e);
        }
        finally
        {
            // 始终记录日志
            try
            {
                smsLogMapper.insertSmsLog(smsLog);
            }
            catch (Exception ex)
            {
                log.error("短信日志写入失败", ex);
            }
        }

        log.info("【短信发送】收件人: {}, 手机号: {}, 业务类型: {}, 发送结果: {}",
                recipient, phoneNumber, bizType, smsLog.getSendResult());
    }

    /**
     * 发送设备离线告警短信
     */
    @Override
    public void sendDeviceOfflineAlert(Device device)
    {
        String content = buildSmsContent("DEVICE_OFFLINE",
                "【设备离线告警】设备「" + safeStr(device.getDeviceName()) + "」（IP: " + safeStr(device.getIpAddress())
                + "，位置: " + safeStr(device.getLocation()) + "）已离线，请及时排查处理。");
        sendSms(device.getResponsible(), device.getResponsiblePhone(), content,
                BIZ_TYPE_DEVICE_OFFLINE, device.getDeviceId());
    }

    /**
     * 发送设备上线恢复短信
     */
    @Override
    public void sendDeviceOnlineAlert(Device device)
    {
        String content = buildSmsContent("DEVICE_ONLINE",
                "【设备上线通知】设备「" + safeStr(device.getDeviceName()) + "」（IP: " + safeStr(device.getIpAddress())
                + "，位置: " + safeStr(device.getLocation()) + "）已恢复正常在线状态。");
        sendSms(device.getResponsible(), device.getResponsiblePhone(), content,
                BIZ_TYPE_DEVICE_ONLINE, device.getDeviceId());
    }

    /**
     * 发送设备维修通知短信
     */
    @Override
    public void sendRepairAlert(Device device)
    {
        // 此方法不再直接发送维修短信 — 改由 DeviceRepairController.createRepair 统一发送
        // 保留方法以防其他调用方
        String content = "设备离线告警，设备：" + safeStr(device.getDeviceName())
                + "，已离线，请及时处理。";
        sendSms(device.getResponsible(), device.getResponsiblePhone(), content,
                BIZ_TYPE_REPAIR, device.getDeviceId());
    }

    /**
     * 发送访客审批通过短信
     */
    @Override
    public void sendVisitorApprovalSms(VisitorAppointment appointment)
    {
        String passCode = appointment.getPassCode() != null ? appointment.getPassCode() : "";
        String content = buildSmsContent("VISITOR_APPROVE",
                "【访客审批通知】" + safeStr(appointment.getVisitorName()) + "您好，欢迎来访！"
                + "被访人：" + safeStr(appointment.getHostName())
                + "（" + safeStr(appointment.getHostDept()) + "），"
                + "通行码：" + passCode
                + "，通行链接：http://1.94.26.126:3000/pass/" + passCode);
        sendSms(appointment.getVisitorName(), appointment.getVisitorPhone(), content,
                BIZ_TYPE_VISITOR_APPROVE, appointment.getAppointmentId());
    }

    /**
     * 发送测试短信
     */
    @Override
    public void sendTestSms(String phoneNumber)
    {
        if (!smsConfigService.isSmsEnabled())
        {
            throw new RuntimeException("短信功能未开启，请先在短信配置中开启");
        }
        String content = buildSmsContent("TEST",
                "【智慧园区系统】测试短信，验证码：" + (int)(Math.random() * 9000 + 1000) + "，您正在测试短信发送功能。");
        sendSms("测试用户", phoneNumber, content, BIZ_TYPE_TEST, null);
    }

    /**
     * 构建短信内容（使用配置的模板变量替换）
     */
    private String buildSmsContent(String bizType, String defaultContent)
    {
        // 阿里云短信使用模板CODE+模板参数，defaultContent作为备用
        return defaultContent;
    }

    /**
     * 调用阿里云短信API
     */
    private void callAliyunSmsApi(String phoneNumber, String templateParam, String bizType) throws Exception
    {
        String accessKeyId = smsConfigService.getAccessKeyId();
        String accessKeySecret = smsConfigService.getAccessKeySecret();
        String signName = smsConfigService.getSignName();
        String templateCode = getTemplateCode(bizType);
        String regionId = smsConfigService.getRegionId();
        String endpoint = smsConfigService.getSmsEndpoint();

        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)
                || StringUtils.isEmpty(signName) || StringUtils.isEmpty(templateCode))
        {
            log.info("阿里云短信配置不完整 (缺少 AccessKey/SignName/TemplateCode)，使用模拟发送模式");
            return;
        }

        if (StringUtils.isEmpty(endpoint))
        {
            endpoint = ALIYUN_SMS_ENDPOINT;
        }

        // 构建请求参数
        TreeMap<String, String> params = new TreeMap<>();
        params.put("PhoneNumbers", phoneNumber);
        params.put("SignName", signName);
        params.put("TemplateCode", templateCode);
        // 将模板内容序列化为JSON格式的TemplateParam
        params.put("TemplateParam", "{\"content\":\"" + escapeJson(templateParam) + "\"}");
        params.put("AccessKeyId", accessKeyId);
        params.put("Action", ALIYUN_SMS_ACTION);
        params.put("Version", ALIYUN_SMS_VERSION);
        params.put("SignatureMethod", "HMAC-SHA1");
        params.put("SignatureVersion", "1.0");
        params.put("Timestamp", getTimestamp());
        params.put("Format", "JSON");
        params.put("SignatureNonce", generateNonce());
        params.put("RegionId", StringUtils.isEmpty(regionId) ? "cn-hangzhou" : regionId);

        // 计算签名
        String signature = sign(params, accessKeySecret);
        params.put("Signature", signature);

        // 发送HTTP POST请求
        String urlStr = "https://" + endpoint;
        String postData = buildQueryString(params, false);

        log.debug("阿里云短信API请求: endpoint={}, phone={}, sign={}", endpoint, phoneNumber, signName);

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        try (OutputStream os = conn.getOutputStream())
        {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

        int statusCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        statusCode == 200 ? conn.getInputStream() : conn.getErrorStream(),
                        StandardCharsets.UTF_8)))
        {
            String line;
            while ((line = br.readLine()) != null)
            {
                response.append(line);
            }
        }

        log.info("阿里云短信API响应: status={}, body={}", statusCode, response);

        if (statusCode != 200)
        {
            throw new RuntimeException("阿里云短信API返回状态码: " + statusCode + ", 响应: " + response);
        }

        // 解析响应JSON
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> respMap = mapper.readValue(response.toString(), Map.class);
        String code = (String) respMap.get("Code");
        if (!"OK".equals(code))
        {
            throw new RuntimeException("阿里云短信发送失败: " + respMap.get("Message"));
        }
    }

    /**
     * 根据业务类型获取模板CODE
     */
    private String getTemplateCode(String bizType)
    {
        switch (bizType)
        {
            case BIZ_TYPE_DEVICE_OFFLINE:
                return smsConfigService.getTemplateDeviceOffline();
            case BIZ_TYPE_DEVICE_ONLINE:
                return smsConfigService.getTemplateDeviceOnline();
            case BIZ_TYPE_REPAIR:
                return smsConfigService.getTemplateRepair();
            case BIZ_TYPE_VISITOR_APPROVE:
                return smsConfigService.getTemplateVisitorApproval();
            case BIZ_TYPE_TEST:
                // 测试发送使用设备上线模板（任意有效模板即可）
                String tpl = smsConfigService.getTemplateDeviceOnline();
                return StringUtils.isNotEmpty(tpl) ? tpl : smsConfigService.getTemplateDeviceOffline();
            default:
                return "";
        }
    }

    // ===== 阿里云签名计算 =====

    /**
     * 计算HMAC-SHA1签名
     */
    private String sign(TreeMap<String, String> params, String accessKeySecret) throws Exception
    {
        String queryString = buildQueryString(params, true);
        String stringToSign = "POST&" + percentEncode("/") + "&" + percentEncode(queryString);
        String key = accessKeySecret + "&";
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
        byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
        return base64Encode(signData);
    }

    private String buildQueryString(TreeMap<String, String> params, boolean forSign)
    {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet())
        {
            if ("Signature".equals(entry.getKey()))
            {
                continue;
            }
            if (sb.length() > 0)
            {
                sb.append("&");
            }
            sb.append(percentEncode(entry.getKey()));
            sb.append("=");
            sb.append(forSign ? percentEncode(entry.getValue()) : urlEncode(entry.getValue()));
        }
        return sb.toString();
    }

    private String percentEncode(String value)
    {
        if (value == null) return "";
        try
        {
            String encoded = URLEncoder.encode(value, "UTF-8")
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~");
            return encoded;
        }
        catch (Exception e)
        {
            return value;
        }
    }

    private String urlEncode(String value)
    {
        if (value == null) return "";
        try
        {
            return URLEncoder.encode(value, "UTF-8");
        }
        catch (Exception e)
        {
            return value;
        }
    }

    private String getTimestamp()
    {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(new Date());
    }

    private String generateNonce()
    {
        StringBuilder sb = new StringBuilder();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < 32; i++)
        {
            sb.append(chars.charAt((int)(Math.random() * chars.length())));
        }
        return sb.toString();
    }

    private String base64Encode(byte[] data)
    {
        return java.util.Base64.getEncoder().encodeToString(data);
    }

    private String escapeJson(String str)
    {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    private String safeStr(String str)
    {
        return str != null ? str : "未知";
    }
}
