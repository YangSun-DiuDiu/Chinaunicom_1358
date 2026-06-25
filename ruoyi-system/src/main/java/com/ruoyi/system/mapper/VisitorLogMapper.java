package com.ruoyi.system.mapper;

import java.util.List;
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
}
