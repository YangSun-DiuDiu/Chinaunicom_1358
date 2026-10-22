package com.ruoyi.system.domain.vo;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 通行信息 DTO —— 保安核验/通行展示的统一契约
 *
 * <p>预约(visitor_appointment)与来访记录(visitor_log)两种来源归一化为同一结构，
 * 不再由 Controller 手拼 Map。</p>
 */
public class PassInfoDTO {

    /** 通行码 */
    private String passCode;

    /** 访客姓名 */
    private String visitorName;

    /** 访客电话 */
    private String visitorPhone;

    /** 访客单位 */
    private String visitorCompany;

    /** 被访人姓名 */
    private String hostName;

    /** 被访人部门 */
    private String hostDept;

    /** 预约来访时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date visitTime;

    /** 实际进入时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date entryTime;

    /** 当前状态 */
    private String status;

    public String getPassCode() {
        return passCode;
    }

    public void setPassCode(String passCode) {
        this.passCode = passCode;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public void setVisitorName(String visitorName) {
        this.visitorName = visitorName;
    }

    public String getVisitorPhone() {
        return visitorPhone;
    }

    public void setVisitorPhone(String visitorPhone) {
        this.visitorPhone = visitorPhone;
    }

    public String getVisitorCompany() {
        return visitorCompany;
    }

    public void setVisitorCompany(String visitorCompany) {
        this.visitorCompany = visitorCompany;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getHostDept() {
        return hostDept;
    }

    public void setHostDept(String hostDept) {
        this.hostDept = hostDept;
    }

    public Date getVisitTime() {
        return visitTime;
    }

    public void setVisitTime(Date visitTime) {
        this.visitTime = visitTime;
    }

    public Date getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(Date entryTime) {
        this.entryTime = entryTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
