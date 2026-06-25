package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.mapper.VisitorAppointmentMapper;
import com.ruoyi.system.service.IVisitorAppointmentService;
import com.ruoyi.system.service.ISmsService;

/**
 * 访客预约 服务层处理
 *
 * @author ruoyi
 */
@Service
public class VisitorAppointmentServiceImpl implements IVisitorAppointmentService
{
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
    private ISmsService smsService;

    /**
     * 查询预约列表
     *
     * @param appointment 预约信息
     * @return 预约集合
     */
    @Override
    public List<VisitorAppointment> selectAppointmentList(VisitorAppointment appointment)
    {
        return appointmentMapper.selectAppointmentList(appointment);
    }

    /**
     * 通过预约ID查询预约信息
     *
     * @param appointmentId 预约ID
     * @return 预约信息
     */
    @Override
    public VisitorAppointment selectAppointmentById(Long appointmentId)
    {
        return appointmentMapper.selectAppointmentById(appointmentId);
    }

    /**
     * 新增预约
     *
     * @param appointment 预约信息
     * @return 结果
     */
    @Override
    public int insertAppointment(VisitorAppointment appointment)
    {
        appointment.setStatus(STATUS_PENDING);
        return appointmentMapper.insertAppointment(appointment);
    }

    /**
     * 修改预约
     *
     * @param appointment 预约信息
     * @return 结果
     */
    @Override
    public int updateAppointment(VisitorAppointment appointment)
    {
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
    public int deleteAppointmentByIds(Long[] appointmentIds)
    {
        int count = 0;
        for (Long appointmentId : appointmentIds)
        {
            count += appointmentMapper.deleteAppointmentById(appointmentId);
        }
        return count;
    }

    /**
     * 审批预约（通过/拒绝），记录审批人信息并触发短信通知
     *
     * @param appointmentId 预约ID
     * @param status 审批状态（APPROVED=已通过 REJECTED=已拒绝）
     * @param remark 审批备注
     * @return 结果
     */
    @Override
    @Transactional
    public int approveAppointment(Long appointmentId, String status, String remark)
    {
        VisitorAppointment appointment = appointmentMapper.selectAppointmentById(appointmentId);
        if (appointment == null)
        {
            throw new ServiceException("预约信息不存在");
        }
        if (!STATUS_PENDING.equals(appointment.getStatus()))
        {
            throw new ServiceException("当前预约状态不允许审批，仅待审批状态可操作");
        }

        // 设置审批信息
        appointment.setStatus(status);
        appointment.setApproverId(SecurityUtils.getUserId());
        appointment.setApproveTime(new Date());
        appointment.setApproveRemark(remark);

        int result = appointmentMapper.updateAppointment(appointment);

        if (result > 0)
        {
            // 触发短信通知
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
    public int cancelAppointment(Long appointmentId)
    {
        VisitorAppointment appointment = appointmentMapper.selectAppointmentById(appointmentId);
        if (appointment == null)
        {
            throw new ServiceException("预约信息不存在");
        }
        if (!STATUS_PENDING.equals(appointment.getStatus()) && !STATUS_APPROVED.equals(appointment.getStatus()))
        {
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
    public int completeAppointment(Long appointmentId)
    {
        VisitorAppointment appointment = appointmentMapper.selectAppointmentById(appointmentId);
        if (appointment == null)
        {
            throw new ServiceException("预约信息不存在");
        }
        if (!STATUS_VISITING.equals(appointment.getStatus()) && !STATUS_APPROVED.equals(appointment.getStatus()))
        {
            throw new ServiceException("当前预约状态不允许完成操作");
        }

        appointment.setStatus(STATUS_COMPLETED);
        appointment.setLeaveTime(new Date());
        return appointmentMapper.updateAppointment(appointment);
    }

    /**
     * 查询待处理预约列表
     *
     * @return 待处理预约集合
     */
    @Override
    public List<VisitorAppointment> selectPendingList()
    {
        return appointmentMapper.selectPendingList();
    }

    /**
     * 发送审批结果短信通知
     * <p>
     * 目前通过日志记录短信内容。待接入第三方短信平台（如阿里云短信、腾讯云短信）
     * 后，在此处调用实际发送接口。
     * </p>
     *
     * @param appointment 预约信息
     */
    private void sendApprovalSms(VisitorAppointment appointment)
    {
        String smsContent = buildSmsContent(appointment);
        try
        {
            // 调用短信服务实际发送
            smsService.sendVisitorApprovalSms(appointment);

            log.info("审批短信已发送 -> 手机号: {}, 内容: {}",
                    appointment.getVisitorPhone(), smsContent);

            // 记录短信发送状态
            appointment.setSmsSent("Y");
            appointment.setSmsContent(smsContent);
            appointmentMapper.updateAppointment(appointment);
        }
        catch (Exception e)
        {
            log.error("短信发送失败 -> 预约ID: {}, 手机号: {}", appointment.getAppointmentId(),
                    appointment.getVisitorPhone(), e);
            appointment.setSmsSent("N");
            appointment.setSmsContent(smsContent);
            appointmentMapper.updateAppointment(appointment);
        }
    }

    /**
     * 根据审批结果构建短信内容
     *
     * @param appointment 预约信息
     * @return 短信内容
     */
    private String buildSmsContent(VisitorAppointment appointment)
    {
        String statusText = STATUS_APPROVED.equals(appointment.getStatus()) ? "已通过" : "未通过";
        StringBuilder sb = new StringBuilder();
        sb.append("【访客预约】尊敬的");
        sb.append(appointment.getVisitorName());
        sb.append("，您预约的");
        sb.append(appointment.getVisitReason());
        sb.append("申请已审批，结果为：");
        sb.append(statusText);
        if (STATUS_APPROVED.equals(appointment.getStatus()))
        {
            sb.append("，请按时到访。");
        }
        else
        {
            sb.append("。");
            if (appointment.getApproveRemark() != null && !appointment.getApproveRemark().isEmpty())
            {
                sb.append("原因：");
                sb.append(appointment.getApproveRemark());
            }
        }
        return sb.toString();
    }
}
