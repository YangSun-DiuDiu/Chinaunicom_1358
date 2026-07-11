package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.mapper.VisitorAppointmentMapper;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.IVisitorAppointmentService;
import com.ruoyi.system.service.IVisitorLogService;
import com.ruoyi.system.sms.SmsUtil;

/**
 * 访客预约 服务层处理
 *
 * @author ruoyi
 */
@Service
public class VisitorAppointmentServiceImpl implements IVisitorAppointmentService {
    private static final Logger log = LoggerFactory.getLogger(VisitorAppointmentServiceImpl.class);

    /** 预约状态 */
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_VISITING = "VISITING";
    private static final String STATUS_COMPLETED = "COMPLETED";

    @Autowired
    private VisitorAppointmentMapper appointmentMapper;

    @Autowired
    private SmsUtil smsUtil;

    @Autowired
    private IVisitorLogService visitorLogService;

    @Autowired
    @Lazy
    private ISysUserService sysUserService;

    /**
     * 查询预约列表
     *
     * @param appointment 预约信息
     * @return 预约集合
     */
    @Override
    public List<VisitorAppointment> selectAppointmentList(VisitorAppointment appointment) {
        return appointmentMapper.selectAppointmentList(appointment);
    }

    /**
     * 通过预约ID查询预约信息
     *
     * @param appointmentId 预约ID
     * @return 预约信息
     */
    @Override
    public VisitorAppointment selectAppointmentById(Long appointmentId) {
        return appointmentMapper.selectAppointmentById(appointmentId);
    }

    /**
     * 新增预约
     *
     * @param appointment 预约信息
     * @return 结果
     */
    @Override
    public int insertAppointment(VisitorAppointment appointment) {
        appointment.setStatus(STATUS_PENDING);
        // 尝试匹配被访人为审批人（含 dept_id 数据范围）
        tryAssignApprover(appointment);
        // 匹配失败时兜底：使用当前登录用户的 dept_id，确保数据范围可见
        if (appointment.getDeptId() == null) {
            try {
                appointment.setDeptId(SecurityUtils.getDeptId());
            } catch (Exception e) {
                // 无登录上下文时跳过
            }
        }
        int rows = appointmentMapper.insertAppointment(appointment);
        // 通知被访人（审批人）有新的访客预约
        notifyApprover(appointment);
        return rows;
    }

    /**
     * 修改预约
     *
     * @param appointment 预约信息
     * @return 结果
     */
    @Override
    public int updateAppointment(VisitorAppointment appointment) {
        return appointmentMapper.updateAppointment(appointment);
    }

    /**
     * 批量删除预约
     *
     * @param appointmentIds 需要删除的预约ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteAppointmentByIds(Long[] appointmentIds) {
        int count = 0;
        for (Long appointmentId : appointmentIds) {
            count += appointmentMapper.deleteAppointmentById(appointmentId);
        }
        return count;
    }

    /**
     * 审批预约（通过/拒绝），记录审批人信息、创建来访记录、发送短信通知
     *
     * @param appointmentId 预约ID
     * @param status        审批状态（APPROVED=已通过 REJECTED=已拒绝）
     * @param remark        审批备注
     * @return 结果
     */
    @Override
    @Transactional
    public int approveAppointment(Long appointmentId, String status, String remark) {
        VisitorAppointment appointment = appointmentMapper.selectAppointmentById(appointmentId);
        if (appointment == null) {
            throw new ServiceException("预约信息不存在");
        }
        if (!STATUS_PENDING.equals(appointment.getStatus())) {
            throw new ServiceException("当前预约状态不允许审批，仅待审批状态可操作");
        }

        // 设置审批信息
        appointment.setStatus(status);
        appointment.setApproverId(SecurityUtils.getUserId());
        appointment.setApproveTime(new Date());
        appointment.setApproveRemark(remark);

        int result = appointmentMapper.updateAppointment(appointment);

        // 审批通过后的连锁操作：创建来访记录 + 发送短信
        if (result > 0 && STATUS_APPROVED.equals(status)) {
            // 确保通行码存在
            String passCode = appointment.getPassCode();
            if (passCode == null || passCode.isEmpty()) {
                passCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                appointment.setPassCode(passCode);
                appointmentMapper.updateAppointment(appointment);
            }

            // 避免重复创建来访记录
            VisitorLog existLog = new VisitorLog();
            existLog.setAppointmentId(appointmentId);
            List<VisitorLog> existLogs = visitorLogService.selectLogList(existLog);
            if (existLogs == null || existLogs.isEmpty()) {
                VisitorLog log = new VisitorLog();
                log.setAppointmentId(appointmentId);
                log.setPassCode(passCode);
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
                log.setEntryTime(appointment.getVisitTime());
                log.setRegisterType("APPOINTMENT");
                if (appointment.getCreateBy() != null) {
                    log.setCreateBy(appointment.getCreateBy());
                }
                visitorLogService.insertLog(log);
            }

            // 发送短信通知（仅一次，通过模板）
            sendApprovalSms(appointment);
        }

        return result;
    }

