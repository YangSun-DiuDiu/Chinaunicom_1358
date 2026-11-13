package com.ruoyi.system.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.system.domain.VisitorLog;

/**
 * 访客记录 服务层
 *
 * @author ruoyi
 */
public interface IVisitorLogService
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
     * 修改访客记录（设置离开时间）
     *
     * @param log 访客记录信息
     * @return 结果
     */
    public int updateLog(VisitorLog log);

    /**
     * 通过通行码查询访客记录
     *
     * @param passCode 通行码
     * @return 访客记录信息
     */
    public VisitorLog selectLogByPassCode(String passCode);

    /**
     * 按预约ID同步通行码到来访记录（审批通过补码后调用，保证记录表通行码与预约一致）
     *
     * @param appointmentId 预约ID
     * @param passCode      通行码
     * @return 结果
     */
    public int syncPassCodeByAppointmentId(Long appointmentId, String passCode);

    /**
     * 写入实际入园时间（门卫扫码入场核验时调用）
     *
     * <p>幂等：仅当该记录 entry_time 为空时写入，重复核验不覆盖首次入园时间。</p>
     *
     * @param passCode  通行码
     * @param entryTime 入园时间
     * @return 更新条数（0 表示记录不存在或已写过入园时间）
     */
    public int markEntryTime(String passCode, Date entryTime);
}
