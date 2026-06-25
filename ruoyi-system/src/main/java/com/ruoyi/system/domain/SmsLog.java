package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 短信日志表 sys_sms_log
 *
 * @author ruoyi
 */
public class SmsLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 短信ID */
    @Excel(name = "短信ID", cellType = ColumnType.NUMERIC)
    private Long smsId;

    /** 收件人 */
    @Excel(name = "收件人")
    private String recipient;

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String phoneNumber;

    /** 短信内容 */
    @Excel(name = "短信内容")
    private String content;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发送时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    /** 发送结果（SUCCESS 成功 / FAIL 失败） */
    @Excel(name = "发送结果")
    private String sendResult;

    /** 业务类型（DEVICE_OFFLINE 设备离线 / DEVICE_ONLINE 设备上线 / VISITOR_APPROVE 访客审批 / REPAIR 维修） */
    @Excel(name = "业务类型")
    private String bizType;

    /** 业务ID */
    @Excel(name = "业务ID", cellType = ColumnType.NUMERIC)
    private Long bizId;

    public Long getSmsId()
    {
        return smsId;
    }

    public void setSmsId(Long smsId)
    {
        this.smsId = smsId;
    }

    public String getRecipient()
    {
        return recipient;
    }

    public void setRecipient(String recipient)
    {
        this.recipient = recipient;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public Date getSendTime()
    {
        return sendTime;
    }

    public void setSendTime(Date sendTime)
    {
        this.sendTime = sendTime;
    }

    public String getSendResult()
    {
        return sendResult;
    }

    public void setSendResult(String sendResult)
    {
        this.sendResult = sendResult;
    }

    public String getBizType()
    {
        return bizType;
    }

    public void setBizType(String bizType)
    {
        this.bizType = bizType;
    }

    public Long getBizId()
    {
        return bizId;
    }

    public void setBizId(Long bizId)
    {
        this.bizId = bizId;
    }
}
