package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.VisitorAppointment;

/**
 * 访客预约表 数据层
 *
 * @author ruoyi
 */
public interface VisitorAppointmentMapper
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
     * 删除预约
     *
     * @param appointmentId 预约ID
     * @return 结果
     */
    public int deleteAppointmentById(Long appointmentId);

    /**
     * 查询待处理预约列表（可按审批人过滤）
     *
     * @param approverId 审批人ID（null=查全部）
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
     * 通过访客手机号查询预约列表（公开查询用，按创建时间倒序）
     *
     * @param visitorPhone 访客手机号
     * @return 预约概要列表
     */
    public java.util.List<com.ruoyi.system.domain.vo.AppointmentBriefDTO> selectAppointmentBriefByPhone(String visitorPhone);

    /**
     * 通过昵称（nick_name）查询用户ID，用于被访人→审批人匹配
     *
     * @param nickName 用户昵称
     * @return 用户ID，未找到返回null
     */
    public Long selectUserIdByNickName(String nickName);

    /**
     * 通过昵称查询用户（含 dept_id），用于被访人→审批人匹配后设置数据范围
     * @return Map 包含 user_id, dept_id
     */
    public java.util.Map<String, Object> selectUserByNickName(String nickName);

    /**
     * warm-flow 代批认领：把指定待办任务的审批权限人(flow_user.processed_by)更新为当前操作人
     * （引擎 skip/终止均按 flow_user.processed_by 校验 handler 权限，代批必须先认领）
     *
     * @param taskId   待办任务ID（flow_task.id = flow_user.associated）
     * @param operator 当前操作人ID（字符串）
     * @return 更新的行数
     */
    public int claimFlowUser(@org.apache.ibatis.annotations.Param("taskId") Long taskId,
                             @org.apache.ibatis.annotations.Param("operator") String operator);
}
