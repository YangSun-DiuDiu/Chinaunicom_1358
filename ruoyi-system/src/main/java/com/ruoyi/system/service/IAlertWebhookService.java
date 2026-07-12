package com.ruoyi.system.service;

import java.util.Map;

/**
 * AlertManager 告警处理 Service 接口
 *
 * @author ruoyi
 */
public interface IAlertWebhookService
{
    /**
     * 处理 AlertManager 告警 payload
     *
     * @param payload AlertManager 推送的告警数据
     * @return 处理的设备数
     */
    public int handleAlert(Map<String, Object> payload);
}
