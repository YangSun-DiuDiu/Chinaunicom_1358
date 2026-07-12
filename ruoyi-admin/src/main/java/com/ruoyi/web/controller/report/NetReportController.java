package com.ruoyi.web.controller.report;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.NetReport;
import com.ruoyi.system.service.INetReportService;

/**
 * 网络运行报告 Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/report")
public class NetReportController extends BaseController
{
    @Autowired
    private INetReportService netReportService;

    /** 查询报告列表 */
    @PreAuthorize("@ss.hasPermi('report:net:list')")
    @GetMapping("/list")
    public TableDataInfo list(NetReport report)
    {
        startPage();
        List<NetReport> list = netReportService.selectNetReportList(report);
        return getDataTable(list);
    }

    /** 查询报告详情 */
    @PreAuthorize("@ss.hasPermi('report:net:query')")
    @GetMapping("/{reportId}")
    public AjaxResult getInfo(@PathVariable Long reportId)
    {
        return success(netReportService.selectNetReportById(reportId));
    }

    /** 生成指定周期报告 */
    @PreAuthorize("@ss.hasPermi('report:net:add')")
    @PostMapping("/generate/{reportType}")
    public AjaxResult generate(@PathVariable String reportType)
    {
        return success(netReportService.generateReport(reportType));
    }

    /** 逻辑删除报告 */
    @PreAuthorize("@ss.hasPermi('report:net:remove')")
    @DeleteMapping("/{reportId}")
    public AjaxResult remove(@PathVariable Long reportId)
    {
        return toAjax(netReportService.deleteNetReportById(reportId));
    }
}
