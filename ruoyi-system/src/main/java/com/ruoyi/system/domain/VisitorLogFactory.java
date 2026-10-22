package com.ruoyi.system.domain;

import java.util.Date;

/**
 * 访客记录工厂 —— 预约(visitor_appointment) → 来访记录(visitor_log) 字段映射的唯一入口
 *
 * <p>消除 approveAppointment / registerWalkin 两处重复的逐字段拷贝（低 leverage 复制粘贴）。
 * 新增字段只改这一处。</p>
 */
public final class VisitorLogFactory {

    private VisitorLogFactory() {
    }

    /**
     * 由预约构建来访记录
     *
     * @param appointment  预约（须已生成 passCode）
     * @param registerType 登记类型：APPOINTMENT=预约 WALKIN=散客
     * @param entryTime    进入时间；null 时取预约来访时间
     * @return 来访记录（未落库）
     */
    public static VisitorLog from(VisitorAppointment appointment, String registerType, Date entryTime) {
        VisitorLog log = new VisitorLog();
        log.setAppointmentId(appointment.getAppointmentId());
        log.setPassCode(appointment.getPassCode());
        log.setVisitorName(appointment.getVisitorName());
        log.setVisitorPhone(appointment.getVisitorPhone());
        log.setVisitorIdCard(appointment.getVisitorIdCard());
        log.setVisitorCompany(appointment.getVisitorCompany());
        log.setVisitReason(appointment.getVisitReason());
        log.setHostName(appointment.getHostName());
        log.setHostDept(appointment.getHostDept());
        log.setHasCar(appointment.getHasCar());
        log.setCarPlate(appointment.getCarPlate());
        log.setHasGoods(appointment.getHasGoods());
        log.setGoodsDesc(appointment.getGoodsDesc());
        log.setDeptId(appointment.getDeptId());
        log.setRegisterType(registerType);
        log.setEntryTime(entryTime != null ? entryTime : appointment.getVisitTime());
        if (appointment.getCreateBy() != null) {
            log.setCreateBy(appointment.getCreateBy());
        }
        return log;
    }
}
