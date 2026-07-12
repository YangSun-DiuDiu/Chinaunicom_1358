package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.NetReport;

/**
 * 网络运行报告 Mapper 接口
 *
 * @author system
 */
public interface NetReportMapper
{
    /**
     * 新增网络运行报告
     *
     * @param report 网络运行报告
     * @return 结果
     */
    int insertNetReport(NetReport report);

    /**
     * 查询网络运行报告列表
     *
     * @param report 网络运行报告
     * @return 网络运行报告集合
     */
    List<NetReport> selectNetReportList(NetReport report);

    /**
     * 查询网络运行报告
     *
     * @param reportId 报告ID
     * @return 网络运行报告
     */
    NetReport selectNetReportById(Long reportId);

    /**
     * 删除网络运行报告(逻辑删除)
     *
     * @param reportId 报告ID
     * @return 结果
     */
    int deleteNetReportById(Long reportId);
}
