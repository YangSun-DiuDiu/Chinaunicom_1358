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
     * @param approverId 审批人ID（null=查全部，非null=按审批人过滤）
     * @return 待处理预约集合
     */
    public List<VisitorAppointment> selectPendingList(Long approverId);

    /**
     * 通过通行码查询预约
     *
     * @param passCode 通行码
     * @return 预约信息
     */
    public VisitorAppointment selectAppointmentByPassCode(String passCode);

    /**
     * 到访确认：通行码验证通过后将预约状态更新为 VISITING
     *
     * @param passCode 通行码
     * @return 更新后的预约信息，未找到返回null
     */
    public VisitorAppointment checkInVisitor(String passCode);

    /**
     * 访客自助登记/现场登记：事务中创建预约+来访记录
     *
     * @param appointment 预约信息
     * @param createBy 创建者
     * @return Map 包含 passCode、appointmentId、log
     */
    public java.util.Map<String, Object> registerWalkin(VisitorAppointment appointment, String createBy);
}
