package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SmsLog;

/**
 * 短信日志Mapper接口
 *
 * @author ruoyi
 */
public interface SmsLogMapper
{
    /**
     * 查询短信日志列表
     *
     * @param smsLog 短信日志
     * @return 短信日志集合
     */
    public List<SmsLog> selectSmsLogList(SmsLog smsLog);

    /**
     * 查询短信日志信息
     *
     * @param smsId 短信日志主键
     * @return 短信日志
     */
    public SmsLog selectSmsLogById(Long smsId);

    /**
     * 新增短信日志
     *
     * @param smsLog 短信日志
     * @return 结果
     */
    public int insertSmsLog(SmsLog smsLog);
}
