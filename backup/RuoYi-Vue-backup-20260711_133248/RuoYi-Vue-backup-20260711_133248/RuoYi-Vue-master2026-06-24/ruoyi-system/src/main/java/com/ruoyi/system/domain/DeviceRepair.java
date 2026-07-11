package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 设备维修工单实体
 *
 * @author ruoyi
 */
public class DeviceRepair extends BaseEntity {
    private static final long serialVersionUID = 1L;

    private Long repairId;
    private String repairNo;
    private Long deviceId;
    private String deviceName;
    private String deviceIp;
    private String faultDescription;
    private String originalResponsible;
    private String originalPhone;
    private String currentResponsible;
    private String currentPhone;
    private String status;
    private String transferFrom;
    private String transferTo;
    private String transferReason;
    private String repairResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date repairTime;
    private String completeToken;
    private String photoBefore;
    private String photoAfter;
    private String hasParts;
    private String partsDesc;

    public Long getRepairId() {
        return repairId;
    }

    public void setRepairId(Long repairId) {
        this.repairId = repairId;
    }

    public String getRepairNo() {
        return repairNo;
    }

    public void setRepairNo(String repairNo) {
        this.repairNo = repairNo;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public String getFaultDescription() {
        return faultDescription;
    }

    public void setFaultDescription(String faultDescription) {
        this.faultDescription = faultDescription;
    }

    public String getOriginalResponsible() {
        return originalResponsible;
    }

    public void setOriginalResponsible(String originalResponsible) {
        this.originalResponsible = originalResponsible;
    }

    public String getOriginalPhone() {
        return originalPhone;
    }

    public void setOriginalPhone(String originalPhone) {
        this.originalPhone = originalPhone;
    }

    public String getCurrentResponsible() {
        return currentResponsible;
    }

    public void setCurrentResponsible(String currentResponsible) {
        this.currentResponsible = currentResponsible;
    }

    public String getCurrentPhone() {
        return currentPhone;
    }

    public void setCurrentPhone(String currentPhone) {
        this.currentPhone = currentPhone;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransferFrom() {
        return transferFrom;
    }

    public void setTransferFrom(String transferFrom) {
        this.transferFrom = transferFrom;
    }

    public String getTransferTo() {
        return transferTo;
    }

    public void setTransferTo(String transferTo) {
        this.transferTo = transferTo;
    }

    public String getTransferReason() {
        return transferReason;
    }

    public void setTransferReason(String transferReason) {
        this.transferReason = transferReason;
    }

    public String getRepairResult() {
        return repairResult;
    }

    public void setRepairResult(String repairResult) {
        this.repairResult = repairResult;
    }

    public Date getRepairTime() {
        return repairTime;
    }

    public void setRepairTime(Date repairTime) {
        this.repairTime = repairTime;
    }

    public String getCompleteToken() {
        return completeToken;
    }

    public void setCompleteToken(String completeToken) {
        this.completeToken = completeToken;
    }

    public String getPhotoBefore() {
        return photoBefore;
    }

    public void setPhotoBefore(String photoBefore) {
        this.photoBefore = photoBefore;
    }

    public String getPhotoAfter() {
        return photoAfter;
    }

    public void setPhotoAfter(String photoAfter) {
        this.photoAfter = photoAfter;
    }

    public String getHasParts() {
        return hasParts;
    }

    public void setHasParts(String hasParts) {
        this.hasParts = hasParts;
    }

    public String getPartsDesc() {
        return partsDesc;
    }

    public void setPartsDesc(String partsDesc) {
        this.partsDesc = partsDesc;
    }
}
