package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorAppointmentState;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.mapper.VisitorAppointmentMapper;
import com.ruoyi.system.mapper.VisitorLogMapper;
import com.ruoyi.system.service.IVisitorLogService;

/**
 * 访客记录 服务层处理
 *
 * @author ruoyi
 */
@Service
public class VisitorLogServiceImpl implements IVisitorLogService
{
    private static final Logger log = LoggerFactory.getLogger(VisitorLogServiceImpl.class);

    @Autowired
    private VisitorLogMapper visitorLogMapper;

    @Autowired
    private VisitorAppointmentMapper visitorAppointmentMapper;

    /**
     * 查询访客记录列表
     *
     * @param log 访客记录信息
     * @return 访客记录集合
     */
    @Override
    public List<VisitorLog> selectLogList(VisitorLog log)
    {
        return visitorLogMapper.selectLogList(log);
    }

    /**
     * 通过记录ID查询访客记录信息
     *
     * @param logId 记录ID
     * @return 访客记录信息
     */
    @Override
    public VisitorLog selectLogById(Long logId)
    {
        return visitorLogMapper.selectLogById(logId);
    }

    /**
     * 新增访客记录
     *
     * @param log 访客记录信息
     * @return 结果
     */
    @Override
    public int insertLog(VisitorLog log)
    {
        return visitorLogMapper.insertLog(log);
    }

    /**
     * 修改访客记录（设置离开时间）
     *
     * @param log 访客记录信息
     * @return 结果
     */
    @Override
    public int updateLog(VisitorLog log)
    {
        VisitorLog existingLog = visitorLogMapper.selectLogById(log.getLogId());
        if (existingLog == null)
        {
            throw new ServiceException("访客记录不存在");
        }
        existingLog.setExitTime(new Date());
        int result = visitorLogMapper.updateLog(existingLog);

        // 联动预约状态：预约型记录离场 → 预约置为已完成（走状态机校验，已结束的预约幂等跳过）
        if (existingLog.getAppointmentId() != null)
        {
            VisitorAppointment appointment = visitorAppointmentMapper.selectAppointmentById(existingLog.getAppointmentId());
            if (appointment != null && VisitorAppointmentState.can(appointment.getStatus(), VisitorAppointmentState.Event.COMPLETE))
            {
                appointment.setStatus(VisitorAppointmentState.COMPLETED);
                appointment.setLeaveTime(new Date());
                visitorAppointmentMapper.updateAppointment(appointment);
            }
        }
        return result;
    }

    @Override
    public int syncPassCodeByAppointmentId(Long appointmentId, String passCode)
    {
        return visitorLogMapper.syncPassCodeByAppointmentId(appointmentId, passCode);
    }

    /**
     * 通过通行码查询访客记录
     *
     * @param passCode 通行码
     * @return 访客记录信息
     */
    @Override
    public VisitorLog selectLogByPassCode(String passCode)
    {
        return visitorLogMapper.selectLogByPassCode(passCode);
    }
}
