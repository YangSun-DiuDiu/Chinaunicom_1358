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
     * 查询待处理预约列表
     *
     * @return 待处理预约集合
     */
    public List<VisitorAppointment> selectPendingList();

    /**
     * 通过通行码查询预约
     *
     * @param passCode 通行码
     * @return 预约信息
     */
    public VisitorAppointment selectAppointmentByPassCode(String passCode);
}