    /**
     * 取消预约
     *
     * @param appointmentId 预约ID
     * @return 结果
     */
    @Override
    public int cancelAppointment(Long appointmentId) {
        VisitorAppointment appointment = appointmentMapper.selectAppointmentById(appointmentId);
        if (appointment == null) {
            throw new ServiceException("预约信息不存在");
        }
        if (!STATUS_PENDING.equals(appointment.getStatus()) && !STATUS_APPROVED.equals(appointment.getStatus())) {
            throw new ServiceException("当前预约状态不允许取消");
        }

        appointment.setStatus(STATUS_CANCELLED);
        return appointmentMapper.updateAppointment(appointment);
    }

    /**
     * 完成预约（访客离开）
     *
     * @param appointmentId 预约ID
     * @return 结果
     */
    @Override
    public int completeAppointment(Long appointmentId) {
        VisitorAppointment appointment = appointmentMapper.selectAppointmentById(appointmentId);
        if (appointment == null) {
            throw new ServiceException("预约信息不存在");
        }
        if (!STATUS_VISITING.equals(appointment.getStatus()) && !STATUS_APPROVED.equals(appointment.getStatus())) {
            throw new ServiceException("当前预约状态不允许完成操作");
        }

        appointment.setStatus(STATUS_COMPLETED);
        appointment.setLeaveTime(new Date());
        return appointmentMapper.updateAppointment(appointment);
    }

    /**
     * 查询待处理预约列表（可按审批人过滤）
     *
     * @param approverId 审批人ID（null=查全部，非null=按审批人过滤）
     * @return 待处理预约集合
     */
    @Override
    public List<VisitorAppointment> selectPendingList(Long approverId) {
        // 非管理员时按审批人过滤；管理员(admin)或approverId为null时查全部
        return appointmentMapper.selectPendingList(approverId);
    }

    /**
     * 发送审批结果短信通知（使用短信中台模板发送）
     *
     * @param appointment 预约信息
     */
    private void sendApprovalSms(VisitorAppointment appointment) {
        try {
            // 使用短信中台模板驱动发送（模板内容在云平台维护）
            String passCode = appointment.getPassCode() != null ? appointment.getPassCode() : "";
            smsUtil.sendSms("visitor_approval", appointment.getVisitorPhone(),
                    "{\"visitor_name\":\"" + safeStr(appointment.getVisitorName())
                            + "\",\"host_name\":\"" + safeStr(appointment.getHostName())
                            + "\",\"pass_code\":\"" + passCode + "\"}",
                    1, null);

            log.info("审批短信已发送 -> 手机号: {}, 预约ID: {}",
                    appointment.getVisitorPhone(), appointment.getAppointmentId());

            appointment.setSmsSent("Y");
            appointment.setSmsContent("[模板发送] bizCode=visitor_approval, passCode=" + passCode);
            appointmentMapper.updateAppointment(appointment);
        } catch (Exception e) {
            log.error("短信发送失败 -> 预约ID: {}, 手机号: {}", appointment.getAppointmentId(),
                    appointment.getVisitorPhone(), e);
            appointment.setSmsSent("N");
            appointment.setSmsContent("[模板发送失败] bizCode=visitor_approval");
            appointmentMapper.updateAppointment(appointment);
        }
    }

    /** 安全字符串，避免null */
    private String safeStr(String s) {
        return s != null ? s : "";
    }

    // ========== 以下为新增方法 ==========

    /**
     * 通过通行码查询预约
     */
    @Override
    public VisitorAppointment selectAppointmentByPassCode(String passCode) {
        return appointmentMapper.selectAppointmentByPassCode(passCode);
    }

    /**
     * 到访确认：通行码验证通过后更新状态为 VISITING
     */
    @Override
    @Transactional
    public VisitorAppointment checkInVisitor(String passCode) {
        VisitorAppointment appointment = appointmentMapper.selectAppointmentByPassCode(passCode);
        if (appointment == null) {
            return null;
        }
        // 只有 APPROVED 或 PENDING 状态的才更新为 VISITING
        if (STATUS_APPROVED.equals(appointment.getStatus()) || STATUS_PENDING.equals(appointment.getStatus())) {
            appointment.setStatus(STATUS_VISITING);
            appointmentMapper.updateAppointment(appointment);
        }
        return appointment;
    }

