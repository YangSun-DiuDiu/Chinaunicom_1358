package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.DocSyncLog;

/**
 * 文档同步日志 Mapper
 *
 * @author ruoyi
 */
public interface DocSyncLogMapper
{
    /**
     * 新增同步日志
     *
     * @param log 同步日志
     * @return 结果
     */
    public int insertLog(DocSyncLog log);

    /**
     * 查询最近一次同步日志
     *
     * @return 最近一次同步日志
     */
    public DocSyncLog selectLastSyncLog();
}
