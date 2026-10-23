package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.VisitorLog;

/**
 * 访客记录表 数据层
 *
 * @author ruoyi
 */
public interface VisitorLogMapper
{
    /**
     * 查询访客记录列表
     *
     * @param log 访客记录信息
     * @return 访客记录集合
     */
    public List<VisitorLog> selectLogList(VisitorLog log);

    /**
     * 通过记录ID查询访客记录信息
     *
     * @param logId 记录ID
     * @return 访客记录信息
     */
    public VisitorLog selectLogById(Long logId);

    /**
     * 新增访客记录
     *
     * @param log 访客记录信息
     * @return 结果
     */
    public int insertLog(VisitorLog log);

    /**
     * 修改访客记录
     *
     * @param log 访客记录信息
     * @return 结果
     */
    public int updateLog(VisitorLog log);

    public VisitorLog selectLogByPassCode(String passCode);

    /**
     * 按预约ID同步通行码到来访记录（审批通过补码后调用，保证记录表通行码与预约一致）
     *
     * @param appointmentId 预约ID
     * @param passCode 通行码
     * @return 结果
     */
    public int syncPassCodeByAppointmentId(@Param("appointmentId") Long appointmentId, @Param("passCode") String passCode);
}
