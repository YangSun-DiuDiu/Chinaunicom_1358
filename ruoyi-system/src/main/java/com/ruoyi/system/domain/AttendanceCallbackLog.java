package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 考勤回调日志对象 attendance_callback_log
 *
 * @author ruoyi
 */
public class AttendanceCallbackLog
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 来源(DINGTALK/WECHAT) */
    private String source;

    /** 回调类型 */
    private String callbackType;

    /** 原始请求体 */
    private String requestBody;

    /** 请求头(JSON) */
    private String requestHeaders;

    /** 处理结果(SUCCESS/FAILED) */
    private String processResult;

    /** 响应内容 */
    private String responseBody;

    /** 错误信息 */
    private String errorMessage;

    /** 关联用户名 */
    private String userName;

    /** 关联考勤日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date attendanceDate;

    /** 请求来源IP */
    private String ipAddress;

    /** 处理耗时(毫秒) */
    private Integer costMs;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 备注 */
    private String remark;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCallbackType() { return callbackType; }
    public void setCallbackType(String callbackType) { this.callbackType = callbackType; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public String getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(String requestHeaders) { this.requestHeaders = requestHeaders; }

    public String getProcessResult() { return processResult; }
    public void setProcessResult(String processResult) { this.processResult = processResult; }

    public String getResponseBody() { return responseBody; }
    public void setResponseBody(String responseBody) { this.responseBody = responseBody; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public Date getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(Date attendanceDate) { this.attendanceDate = attendanceDate; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Integer getCostMs() { return costMs; }
    public void setCostMs(Integer costMs) { this.costMs = costMs; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("source", getSource())
            .append("callbackType", getCallbackType())
            .append("processResult", getProcessResult())
            .append("userName", getUserName())
            .append("createTime", getCreateTime())
            .toString();
    }
}
