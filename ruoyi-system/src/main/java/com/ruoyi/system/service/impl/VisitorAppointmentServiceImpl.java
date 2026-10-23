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
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorAppointmentState;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.domain.VisitorLogFactory;
import com.ruoyi.system.mapper.VisitorAppointmentMapper;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.system.service.IVisitorAppointmentService;
import com.ruoyi.system.service.IVisitorDeptQrcodeService;
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

    @Autowired
    private VisitorAppointmentMapper appointmentMapper;

    @Autowired
    private SmsUtil smsUtil;

    @Autowired
    private IVisitorLogService visitorLogService;

    @Autowired
    @Lazy
    private ISysUserService sysUserService;

    @Autowired
    private ISysDeptService sysDeptService;

    @Autowired
    private IVisitorDeptQrcodeService visitorDeptQrcodeService;

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
        return insertAppointment(appointment, true);
    }

    /**
     * 新增预约（内部创建路径共用）
     *
     * @param appointment  预约信息
     * @param forceAssign  是否强制匹配被访人（审批人+部门）。普通预约入口传 true；
     *                     registerWalkin 已在创建前完成匹配与校验，传 false 避免重复执行
     * @return 结果
     */
    public int insertAppointment(VisitorAppointment appointment, boolean forceAssign) {
        appointment.setStatus(VisitorAppointmentState.PENDING);
        // 强制匹配被访人：设置审批人 + 部门数据范围，匹配失败直接抛异常
        if (forceAssign) {
            assignApproverAndDept(appointment);
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
        if (!VisitorAppointmentState.can(appointment.getStatus(), VisitorAppointmentState.Event.APPROVE)
                && !VisitorAppointmentState.can(appointment.getStatus(), VisitorAppointmentState.Event.REJECT)) {
            throw new ServiceException("当前预约状态不允许审批，仅待审批状态可操作");
        }

        // 审批人身份校验：仅被访人本人（预约审批人）可审批；超管(admin)与园区管理员(smartparkAdmin)不受限
        Long currentUserId = SecurityUtils.getUserId();
        boolean isParkAdmin = SecurityUtils.hasRole("smartparkAdmin");
        if (currentUserId != null && !SecurityUtils.isAdmin(currentUserId) && !isParkAdmin
                && (appointment.getApproverId() == null || !appointment.getApproverId().equals(currentUserId))) {
            throw new ServiceException("无权审批该预约：仅被访人本人可审批");
        }

        // 设置审批信息（目标状态由状态机决定）
        appointment.setStatus(VisitorAppointmentState.transition(appointment.getStatus(),
                status.equals(VisitorAppointmentState.REJECTED)
                        ? VisitorAppointmentState.Event.REJECT
                        : VisitorAppointmentState.Event.APPROVE));
        appointment.setApproverId(SecurityUtils.getUserId());
        appointment.setApproveTime(new Date());
        appointment.setApproveRemark(remark);

        int result = appointmentMapper.updateAppointment(appointment);

        // 审批通过后的连锁操作：创建来访记录 + 发送短信
        if (result > 0 && VisitorAppointmentState.APPROVED.equals(appointment.getStatus())) {
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
                visitorLogService.insertLog(VisitorLogFactory.from(appointment, "APPOINTMENT", null));
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
        if (!VisitorAppointmentState.can(appointment.getStatus(), VisitorAppointmentState.Event.CANCEL)) {
            throw new ServiceException("当前预约状态不允许取消");
        }

        appointment.setStatus(VisitorAppointmentState.transition(appointment.getStatus(),
                VisitorAppointmentState.Event.CANCEL));
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
        if (!VisitorAppointmentState.can(appointment.getStatus(), VisitorAppointmentState.Event.COMPLETE)) {
            throw new ServiceException("当前预约状态不允许完成操作");
        }

        appointment.setStatus(VisitorAppointmentState.transition(appointment.getStatus(),
                VisitorAppointmentState.Event.COMPLETE));
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
                            + "\",\"passcode\":\"" + passCode + "\"}",
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
     * 通过访客手机号查询预约概要列表（公开查询）
     */
    @Override
    public java.util.List<com.ruoyi.system.domain.vo.AppointmentBriefDTO> selectAppointmentBriefByPhone(String visitorPhone) {
        return appointmentMapper.selectAppointmentBriefByPhone(visitorPhone);
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
        // 仅 APPROVED / PENDING 状态可转为 VISITING（状态机校验）
        if (VisitorAppointmentState.can(appointment.getStatus(), VisitorAppointmentState.Event.CHECKIN)) {
            appointment.setStatus(VisitorAppointmentState.transition(appointment.getStatus(),
                    VisitorAppointmentState.Event.CHECKIN));
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
        // 旧版二维码/现场登记：不限定部门
        return registerWalkin(appointment, createBy, null);
    }

    /**
     * 访客自助登记（部门二维码模式）：创建预约+来访记录前，校验被访人所在部门
     * 是否在登记码限定的部门(含子部门)范围内
     */
    @Override
    @Transactional
    public Map<String, Object> registerWalkin(VisitorAppointment appointment, String createBy, Long qrDeptId) {
        // 1. 强制匹配被访人为审批人，并得到被访人部门(dept_id)，匹配失败直接抛异常
        assignApproverAndDept(appointment);

        // 2. 部门登记码限定校验：被访人部门必须在扫码部门(含子部门)范围内
        if (qrDeptId != null) {
            validateDeptScope(appointment, qrDeptId);
        }

        // 3. 生成通行码
        String passCode = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // 4. 创建预约记录（宿主匹配已在步骤1完成，forceAssign=false 避免重复执行）
        appointment.setPassCode(passCode);
        appointment.setStatus(VisitorAppointmentState.PENDING);
        appointment.setCreateBy(createBy);
        if (appointment.getVisitTime() == null) {
            appointment.setVisitTime(new Date());
        }
        this.insertAppointment(appointment, false);

        // 5. 同时创建来访记录
        VisitorLog log = VisitorLogFactory.from(appointment, "WALKIN", new Date());
        visitorLogService.insertLog(log);

        Map<String, Object> result = new HashMap<>();
        result.put("passCode", passCode);
        result.put("appointmentId", appointment.getAppointmentId());
        result.put("log", log);
        return result;
    }

    /**
     * 部门登记码限定校验：被访人所在部门必须在扫码部门(含子部门)范围内
     *
     * @param appointment 预约信息（须已执行 assignApproverAndDept，dept_id 为被访人部门）
     * @param qrDeptId    登记码限定部门ID
     */
    private void validateDeptScope(VisitorAppointment appointment, Long qrDeptId) {
        Long hostDeptId = appointment.getDeptId();
        if (hostDeptId == null) {
            throw new ServiceException("被访人部门信息缺失，无法校验登记范围");
        }
        if (!visitorDeptQrcodeService.isDeptWithinScope(hostDeptId, qrDeptId)) {
            SysDept dept = sysDeptService.selectDeptById(qrDeptId);
            String deptName = dept != null ? dept.getDeptName() : String.valueOf(qrDeptId);
            throw new ServiceException("该访客登记码仅限预约『" + deptName + "』部门人员，当前被访人不在可预约范围");
        }
    }

    /**
     * 强制匹配被访人为审批人（hostName → sys_user），同时设置 dept_id 数据范围
     *
     * 匹配失败直接抛异常，阻止创建访客实体。
     * 适用于员工后台预约、H5自助登记、现场散客登记等所有创建路径。
     */
    private void assignApproverAndDept(VisitorAppointment appointment) {
        String hostName = appointment.getHostName();
        if (hostName == null || hostName.isEmpty()) {
            throw new ServiceException("被访人姓名不能为空");
        }
        Long approverId = null;
        Long deptId = null;
        // 0. 优先按前端下发的被访人账号(user_name)精确匹配（H5部门登记模式选中人时下发）
        String hostAccount = appointment.getHostAccount();
        if (StringUtils.isNotEmpty(hostAccount)) {
            SysUser accountUser = sysUserService.selectUserByUserName(hostAccount);
            if (accountUser != null && accountUser.getUserId() != null) {
                approverId = accountUser.getUserId();
                deptId = accountUser.getDeptId();
            }
        }
        // 1. 其次精确匹配 loginName（user_name 列）
        if (approverId == null) {
            SysUser user = sysUserService.selectUserByUserName(hostName);
            if (user != null && user.getUserId() != null) {
                approverId = user.getUserId();
                deptId = user.getDeptId();
            }
        }
        // 2. loginName 没匹配上，按 nickName 匹配（nick_name 列）
        if (approverId == null) {
            Map<String, Object> nickUser = appointmentMapper.selectUserByNickName(hostName);
            if (nickUser != null && nickUser.get("userId") != null) {
                approverId = Long.valueOf(nickUser.get("userId").toString());
                if (nickUser.get("deptId") != null) {
                    deptId = Long.valueOf(nickUser.get("deptId").toString());
                }
            }
        }
        // 3. 匹配失败 → 拒绝创建
        if (approverId == null) {
            throw new ServiceException("被访人『" + hostName + "』不存在，请检查姓名是否正确");
        }
        // 匹配成功：设置审批人 + 数据范围（以被访人部门为准）
        appointment.setApproverId(approverId);
        if (deptId != null) {
            appointment.setDeptId(deptId);
        }
        log.debug("被访人-审批人匹配成功: hostName={}, approverId={}, deptId={}",
                hostName, approverId, deptId);
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
