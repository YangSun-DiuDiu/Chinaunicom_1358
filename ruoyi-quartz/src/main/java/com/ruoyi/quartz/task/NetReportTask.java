package com.ruoyi.quartz.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.service.INetReportService;

/** 网络运行报告定时任务（sys_job 配置 invoke_target=netReportTask.daily() 等） */
@Component("netReportTask")
public class NetReportTask {
    @Autowired
    private INetReportService netReportService;

    public void daily()     { netReportService.generateReport("DAILY"); }
    public void weekly()    { netReportService.generateReport("WEEKLY"); }
    public void monthly()   { netReportService.generateReport("MONTHLY"); }
    public void quarterly() { netReportService.generateReport("QUARTERLY"); }
    public void halfYear()  { netReportService.generateReport("HALF_YEAR"); }
    public void yearly()    { netReportService.generateReport("YEARLY"); }
}
