package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 文档同步日志 sys_doc_sync_log
 *
 * @author ruoyi
 */
public class DocSyncLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 同步时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date syncTime;

    /** 触发同步的Git commit */
    private String commitHash;

    /** 模块总数 */
    private int modulesTotal;

    /** 新增文件数 */
    private int filesAdded;

    /** 过时文件数 */
    private int filesOrphaned;

    /** 同步状态(SUCCESS/FAILED) */
    private String status;

    /** 失败原因 */
    private String errorMsg;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Date getSyncTime() { return syncTime; }
    public void setSyncTime(Date syncTime) { this.syncTime = syncTime; }

    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }

    public int getModulesTotal() { return modulesTotal; }
    public void setModulesTotal(int modulesTotal) { this.modulesTotal = modulesTotal; }

    public int getFilesAdded() { return filesAdded; }
    public void setFilesAdded(int filesAdded) { this.filesAdded = filesAdded; }

    public int getFilesOrphaned() { return filesOrphaned; }
    public void setFilesOrphaned(int filesOrphaned) { this.filesOrphaned = filesOrphaned; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
}
