package com.ruoyi.quartz.task;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.service.IDocService;

/**
 * 文档同步定时任务 —— 每分钟检测 Git 变化并同步文档骨架
 *
 * @author ruoyi
 */
@Component("docSyncTask")
public class DocSyncTask
{
    private static final Logger log = LoggerFactory.getLogger(DocSyncTask.class);

    @Autowired
    private IDocService docService;

    private String lastCommitHash = "";

    public void checkAndSync()
    {
        try
        {
            Map<String, Object> status = docService.getStatus();
            String currentHash = (String) status.getOrDefault("currentCommitHash", "");
            if (!currentHash.isEmpty() && !currentHash.equals(lastCommitHash))
            {
                log.info("检测到 Git 变化 ({} -> {})，触发文档同步", lastCommitHash, currentHash);
                docService.syncAll();
                lastCommitHash = currentHash;
            }
        }
        catch (Exception e)
        {
            log.error("文档同步定时任务执行失败", e);
        }
    }
}
