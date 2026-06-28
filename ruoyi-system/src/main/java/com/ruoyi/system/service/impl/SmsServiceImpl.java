package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.Map;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
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
public class SmsServiceImpl implements ISmsService {
    private static final Logger log = LoggerFactory.getLogger(SmsServiceImpl.class);

    @Autowired
    private SmsLogMapper smsLogMapper;

    @Autowired
    private ISmsConfigService smsConfigService;

    /** 业务类型常量 */
    private static final String BIZ_TYPE_DEVICE_OFFLINE = "DEVICE_OFFLINE";
    private static final String BIZ_TYPE_DEVICE_ONLINE = "DEVICE_ONLINE";
    private static final String BIZ_TYPE_REPAIR = "REPAIR";
    private static final String BIZ_TYPE_VISITOR_APPROVE = "VISITOR_APPROVE";
    private static final String BIZ_TYPE_TEST = "TEST";

    /** 发送结果常量 */
    private static final String SEND_RESULT_SUCCESS = "SUCCESS";
    private static final String SEND_RESULT_FAIL = "FAIL";

    /**
     * 发送短信并记录到短信日志表
     */
    @Override
    public void sendSms(String recipient, String phoneNumber, String content, String bizType, Long bizId) {
        SmsLog smsLog = new SmsLog();
        smsLog.setRecipient(recipient);
        smsLog.setPhoneNumber(phoneNumber);
        smsLog.setContent(content);
        smsLog.setBizType(bizType);
        smsLog.setBizId(bizId);
        smsLog.setSendTime(new Date());

        try {
            // 尝试阿里云短信发送
            if (smsConfigService.isSmsEnabled()) {
                callAliyunSmsApi(phoneNumber, content, bizType);
            }
            smsLog.setSendResult(SEND_RESULT_SUCCESS);
        } catch (Exception e) {
            smsLog.setSendResult(SEND_RESULT_FAIL);
            log.error("短信发送失败: recipient={}, phone={}, bizType={}", recipient, phoneNumber, bizType, e);
        } finally {
            // 始终记录日志
            try {
                smsLogMapper.insertSmsLog(smsLog);
            } catch (Exception ex) {
                log.error("短信日志写入失败", ex);
            }
        }

        log.info("短信发送：收件人: {}, 手机号: {}, 业务类型: {}, 发送结果: {}",
                recipient, phoneNumber, bizType, smsLog.getSendResult());
    }

    /**
     * 发送设备离线告警短信
     */
    @Override
    public void sendDeviceOfflineAlert(Device device) {
        String content = buildSmsContent("DEVICE_OFFLINE",
                "设备离线告警：设备「" + safeStr(device.getDeviceName()) + "」（IP: " + safeStr(device.getIpAddress())
                        + "，位置: " + safeStr(device.getLocation()) + "）已离线，请及时排查处理。");
        sendSms(device.getResponsible(), device.getResponsiblePhone(), content,
                BIZ_TYPE_DEVICE_OFFLINE, device.getDeviceId());
    }

    /**
     * 发送设备上线恢复短信
     */
    @Override
    public void sendDeviceOnlineAlert(Device device) {
        String content = buildSmsContent("DEVICE_ONLINE",
                "设备上线通知：设备「" + safeStr(device.getDeviceName()) + "」（IP: " + safeStr(device.getIpAddress())
                        + "，位置: " + safeStr(device.getLocation()) + "）已恢复正常在线状态。");
        sendSms(device.getResponsible(), device.getResponsiblePhone(), content,
                BIZ_TYPE_DEVICE_ONLINE, device.getDeviceId());
    }

    /**
     * 发送设备维修通知短信
     */
    @Override
    public void sendRepairAlert(Device device) {
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
    public void sendVisitorApprovalSms(VisitorAppointment appointment) {
        String passCode = appointment.getPassCode() != null ? appointment.getPassCode() : "";
        String content = buildSmsContent("VISITOR_APPROVE",
                "访客审批通知：" + safeStr(appointment.getVisitorName()) + "您好，欢迎来访！"
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
    public void sendTestSms(String phoneNumber) {
        if (!smsConfigService.isSmsEnabled()) {
            throw new RuntimeException("短信功能未开启，请先在短信配置中开启");
        }
        String content = buildSmsContent("TEST",
                "测试短信，验证码：" + (int) (Math.random() * 9000 + 1000) + "，您正在测试短信发送功能。");
        sendSms("测试用户", phoneNumber, content, BIZ_TYPE_TEST, null);
    }

    /**
     * 构建短信内容（使用配置的模板变量替换）
     */
    private String buildSmsContent(String bizType, String defaultContent) {
        // 阿里云短信使用模板CODE+模板参数，defaultContent作为备用
        return defaultContent;
    }

    /**
     * 调用阿里云短信SDK发送短信
     */
    private void callAliyunSmsApi(String phoneNumber, String templateParam, String bizType) throws Exception {
        String accessKeyId = smsConfigService.getAccessKeyId();
        String accessKeySecret = smsConfigService.getAccessKeySecret();
        String signName = smsConfigService.getSignName();
        String templateCode = getTemplateCode(bizType);
        String regionId = smsConfigService.getRegionId();

        if (StringUtils.isEmpty(accessKeyId) || StringUtils.isEmpty(accessKeySecret)
                || StringUtils.isEmpty(signName) || StringUtils.isEmpty(templateCode)) {
            log.info("阿里云短信配置不完整 (缺少 AccessKey/SignName/TemplateCode)，使用模拟发送模式");
            return;
        }

        if (StringUtils.isEmpty(regionId)) {
            regionId = "cn-hangzhou";
        }

        com.aliyuncs.profile.DefaultProfile profile = com.aliyuncs.profile.DefaultProfile.getProfile(
                regionId, accessKeyId, accessKeySecret);
        com.aliyuncs.DefaultAcsClient client = new com.aliyuncs.DefaultAcsClient(profile);
        com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest request =
                new com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest();
        request.setPhoneNumbers(phoneNumber);
        request.setSignName(signName);
        request.setTemplateCode(templateCode);
        request.setTemplateParam("{\"content\":\"" + escapeJson(templateParam) + "\"}");

        try {
            com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse response =
                    client.getAcsResponse(request);
            log.info("阿里云短信发送: phone={}, code={}, message={}, bizId={}",
                    phoneNumber, response.getCode(), response.getMessage(), response.getBizId());
            if (!"OK".equals(response.getCode())) {
                throw new RuntimeException("阿里云短信发送失败: " + response.getMessage());
            }
        } catch (com.aliyuncs.exceptions.ClientException e) {
            log.error("阿里云短信SDK调用异常: phone={}", phoneNumber, e);
            throw e;
        }
    }

    /**
     * 根据业务类型获取模板CODE
     */
    private String getTemplateCode(String bizType) {
        switch (bizType) {
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

    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String safeStr(String str) {
        return str != null ? str : "未知";
    }
}