    /**
     * 访客自助登记/现场登记：事务中创建预约(PENDING)+来访记录(WALKIN)
     */
    @Override
    @Transactional
    public Map<String, Object> registerWalkin(VisitorAppointment appointment, String createBy) {
        // 1. 生成通行码
        String passCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 2. 创建预约记录
        appointment.setPassCode(passCode);
        appointment.setStatus(STATUS_PENDING);
        appointment.setCreateBy(createBy);
        if (appointment.getVisitTime() == null) {
            appointment.setVisitTime(new Date());
        }
        // 尝试匹配被访人为审批人（含 dept_id 数据范围），复用 insertAppointment 的 fallback 逻辑
        this.insertAppointment(appointment);

        // 3. 同时创建来访记录
        VisitorLog log = new VisitorLog();
        log.setAppointmentId(appointment.getAppointmentId());
        log.setPassCode(passCode);
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
        log.setEntryTime(new Date());
        log.setRegisterType("WALKIN");
        log.setCreateBy(createBy);
        visitorLogService.insertLog(log);

        Map<String, Object> result = new HashMap<>();
        result.put("passCode", passCode);
        result.put("appointmentId", appointment.getAppointmentId());
        result.put("log", log);
        return result;
    }

    /**
     * 尝试匹配被访人为审批人（best-effort）
     * 根据hostName反查sys_user，匹配成功则设置为预约审批人
     */
    private void tryAssignApprover(VisitorAppointment appointment) {
        try {
            String hostName = appointment.getHostName();
            if (hostName == null || hostName.isEmpty()) {
                return;
            }
            Long approverId = null;
            Long deptId = null;
            // 1. 优先尝试精确匹配 loginName（user_name 列）
            SysUser user = sysUserService.selectUserByUserName(hostName);
            if (user != null && user.getUserId() != null) {
                approverId = user.getUserId();
                deptId = user.getDeptId();
            }
            // 2. loginName 没匹配上，尝试按 nickName 匹配（nick_name 列）
            //    使用 Map 返回避免 SysUser 类型别名不匹配问题
            if (approverId == null) {
                Map<String, Object> nickUser = appointmentMapper.selectUserByNickName(hostName);
                if (nickUser != null && nickUser.get("userId") != null) {
                    approverId = Long.valueOf(nickUser.get("userId").toString());
                    if (nickUser.get("deptId") != null) {
                        deptId = Long.valueOf(nickUser.get("deptId").toString());
                    }
                }
            }
            // 匹配成功：设置审批人 + 数据范围（dept_id）
            if (approverId != null) {
                appointment.setApproverId(approverId);
                if (deptId != null) {
                    appointment.setDeptId(deptId);
                }
                log.debug("被访人-审批人匹配成功: hostName={}, approverId={}, deptId={}",
                        hostName, approverId, deptId);
            }
        } catch (Exception e) {
            log.debug("被访人-审批人匹配跳过: hostName={}", appointment.getHostName());
        }
    }

    /**
     * 通知被访人（审批人）有新的访客预约待审批
     */
    private void notifyApprover(VisitorAppointment appointment) {
        try {
            Long approverId = appointment.getApproverId();
            if (approverId == null) return;
            SysUser approver = sysUserService.selectUserById(approverId);
            if (approver == null || approver.getPhonenumber() == null || approver.getPhonenumber().isEmpty()) return;
            String approverName = approver.getNickName() != null ? approver.getNickName() : approver.getUserName();
            String visitTimeStr = appointment.getVisitTime() != null
                ? new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(appointment.getVisitTime()) : "";
            smsUtil.sendSms("visitor_pending_notify", approver.getPhonenumber(),
                "{\"host_name\":\"" + safeStr(approverName)
                + "\",\"visitor_name\":\"" + safeStr(appointment.getVisitorName())
                + "\",\"visitor_phone\":\"" + safeStr(appointment.getVisitorPhone())
                + "\",\"visit_reason\":\"" + safeStr(appointment.getVisitReason())
                + "\",\"visit_time\":\"" + visitTimeStr + "\"}", 1, null);
            log.info("已通知审批人: approverId={}, phone={}", approverId, approver.getPhonenumber());
        } catch (Exception e) {
            log.warn("通知审批人失败: appointmentId={}", appointment.getAppointmentId(), e);
        }
    }
}
