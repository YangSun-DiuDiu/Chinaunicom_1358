package com.ruoyi.system.service.impl;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Device;
import com.ruoyi.system.service.IAlertWebhookService;
import com.ruoyi.system.service.IDeviceService;

/**
 * AlertManager 告警处理 Service 实现
 *
 * 解析 AlertManager 推送的告警 payload，匹配设备并更新设备状态。
 * firing -> OFFLINE(离线)，resolved -> ONLINE(在线)。
 *
 * @author ruoyi
 */
@Service
public class AlertWebhookServiceImpl implements IAlertWebhookService {
    private static final Logger log = LoggerFactory.getLogger(AlertWebhookServiceImpl.class);

    /** 设备状态：离线 */
    private static final String STATUS_OFFLINE = "OFFLINE";

    /** 设备状态：在线 */
    private static final String STATUS_ONLINE = "ONLINE";

    @Autowired
    private IDeviceService deviceService;

    /**
     * 处理 AlertManager 告警 payload
     *
     * @param payload AlertManager 推送的告警数据
     * @return 处理的设备数
     */
    @Override
    @SuppressWarnings("unchecked")
    public int handleAlert(Map<String, Object> payload) {
        int handledCount = 0;
        try {
            if (payload == null) {
                log.warn("[AlertWebhook] 收到空的告警 payload");
                return 0;
            }
            Object alertsObj = payload.get("alerts");
            if (!(alertsObj instanceof List)) {
                log.warn("[AlertWebhook] 告警 payload 中 alerts 字段缺失或类型不正确");
                return 0;
            }
            List<Map<String, Object>> alerts = (List<Map<String, Object>>) alertsObj;
            for (Map<String, Object> alert : alerts) {
                try {
                    if (alert == null) {
                        continue;
                    }
                    // 告警状态：firing(触发) / resolved(恢复)
                    Object statusObj = alert.get("status");
                    String alertStatus = statusObj == null ? "" : String.valueOf(statusObj);

                    // 从 labels.target 提取实例地址（AlertManager 推送 payload 中 IP 在 labels.target）
                    Object labelsObj = alert.get("labels");
                    if (!(labelsObj instanceof Map)) {
                        log.warn("[AlertWebhook] 告警缺少 labels，跳过");
                        continue;
                    }
                    Map<String, Object> labels = (Map<String, Object>) labelsObj;
                    Object instanceObj = labels.get("target");
                    String instance = instanceObj == null ? "" : String.valueOf(instanceObj);
                    if (StringUtils.isEmpty(instance)) {
                        log.warn("[AlertWebhook] 告警缺少 target 标签，跳过");
                        continue;
                    }
                    // instance 可能是 ip:port 形式，取 ip 部分
                    String ip = instance;
                    int colonIndex = instance.indexOf(':');
                    if (colonIndex > -1) {
                        ip = instance.substring(0, colonIndex);
                    }
                    ip = ip.trim();
                    if (StringUtils.isEmpty(ip)) {
                        log.warn("[AlertWebhook] 解析 instance[{}] 得到空 IP，跳过", instance);
                        continue;
                    }

                    // 目标状态：显式三态判断，firing -> 离线，resolved -> 在线，其余异常值跳过
                    // 避免 status 缺失/异常值被误判为 OFFLINE，触发误报短信
                    String targetStatus;
                    if ("firing".equalsIgnoreCase(alertStatus)) {
                        targetStatus = STATUS_OFFLINE;
                    } else if ("resolved".equalsIgnoreCase(alertStatus)) {
                        targetStatus = STATUS_ONLINE;
                    } else {
                        log.warn("[AlertWebhook] 未知告警状态[{}] IP[{}]，跳过", alertStatus, ip);
                        continue;
                    }

                    // 按 IP 匹配设备
                    Device query = new Device();
                    query.setIpAddress(ip);
                    List<Device> deviceList = deviceService.selectDeviceList(query);
                    if (deviceList == null || deviceList.isEmpty()) {
                        log.warn("[AlertWebhook] 未匹配到 IP[{}] 对应的设备，跳过", ip);
                        continue;
                    }
                    for (Device device : deviceList) {
                        if (device == null || device.getDeviceId() == null) {
                            continue;
                        }
                        // 精确匹配：XML 使用 LIKE 模糊查询，192.168.1.5 会命中 192.168.1.50/55 等
                        // 此处按 IP 全等过滤，跳过 LIKE 误命中的同网段设备，避免误伤
                        if (!ip.equals(device.getIpAddress())) {
                            continue;
                        }
                        deviceService.updateDeviceStatus(device.getDeviceId(), targetStatus);
                        handledCount++;
                        log.info("[AlertWebhook] 设备[{}] IP[{}] 状态更新为[{}]",
                                device.getDeviceName(), ip, targetStatus);
                    }
                } catch (Exception e) {
                    log.error("[AlertWebhook] 处理单条告警异常", e);
                }
            }
        } catch (Exception e) {
            log.error("[AlertWebhook] 处理告警 payload 异常", e);
        }
        return handledCount;
    }
}
