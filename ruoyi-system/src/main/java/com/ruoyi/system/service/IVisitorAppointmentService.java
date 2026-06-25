package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.VisitorAppointment;

/**
 * 访客预约 业务层
 *
 * @author ruoyi
 */
public interface IVisitorAppointmentService
{
    /**
     * 查询预约列表
     *
     * @param appointment 预约信息
     * @return 预约集合
     */
    public List<VisitorAppointment> selectAppointmentList(VisitorAppointment appointment);

    /**
     * 通过预约ID查询预约信息
     *
     * @param appointmentId 预约ID
     * @return 预约信息
     */
    public VisitorAppointment selectAppointmentById(Long appointmentId);

    /**
     * 新增预约
     *
     * @param appointment 预约信息
     * @return 结果
     */
    public int insertAppointment(VisitorAppointment appointment);

    /**
     * 修改预约
     *
     * @param appointment 预约信息
     * @return 结果
     */
    public int updateAppointment(VisitorAppointment appointment);

    /**
     * 批量删除预约
     *
     * @param appointmentIds 需要删除的预约ID
     * @return 结果
     */
    public int deleteAppointmentByIds(Long[] appointmentIds);

    /**
     * 审批预约（通过/拒绝）
     *
     * @param appointmentId 预约ID
     * @param status 审批状态（APPROVED=已通过 REJECTED=已拒绝）
     * @param remark 审批备注
     * @return 结果
     */
    public int approveAppointment(Long appointmentId, String status, String remark);

    /**
     * 取消预约
     *
     * @param appointmentId 预约ID
     * @return 结果
     */
    public int cancelAppointment(Long appointmentId);

    /**
     * 完成预约（访客离开）
     *
     * @param appointmentId 预约ID
     * @return 结果
     */
    public int completeAppointment(Long appointmentId);

    /**
     * 查询待处理预约列表
     *
     * @return 待处理预约集合
     */
    public List<VisitorAppointment> selectPendingList();
}
