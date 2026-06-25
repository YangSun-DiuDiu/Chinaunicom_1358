package com.ruoyi.system.service;

/**
 * 短信配置读取服务
 *
 * @author ruoyi
 */
public interface ISmsConfigService
{
    /** 短信开关 */
    boolean isSmsEnabled();

    /** 阿里云 AccessKey ID */
    String getAccessKeyId();

    /** 阿里云 AccessKey Secret */
    String getAccessKeySecret();

    /** 短信签名 */
    String getSignName();

    /** 区域ID */
    String getRegionId();

    /** API接入域名 */
    String getSmsEndpoint();

    /** 设备离线告警模板CODE */
    String getTemplateDeviceOffline();

    /** 设备上线通知模板CODE */
    String getTemplateDeviceOnline();

    /** 设备报修通知模板CODE */
    String getTemplateRepair();

    /** 访客审批通知模板CODE */
    String getTemplateVisitorApproval();
}
