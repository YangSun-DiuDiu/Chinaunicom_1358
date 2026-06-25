package com.ruoyi.system.service;

import com.ruoyi.system.domain.Device;
import com.ruoyi.system.domain.VisitorAppointment;

/**
 * 短信服务接口
 *
 * @author ruoyi
 */
public interface ISmsService
{
    /**
     * 发送短信并记录到短信日志表
     *
     * @param recipient 收件人
     * @param phoneNumber 手机号码
     * @param content 短信内容
     * @param bizType 业务类型（DEVICE_OFFLINE / DEVICE_ONLINE / VISITOR_APPROVE / REPAIR）
     * @param bizId 业务ID
     */
    public void sendSms(String recipient, String phoneNumber, String content, String bizType, Long bizId);

    /**
     * 发送设备离线告警短信
     *
     * @param device 设备信息
     */
    public void sendDeviceOfflineAlert(Device device);

    /**
     * 发送设备上线恢复短信
     *
     * @param device 设备信息
     */
    public void sendDeviceOnlineAlert(Device device);

    /**
     * 发送设备维修通知短信
     *
     * @param device 设备信息
     */
    public void sendRepairAlert(Device device);

    /**
     * 发送访客审批通过短信
     *
     * @param appointment 访客预约信息
     */
    public void sendVisitorApprovalSms(VisitorAppointment appointment);

    /**
     * 发送测试短信
     *
     * @param phoneNumber 手机号码
     */
    public void sendTestSms(String phoneNumber);
}
