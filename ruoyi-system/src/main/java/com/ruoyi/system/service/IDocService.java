package com.ruoyi.system.service;

import java.util.List;
import java.util.Map;
import com.ruoyi.system.domain.DocSyncLog;

/**
 * 文档中心 服务层
 *
 * @author ruoyi
 */
public interface IDocService
{
    public List<Map<String, Object>> getTree();
    public Map<String, Object> getContent(String relativePath);
    public Map<String, Object> getStatus();
    public DocSyncLog syncAll();
    public List<Map<String, Object>> search(String keyword);
}
