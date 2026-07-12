package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.NetReport;

/**
 * 网络运行报告 Service
 *
 * @author ruoyi
 */
public interface INetReportService
{
    /** 生成指定周期报告并存库 */
    NetReport generateReport(String reportType);

    /** 查询报告列表 */
    List<NetReport> selectNetReportList(NetReport report);

    /** 查询报告详情 */
    NetReport selectNetReportById(Long reportId);

    /** 逻辑删除报告 */
    int deleteNetReportById(Long reportId);
}
