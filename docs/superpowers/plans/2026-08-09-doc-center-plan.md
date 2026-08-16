# 文档中心模块 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增「文档中心」模块——自动扫描代码包结构生成 Markdown 文档骨架，持续 Git 同步，匿名可访问的阅读型 Web 页面。

**Architecture:** 后端 DocSyncService 以约定式映射规则（源码路径→docs/目录）扫描 Controller/Service/Mapper/Entity/前端/数据库源码，生成含 frontmatter 的 .md 骨架文件；Quartz 定时任务每 60s 检测 Git commit 变化触发同步。前端左右分栏布局（DocTree + DocReader），marked + highlight.js 渲染 Markdown，Code Atlas 设计体系。

**Tech Stack:** Java 21 / Spring Boot 3 / MyBatis / Quartz / JGit / Vue 2.6 / element-ui 2.15 / marked 4.x / highlight.js 9.18（已有）

## Global Constraints

- 不改若依底层核心源码（ruoyi-framework / ruoyi-common 固定逻辑）
- 所有 Controller 继承 `BaseController`，使用 `@PreAuthorize` 权限控制
- 统一返回 `AjaxResult` / `TableDataInfo`
- 遵循项目 `Controller → Service → ServiceImpl → Mapper → Entity` 分层
- 前端使用 Vue 2 + element-ui，路由全部声明在 `staticRoutes`
- 文档中心匿名接口不走权限校验，在 SecurityConfig + permission.js + sys_menu 中配置白名单
- 路径安全：所有文件 I/O 操作校验路径不含 `..`，统一 UTF-8 编码
- Git 不可用时降级跳过同步，返回上次成功状态
- 所有代码风格对齐 RuoYi 官方示例

---

## 文件清单

### 新建文件

| 文件 | 职责 |
|------|------|
| `sql/doc_center.sql` | 建表 + 菜单 + 定时任务 SQL |
| `ruoyi-system/src/main/java/com/ruoyi/system/domain/DocSyncLog.java` | 同步日志实体 |
| `ruoyi-system/src/main/java/com/ruoyi/system/mapper/DocSyncLogMapper.java` | 日志 Mapper 接口 |
| `ruoyi-system/src/main/resources/mapper/system/DocSyncLogMapper.xml` | MyBatis XML |
| `ruoyi-system/src/main/java/com/ruoyi/system/service/IDocService.java` | 文档服务接口 |
| `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/DocServiceImpl.java` | 扫描引擎 + 读取 + 同步实现 |
| `ruoyi-admin/src/main/java/com/ruoyi/web/controller/doc/DocController.java` | 文档中心 REST 接口 |
| `ruoyi-quartz/src/main/java/com/ruoyi/quartz/task/DocSyncTask.java` | 定时同步任务 |
| `ruoyi-ui/src/api/doc/doc.js` | 前端 API 封装 |
| `ruoyi-ui/src/views/doc/index.vue` | 文档中心主页面 |
| `ruoyi-ui/src/views/doc/components/DocTree.vue` | 左侧章节树组件 |
| `ruoyi-ui/src/views/doc/components/DocReader.vue` | 右侧 Markdown 渲染组件 |
| `ruoyi-ui/src/views/doc/styles/atlas.scss` | Code Atlas 设计体系样式 |
| `docs/module-order.yml` | 模块排序配置 |

### 修改文件

| 文件 | 改动 |
|------|------|
| `ruoyi-system/pom.xml` | 新增 `org.eclipse.jgit` 依赖 |
| `ruoyi-framework/.../config/SecurityConfig.java` | `/doc/tree`, `/doc/content`, `/doc/status`, `/doc/search` 加入 permitAll |
| `ruoyi-ui/src/router/index.js` | 新增 `/doc` 静态路由块 |
| `ruoyi-ui/src/permission.js` | `/doc` 加入 whiteList |

---

## Phase 1: 后端（P0）

### Task 1: 数据库建表 + 菜单 + 定时任务 SQL

**Files:**
- Create: `sql/doc_center.sql`

- [ ] **Step 1: 编写 SQL 文件**

```sql
-- ============================================================
-- 文档中心模块 — 建表 + 菜单 + 定时任务
-- ============================================================

-- 1. 同步日志表
DROP TABLE IF EXISTS `sys_doc_sync_log`;
CREATE TABLE `sys_doc_sync_log` (
    `id` BIGINT(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `sync_time` DATETIME NOT NULL COMMENT '同步时间',
    `commit_hash` VARCHAR(64) DEFAULT '' COMMENT '触发同步的Git commit',
    `modules_total` INT(11) DEFAULT 0 COMMENT '模块总数',
    `files_added` INT(11) DEFAULT 0 COMMENT '新增文件数',
    `files_orphaned` INT(11) DEFAULT 0 COMMENT '过时文件数',
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUCCESS' COMMENT '同步状态(SUCCESS/FAILED)',
    `error_msg` VARCHAR(2000) DEFAULT '' COMMENT '失败原因',
    `create_by` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT NULL COMMENT '创建时间',
    `update_by` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT NULL COMMENT '更新时间',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COMMENT='文档同步日志表';

-- 2. 文档中心菜单 (parentMenuId: 2150)
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文档中心', 0, 9, 'doc', NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', 'admin', NOW(), '', NULL, '文档中心目录');

-- 获取刚插入的 menu_id
SET @doc_menu_id = LAST_INSERT_ID();

-- 文档中心-查看页面
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文档浏览', @doc_menu_id, 1, '', 'doc/index', 1, 0, 'C', '0', '0', 'doc:view', '#', 'admin', NOW(), '', NULL, '文档中心浏览页面');

SET @doc_view_id = LAST_INSERT_ID();

-- 文档中心-手动同步按钮
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('手动同步', @doc_view_id, 1, '', '', 1, 0, 'F', '0', '0', 'doc:sync', '#', 'admin', NOW(), '', NULL, '文档中心手动同步');

-- 3. 定时任务 (每分钟检查 Git 变化)
INSERT INTO `sys_job` (`job_name`, `job_group`, `invoke_target`, `cron_expression`, `misfire_policy`, `concurrent`, `status`, `create_by`, `create_time`, `update_by`, `update_time`, `remark`)
VALUES ('文档同步任务', 'SYSTEM', 'docSyncTask.checkAndSync()', '0 * * * * ?', '1', '1', '0', 'admin', NOW(), '', NULL, '每分钟检查Git commit变化并同步文档骨架');
```

- [ ] **Step 2: 提交 SQL**

```bash
git add sql/doc_center.sql
git commit -m "feat(doc): 文档中心建表+菜单+定时任务SQL"
```

---

### Task 2: DocSyncLog 实体类

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/domain/DocSyncLog.java`

**Interfaces:**
- Produces: `DocSyncLog` — standard domain class extending `BaseEntity`, fields: `id(Long)`, `syncTime(Date)`, `commitHash(String)`, `modulesTotal(int)`, `filesAdded(int)`, `filesOrphaned(int)`, `status(String)`, `errorMsg(String)`

- [ ] **Step 1: 编写实体类**

```java
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
```

- [ ] **Step 2: 编译验证**

```bash
cd ruoyi-system && mvn compile -q
```

---

### Task 3: DocSyncLogMapper + XML

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/mapper/DocSyncLogMapper.java`
- Create: `ruoyi-system/src/main/resources/mapper/system/DocSyncLogMapper.xml`

**Interfaces:**
- Produces: `DocSyncLogMapper.insertLog(DocSyncLog)` returns `int`, `DocSyncLogMapper.selectLastSyncLog()` returns `DocSyncLog`

- [ ] **Step 1: 编写 Mapper 接口**

```java
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
```

- [ ] **Step 2: 编写 MyBatis XML**

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.ruoyi.system.mapper.DocSyncLogMapper">

    <resultMap type="com.ruoyi.system.domain.DocSyncLog" id="DocSyncLogResult">
        <id     property="id"           column="id"            />
        <result property="syncTime"     column="sync_time"     />
        <result property="commitHash"   column="commit_hash"   />
        <result property="modulesTotal" column="modules_total" />
        <result property="filesAdded"   column="files_added"   />
        <result property="filesOrphaned" column="files_orphaned"/>
        <result property="status"       column="status"        />
        <result property="errorMsg"     column="error_msg"     />
        <result property="createBy"     column="create_by"     />
        <result property="createTime"   column="create_time"   />
        <result property="updateBy"     column="update_by"     />
        <result property="updateTime"   column="update_time"   />
        <result property="remark"       column="remark"        />
    </resultMap>

    <insert id="insertLog" parameterType="com.ruoyi.system.domain.DocSyncLog" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO sys_doc_sync_log (
            sync_time, commit_hash, modules_total, files_added, files_orphaned,
            status, error_msg, create_by, create_time
        ) VALUES (
            #{syncTime}, #{commitHash}, #{modulesTotal}, #{filesAdded}, #{filesOrphaned},
            #{status}, #{errorMsg}, #{createBy}, sysdate()
        )
    </insert>

    <select id="selectLastSyncLog" resultMap="DocSyncLogResult">
        SELECT id, sync_time, commit_hash, modules_total, files_added, files_orphaned,
               status, error_msg, create_by, create_time
        FROM sys_doc_sync_log
        WHERE status = 'SUCCESS'
        ORDER BY sync_time DESC
        LIMIT 1
    </select>

</mapper>
```

- [ ] **Step 3: 编译验证**

```bash
cd ruoyi-system && mvn compile -q
```

---

### Task 4: IDocService 接口 + DocServiceImpl 扫描引擎

**Files:**
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/IDocService.java`
- Create: `ruoyi-system/src/main/java/com/ruoyi/system/service/impl/DocServiceImpl.java`

**Interfaces:**
- Consumes: `DocSyncLogMapper.insertLog(DocSyncLog)`, `DocSyncLogMapper.selectLastSyncLog()`
- Produces: `IDocService.getTree()` returns `List<Map<String,Object>>`, `IDocService.getContent(String relativePath)` returns `Map<String,Object>`, `IDocService.getStatus()` returns `Map<String,Object>`, `IDocService.syncAll()` returns `DocSyncLog`, `IDocService.search(String keyword)` returns `List<Map<String,Object>>`

- [ ] **Step 1: 编写服务接口**

```java
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
    /**
     * 获取文档目录树
     * @return 树结构列表
     */
    public List<Map<String, Object>> getTree();

    /**
     * 获取文档内容
     * @param relativePath docs/ 相对路径
     * @return 文档内容(frontmatter + markdown + html)
     */
    public Map<String, Object> getContent(String relativePath);

    /**
     * 获取同步状态摘要
     * @return 同步状态信息
     */
    public Map<String, Object> getStatus();

    /**
     * 执行全量同步
     * @return 同步日志
     */
    public DocSyncLog syncAll();

    /**
     * 搜索文档
     * @param keyword 关键字
     * @return 匹配的文档列表
     */
    public List<Map<String, Object>> search(String keyword);
}
```

- [ ] **Step 2: 编写 DocServiceImpl**

```java
package com.ruoyi.system.service.impl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.DocSyncLog;
import com.ruoyi.system.mapper.DocSyncLogMapper;
import com.ruoyi.system.service.IDocService;

/**
 * 文档中心 业务层处理
 *
 * @author ruoyi
 */
@Service
public class DocServiceImpl implements IDocService
{
    private static final Logger log = LoggerFactory.getLogger(DocServiceImpl.class);

    /** 项目根目录 */
    @Value("${user.dir}")
    private String projectRoot;

    /** docs 文档根目录 */
    private static final String DOCS_DIR = "docs";

    /** 前端视图根目录 */
    private static final String VIEWS_DIR = "ruoyi-ui/src/views";

    /** 前端 API 根目录 */
    private static final String API_DIR = "ruoyi-ui/src/api";

    /** 后端控制器根目录 */
    private static final String CONTROLLER_DIR = "ruoyi-admin/src/main/java/com/ruoyi/web/controller";

    /** 后端 Service/Mapper/Domain 根目录 */
    private static final String SYSTEM_SRC = "ruoyi-system/src/main/java/com/ruoyi/system";

    /** Mapper XML 根目录 */
    private static final String MAPPER_XML_DIR = "ruoyi-system/src/main/resources/mapper/system";

    /** SQL 脚本目录 */
    private static final String SQL_DIR = "sql";

    /** 模块名中文映射 */
    private static final Map<String, String> MODULE_NAME_MAP = new LinkedHashMap<>();
    static {
        MODULE_NAME_MAP.put("device", "设备管理");
        MODULE_NAME_MAP.put("meeting", "会议管理");
        MODULE_NAME_MAP.put("visitor", "访客管理");
        MODULE_NAME_MAP.put("hostel", "公寓管理");
        MODULE_NAME_MAP.put("smart", "智能化管理");
        MODULE_NAME_MAP.put("attendance", "考勤管理");
        MODULE_NAME_MAP.put("sms", "短信管理");
        MODULE_NAME_MAP.put("system", "系统管理");
    }

    @Autowired
    private DocSyncLogMapper docSyncLogMapper;

    /**
     * 获取文档目录树
     */
    @Override
    public List<Map<String, Object>> getTree()
    {
        List<Map<String, Object>> modules = new ArrayList<>();
        Path docsPath = Paths.get(projectRoot, DOCS_DIR);
        if (!Files.exists(docsPath))
        {
            return modules;
        }

        // 列出 docs/ 下所有子目录（模块目录），按数字前缀排序
        try (Stream<Path> dirs = Files.list(docsPath))
        {
            List<Path> moduleDirs = dirs
                .filter(Files::isDirectory)
                .filter(p -> p.getFileName().toString().matches("\\d{2}-.+"))
                .sorted((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                .collect(Collectors.toList());

            for (Path moduleDir : moduleDirs)
            {
                Map<String, Object> module = buildTreeNode(moduleDir, true);
                if (module != null)
                {
                    modules.add(module);
                }
            }
        }
        catch (IOException e)
        {
            log.error("读取文档目录失败", e);
        }

        return modules;
    }

    /**
     * 递归构建树节点
     */
    private Map<String, Object> buildTreeNode(Path path, boolean isModule)
    {
        Map<String, Object> node = new LinkedHashMap<>();
        String fileName = path.getFileName().toString();
        boolean isDirectory = Files.isDirectory(path);

        if (isDirectory)
        {
            // 文件夹节点：去除数字前缀得到显示名
            String displayName = fileName.replaceFirst("^\\d{2}-", "");
            node.put("name", displayName);
            node.put("type", "folder");
            node.put("path", projectRoot.relativize(path).toString().replace('\\', '/'));

            List<Map<String, Object>> children = new ArrayList<>();
            try (Stream<Path> entries = Files.list(path))
            {
                List<Path> sorted = entries
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .sorted((a, b) -> sortByDirectory(a, b))
                    .collect(Collectors.toList());

                int totalFiles = 0;
                int filledFiles = 0;

                for (Path child : sorted)
                {
                    Map<String, Object> childNode = buildTreeNode(child, false);
                    if (childNode != null)
                    {
                        children.add(childNode);
                    }
                }

                // 统计子节点填充情况
                for (Map<String, Object> child : children)
                {
                    if ("file".equals(child.get("type")))
                    {
                        totalFiles++;
                        if (Boolean.FALSE.equals(child.get("autoGenerated")))
                        {
                            filledFiles++;
                        }
                    }
                    else
                    {
                        totalFiles += (int) child.getOrDefault("totalFiles", 0);
                        filledFiles += (int) child.getOrDefault("filledFiles", 0);
                    }
                }

                node.put("totalFiles", totalFiles);
                node.put("filledFiles", filledFiles);
            }
            catch (IOException e)
            {
                log.warn("读取子目录失败: {}", path, e);
            }

            node.put("children", children);
        }
        else if (fileName.endsWith(".md"))
        {
            // 文件节点：读取 frontmatter 获取元数据
            String displayName = fileName.replace(".md", "");
            String relativePath = projectRoot.relativize(path).toString().replace('\\', '/');

            Map<String, String> frontmatter = parseFrontmatter(path);
            boolean autoGenerated = "true".equals(frontmatter.get("autoGenerated"));
            boolean orphaned = "true".equals(frontmatter.get("orphaned"));

            node.put("name", displayName);
            node.put("type", "file");
            node.put("path", relativePath);
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("sourceType", frontmatter.getOrDefault("type", ""));
            meta.put("autoGenerated", autoGenerated);
            meta.put("orphaned", orphaned);
            meta.put("sourceFile", frontmatter.getOrDefault("source", ""));
            node.put("meta", meta);
        }

        return node;
    }

    /**
     * 目录优先、字母序排序
     */
    private int sortByDirectory(Path a, Path b)
    {
        boolean aDir = Files.isDirectory(a);
        boolean bDir = Files.isDirectory(b);
        if (aDir && !bDir) return -1;
        if (!aDir && bDir) return 1;
        // 子文件夹中 "后端" 优先，"前端" 其次，"数据库" 最后
        String aName = a.getFileName().toString();
        String bName = b.getFileName().toString();
        Map<String, Integer> order = new HashMap<>();
        order.put("后端", 1);
        order.put("前端", 2);
        order.put("数据库", 3);
        int aOrd = order.getOrDefault(aName, 9);
        int bOrd = order.getOrDefault(bName, 9);
        if (aOrd != bOrd) return Integer.compare(aOrd, bOrd);
        return aName.compareTo(bName);
    }

    /**
     * 解析 Markdown 文件的 YAML frontmatter
     */
    private Map<String, String> parseFrontmatter(Path filePath)
    {
        Map<String, String> result = new HashMap<>();
        try
        {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            if (lines.size() > 1 && "---".equals(lines.get(0).trim()))
            {
                for (int i = 1; i < lines.size(); i++)
                {
                    String line = lines.get(i);
                    if ("---".equals(line.trim())) break;
                    int colonIdx = line.indexOf(':');
                    if (colonIdx > 0)
                    {
                        String key = line.substring(0, colonIdx).trim();
                        String value = line.substring(colonIdx + 1).trim();
                        result.put(key, value);
                    }
                }
            }
        }
        catch (IOException e)
        {
            log.warn("解析 frontmatter 失败: {}", filePath, e);
        }
        return result;
    }

    /**
     * 获取文档内容
     */
    @Override
    public Map<String, Object> getContent(String relativePath)
    {
        // 安全检查：防止目录穿越
        if (relativePath == null || relativePath.contains(".."))
        {
            throw new IllegalArgumentException("非法路径");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        Path filePath = Paths.get(projectRoot, relativePath);

        if (!Files.exists(filePath) || !Files.isReadable(filePath))
        {
            result.put("error", "文件不存在");
            return result;
        }

        try
        {
            String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            Map<String, String> frontmatter = parseFrontmatter(filePath);

            // 分离 frontmatter 和正文
            String markdown = content;
            if (content.startsWith("---"))
            {
                int endIdx = content.indexOf("---", 3);
                if (endIdx > 0)
                {
                    markdown = content.substring(endIdx + 3).trim();
                }
            }

            result.put("path", relativePath);
            result.put("frontmatter", frontmatter);
            result.put("markdown", markdown);
            result.put("autoGenerated", "true".equals(frontmatter.get("autoGenerated")));
            result.put("orphaned", "true".equals(frontmatter.get("orphaned")));
        }
        catch (IOException e)
        {
            log.error("读取文件失败: {}", filePath, e);
            result.put("error", "文件读取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取同步状态摘要
     */
    @Override
    public Map<String, Object> getStatus()
    {
        Map<String, Object> status = new LinkedHashMap<>();
        DocSyncLog lastLog = docSyncLogMapper.selectLastSyncLog();

        // 统计模块和文件数
        List<Map<String, Object>> tree = getTree();
        int totalModules = tree.size();
        int totalFiles = 0;
        int autoGenFiles = 0;
        int orphanedFiles = 0;
        for (Map<String, Object> mod : tree)
        {
            totalFiles += (int) mod.getOrDefault("totalFiles", 0);
            autoGenFiles += countAutoGen(mod);
            orphanedFiles += countOrphaned(mod);
        }

        status.put("totalModules", totalModules);
        status.put("totalFiles", totalFiles);
        status.put("autoGenFiles", autoGenFiles);
        status.put("filledFiles", totalFiles - autoGenFiles);
        status.put("orphanedFiles", orphanedFiles);

        if (lastLog != null)
        {
            status.put("lastSyncTime", lastLog.getSyncTime());
            status.put("lastCommitHash", lastLog.getCommitHash());
        }
        else
        {
            status.put("lastSyncTime", null);
            status.put("lastCommitHash", "");
        }

        // 获取当前 Git commit hash
        status.put("currentCommitHash", getCurrentCommitHash());

        return status;
    }

    /**
     * 递归统计 autoGenerated 文件数
     */
    @SuppressWarnings("unchecked")
    private int countAutoGen(Map<String, Object> node)
    {
        int count = 0;
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        if (children != null)
        {
            for (Map<String, Object> child : children)
            {
                if ("file".equals(child.get("type")))
                {
                    Map<String, Object> meta = (Map<String, Object>) child.get("meta");
                    if (meta != null && Boolean.TRUE.equals(meta.get("autoGenerated")))
                    {
                        count++;
                    }
                }
                else
                {
                    count += countAutoGen(child);
                }
            }
        }
        return count;
    }

    /**
     * 递归统计 orphaned 文件数
     */
    @SuppressWarnings("unchecked")
    private int countOrphaned(Map<String, Object> node)
    {
        int count = 0;
        List<Map<String, Object>> children = (List<Map<String, Object>>) node.get("children");
        if (children != null)
        {
            for (Map<String, Object> child : children)
            {
                if ("file".equals(child.get("type")))
                {
                    Map<String, Object> meta = (Map<String, Object>) child.get("meta");
                    if (meta != null && Boolean.TRUE.equals(meta.get("orphaned")))
                    {
                        count++;
                    }
                }
                else
                {
                    count += countOrphaned(child);
                }
            }
        }
        return count;
    }

    /**
     * 执行全量同步 —— 扫描代码结构，生成/更新 docs/ 骨架文件
     */
    @Override
    public DocSyncLog syncAll()
    {
        DocSyncLog syncLog = new DocSyncLog();
        syncLog.setSyncTime(new Date());
        syncLog.setStatus("SUCCESS");
        syncLog.setCreateBy("SYSTEM");

        int filesAdded = 0;
        int filesOrphaned = 0;
        int modulesTotal = 0;

        try
        {
            String commitHash = getCurrentCommitHash();
            syncLog.setCommitHash(commitHash);

            // 确保 docs/ 目录存在
            Path docsRoot = Paths.get(projectRoot, DOCS_DIR);
            Files.createDirectories(docsRoot);

            // 确保 module-order.yml 存在
            Path moduleOrderFile = docsRoot.resolve("module-order.yml");
            if (!Files.exists(moduleOrderFile))
            {
                StringBuilder yml = new StringBuilder("# 文档中心模块排序 (数字越小越靠前)\n");
                int order = 1;
                for (String dirName : MODULE_NAME_MAP.keySet())
                {
                    yml.append(String.format("%02d: %s  # %s\n", order++, dirName,
                        MODULE_NAME_MAP.get(dirName)));
                }
                Files.write(moduleOrderFile, yml.toString().getBytes(StandardCharsets.UTF_8));
            }

            // 扫描每个模块
            Path controllerRoot = Paths.get(projectRoot, CONTROLLER_DIR);
            if (Files.exists(controllerRoot))
            {
                try (Stream<Path> moduleDirs = Files.list(controllerRoot).filter(Files::isDirectory))
                {
                    for (Path moduleDir : moduleDirs.collect(Collectors.toList()))
                    {
                        String moduleKey = moduleDir.getFileName().toString();
                        // 跳过 common 目录（非业务模块）
                        if ("common".equals(moduleKey)) continue;

                        String moduleName = MODULE_NAME_MAP.getOrDefault(moduleKey, moduleKey);
                        // 查找模块序号
                        int sortOrder = 1;
                        for (String k : MODULE_NAME_MAP.keySet())
                        {
                            if (k.equals(moduleKey)) break;
                            sortOrder++;
                        }

                        String moduleDirName = String.format("%02d-%s", sortOrder, moduleName);
                        Path moduleDocDir = docsRoot.resolve(moduleDirName);
                        Files.createDirectories(moduleDocDir);
                        modulesTotal++;

                        // 确保 README.md
                        Path readme = moduleDocDir.resolve("README.md");
                        if (!Files.exists(readme))
                        {
                            String skeleton = generateModuleReadme(moduleName, moduleKey);
                            Files.write(readme, skeleton.getBytes(StandardCharsets.UTF_8));
                            filesAdded++;
                        }

                        // 扫描后端源码
                        filesAdded += scanBackendSources(moduleKey, moduleName, moduleDocDir);

                        // 扫描前端源码
                        filesAdded += scanFrontendSources(moduleKey, moduleName, moduleDocDir);

                        // 扫描数据库 SQL
                        filesAdded += scanSqlSources(moduleKey, moduleName, moduleDocDir);
                    }
                }
            }

            // 标记过时文件：docs 中有但源码中没有的
            filesOrphaned = markOrphanedFiles(docsRoot);

            syncLog.setModulesTotal(modulesTotal);
            syncLog.setFilesAdded(filesAdded);
            syncLog.setFilesOrphaned(filesOrphaned);

            // 写入同步日志
            docSyncLogMapper.insertLog(syncLog);

            log.info("文档同步完成: {} 模块, +{} 新增, {} 过时", modulesTotal, filesAdded, filesOrphaned);
        }
        catch (Exception e)
        {
            syncLog.setStatus("FAILED");
            syncLog.setErrorMsg(e.getMessage() != null ? e.getMessage().substring(0, Math.min(1999, e.getMessage().length())) : "未知错误");
            log.error("文档同步失败", e);
            try { docSyncLogMapper.insertLog(syncLog); } catch (Exception ignored) {}
        }

        return syncLog;
    }

    /**
     * 生成模块 README.md
     */
    private String generateModuleReadme(String moduleName, String moduleKey)
    {
        return "---\n" +
            "module: " + moduleName + "\n" +
            "moduleKey: " + moduleKey + "\n" +
            "type: ModuleOverview\n" +
            "autoGenerated: true\n" +
            "createdAt: " + new Date().toString() + "\n" +
            "---\n\n" +
            "# " + moduleName + "\n\n" +
            "> 此文件由系统自动生成骨架，请填写模块概述。\n\n" +
            "## 模块概述\n\n" +
            "<!-- TODO: 描述模块的业务范围、核心功能 -->\n\n" +
            "## 模块架构\n\n" +
            "<!-- TODO: 描述模块的代码包结构、分层设计 -->\n\n" +
            "## 核心流程\n\n" +
            "<!-- TODO: 描述模块的关键业务流程 -->\n\n";
    }

    /**
     * 扫描后端源码（Controller, Service, Mapper, Entity, Mapper XML）
     */
    private int scanBackendSources(String moduleKey, String moduleName, Path moduleDocDir) throws IOException
    {
        int added = 0;
        Path backendDir = moduleDocDir.resolve("后端");
        Files.createDirectories(backendDir);

        // 扫描 Controller
        Path controllerDir = Paths.get(projectRoot, CONTROLLER_DIR, moduleKey);
        if (Files.exists(controllerDir))
        {
            try (Stream<Path> files = Files.list(controllerDir).filter(f -> f.toString().endsWith(".java")))
            {
                for (Path javaFile : files.collect(Collectors.toList()))
                {
                    String className = javaFile.getFileName().toString().replace(".java", "");
                    Path mdFile = backendDir.resolve(className + ".md");
                    if (!Files.exists(mdFile))
                    {
                        String srcRelPath = projectRoot.relativize(javaFile).toString().replace('\\', '/');
                        String skeleton = generateControllerSkeleton(className, srcRelPath, moduleName, javaFile);
                        Files.write(mdFile, skeleton.getBytes(StandardCharsets.UTF_8));
                        added++;
                    }
                }
            }
        }

        // 扫描 Service 实现类
        Path serviceImplDir = Paths.get(projectRoot, SYSTEM_SRC, "service/impl");
        if (Files.exists(serviceImplDir))
        {
            try (Stream<Path> files = Files.list(serviceImplDir))
            {
                for (Path javaFile : files.collect(Collectors.toList()))
                {
                    String fileName = javaFile.getFileName().toString();
                    // 匹配模块相关 ServiceImpl（按命名约定）
                    if (fileName.endsWith("ServiceImpl.java") && isRelatedToModule(fileName, moduleKey))
                    {
                        String className = fileName.replace(".java", "");
                        Path mdFile = backendDir.resolve(className + ".md");
                        if (!Files.exists(mdFile))
                        {
                            String srcRelPath = projectRoot.relativize(javaFile).toString().replace('\\', '/');
                            String skeleton = generateServiceSkeleton(className, srcRelPath, moduleName);
                            Files.write(mdFile, skeleton.getBytes(StandardCharsets.UTF_8));
                            added++;
                        }
                    }
                }
            }
        }

        // 扫描 Mapper 接口
        Path mapperDir = Paths.get(projectRoot, SYSTEM_SRC, "mapper");
        if (Files.exists(mapperDir))
        {
            try (Stream<Path> files = Files.list(mapperDir))
            {
                for (Path javaFile : files.collect(Collectors.toList()))
                {
                    String fileName = javaFile.getFileName().toString();
                    if (fileName.endsWith("Mapper.java") && isRelatedToModule(fileName, moduleKey)
                        && !fileName.startsWith("Sys"))  // 排除系统级 Mapper
                    {
                        String className = fileName.replace(".java", "");
                        Path mdFile = backendDir.resolve(className + ".md");
                        if (!Files.exists(mdFile))
                        {
                            String srcRelPath = projectRoot.relativize(javaFile).toString().replace('\\', '/');
                            String skeleton = generateMapperSkeleton(className, srcRelPath, moduleName);
                            Files.write(mdFile, skeleton.getBytes(StandardCharsets.UTF_8));
                            added++;
                        }
                    }
                }
            }
        }

        // 扫描 Domain 实体
        Path domainDir = Paths.get(projectRoot, SYSTEM_SRC, "domain");
        if (Files.exists(domainDir))
        {
            try (Stream<Path> files = Files.list(domainDir))
            {
                for (Path javaFile : files.collect(Collectors.toList()))
                {
                    String fileName = javaFile.getFileName().toString();
                    if (fileName.endsWith(".java") && isRelatedToModule(fileName, moduleKey)
                        && !fileName.startsWith("Sys"))
                    {
                        String className = fileName.replace(".java", "");
                        Path mdFile = backendDir.resolve(className + ".md");
                        if (!Files.exists(mdFile))
                        {
                            String srcRelPath = projectRoot.relativize(javaFile).toString().replace('\\', '/');
                            String skeleton = generateEntitySkeleton(className, srcRelPath, moduleName);
                            Files.write(mdFile, skeleton.getBytes(StandardCharsets.UTF_8));
                            added++;
                        }
                    }
                }
            }
        }

        // 扫描 Mapper XML
        Path mapperXmlDir = Paths.get(projectRoot, MAPPER_XML_DIR);
        if (Files.exists(mapperXmlDir))
        {
            try (Stream<Path> files = Files.list(mapperXmlDir))
            {
                for (Path xmlFile : files.collect(Collectors.toList()))
                {
                    String fileName = xmlFile.getFileName().toString();
                    if (fileName.endsWith("Mapper.xml") && isRelatedToModule(fileName, moduleKey)
                        && !fileName.startsWith("Sys"))
                    {
                        String baseName = fileName.replace(".xml", "");
                        Path mdFile = backendDir.resolve(baseName + ".xml.md");
                        if (!Files.exists(mdFile))
                        {
                            String srcRelPath = projectRoot.relativize(xmlFile).toString().replace('\\', '/');
                            String skeleton = generateXmlSkeleton(baseName, srcRelPath, moduleName);
                            Files.write(mdFile, skeleton.getBytes(StandardCharsets.UTF_8));
                            added++;
                        }
                    }
                }
            }
        }

        return added;
    }

    /**
     * 扫描前端源码
     */
    private int scanFrontendSources(String moduleKey, String moduleName, Path moduleDocDir) throws IOException
    {
        int added = 0;
        Path frontendDir = moduleDocDir.resolve("前端");
        Files.createDirectories(frontendDir);

        // 扫描 views 目录
        Path viewsModuleDir = Paths.get(projectRoot, VIEWS_DIR, moduleKey);
        if (Files.exists(viewsModuleDir))
        {
            added += scanVueFiles(viewsModuleDir, frontendDir, projectRoot);
        }

        // 扫描 API 文件
        Path apiModuleFile = Paths.get(projectRoot, API_DIR, moduleKey, moduleKey + ".js");
        if (Files.exists(apiModuleFile))
        {
            Path mdFile = frontendDir.resolve(moduleKey + ".js.md");
            if (!Files.exists(mdFile))
            {
                String srcRelPath = projectRoot.relativize(apiModuleFile).toString().replace('\\', '/');
                String skeleton = "---\n" +
                    "source: " + srcRelPath + "\n" +
                    "type: ApiModule\n" +
                    "module: " + moduleName + "\n" +
                    "chapter: 前端\n" +
                    "autoGenerated: true\n" +
                    "createdAt: " + new Date().toString() + "\n" +
                    "---\n\n" +
                    "# " + moduleKey + ".js\n\n" +
                    "> 此文件由系统自动生成骨架，请描述 API 接口封装。\n\n" +
                    "## 接口列表\n\n" +
                    "<!-- TODO: 列出所有前端 API 方法及其对应的后端接口 -->\n\n" +
                    "## 请求/响应说明\n\n" +
                    "<!-- TODO: 描述请求参数格式、响应数据结构 -->\n\n";
                Files.write(mdFile, skeleton.getBytes(StandardCharsets.UTF_8));
                added++;
            }
        }

        // 扫描独立 API 文件（如 device/repair.js 等）
        Path apiDir = Paths.get(projectRoot, API_DIR, moduleKey);
        if (Files.exists(apiDir))
        {
            try (Stream<Path> files = Files.list(apiDir))
            {
                for (Path jsFile : files.collect(Collectors.toList()))
                {
                    String fileName = jsFile.getFileName().toString();
                    if (fileName.endsWith(".js") && !fileName.equals(moduleKey + ".js"))
                    {
                        Path mdFile = frontendDir.resolve(fileName + ".md");
                        if (!Files.exists(mdFile))
                        {
                            String srcRelPath = projectRoot.relativize(jsFile).toString().replace('\\', '/');
                            String skeleton = "---\n" +
                                "source: " + srcRelPath + "\n" +
                                "type: ApiModule\n" +
                                "module: " + moduleName + "\n" +
                                "chapter: 前端\n" +
                                "autoGenerated: true\n" +
                                "createdAt: " + new Date().toString() + "\n" +
                                "---\n\n" +
                                "# " + fileName + "\n\n" +
                                "> 此文件由系统自动生成骨架，请描述该 API 模块。\n\n";
                            Files.write(mdFile, skeleton.getBytes(StandardCharsets.UTF_8));
                            added++;
                        }
                    }
                }
            }
        }

        return added;
    }

    /**
     * 递归扫描 Vue 文件
     */
    private int scanVueFiles(Path dir, Path targetDir, Path projectRoot) throws IOException
    {
        int added = 0;
        try (Stream<Path> entries = Files.list(dir))
        {
            for (Path entry : entries.collect(Collectors.toList()))
            {
                if (Files.isDirectory(entry))
                {
                    Path subTarget = targetDir.resolve(entry.getFileName().toString());
                    Files.createDirectories(subTarget);
                    added += scanVueFiles(entry, subTarget, projectRoot);
                }
                else if (entry.getFileName().toString().endsWith(".vue"))
                {
                    String name = entry.getFileName().toString();
                    Path mdFile = targetDir.resolve(name + ".md");
                    if (!Files.exists(mdFile))
                    {
                        String srcRelPath = projectRoot.relativize(entry).toString().replace('\\', '/');
                        String skeleton = "---\n" +
                            "source: " + srcRelPath + "\n" +
                            "type: VuePage\n" +
                            "module: (parent module)\n" +
                            "chapter: 前端\n" +
                            "autoGenerated: true\n" +
                            "createdAt: " + new Date().toString() + "\n" +
                            "---\n\n" +
                            "# " + name + "\n\n" +
                            "> 此文件由系统自动生成骨架，请描述页面功能。\n\n" +
                            "## 页面功能\n\n" +
                            "<!-- TODO: 描述页面用途、核心交互 -->\n\n" +
                            "## 数据来源\n\n" +
                            "<!-- TODO: 列出页面调用的 API 接口 -->\n\n" +
                            "## 组件结构\n\n" +
                            "<!-- TODO: 描述页面组件树 -->\n\n";
                        Files.write(mdFile, skeleton.getBytes(StandardCharsets.UTF_8));
                        added++;
                    }
                }
            }
        }
        return added;
    }

    /**
     * 扫描数据库 SQL 文件
     */
    private int scanSqlSources(String moduleKey, String moduleName, Path moduleDocDir) throws IOException
    {
        int added = 0;
        Path sqlDir = moduleDocDir.resolve("数据库");
        Files.createDirectories(sqlDir);

        Path sqlRootDir = Paths.get(projectRoot, SQL_DIR);
        if (Files.exists(sqlRootDir))
        {
            try (Stream<Path> files = Files.list(sqlRootDir))
            {
                for (Path sqlFile : files.collect(Collectors.toList()))
                {
                    String fileName = sqlFile.getFileName().toString();
                    if (fileName.endsWith(".sql") && (fileName.contains(moduleKey) || fileName.contains("device")))
                    {
                        Path mdFile = sqlDir.resolve(fileName.replace(".sql", "") + ".md");
                        if (!Files.exists(mdFile))
                        {
                            String skeleton = "---\n" +
                                "source: " + projectRoot.relativize(sqlFile).toString().replace('\\', '/') + "\n" +
                                "type: DatabaseSQL\n" +
                                "module: " + moduleName + "\n" +
                                "chapter: 数据库\n" +
                                "autoGenerated: true\n" +
                                "createdAt: " + new Date().toString() + "\n" +
                                "---\n\n" +
                                "# " + fileName + "\n\n" +
                                "> 此文件由系统自动生成骨架，请描述涉及的数据库表。\n\n" +
                                "## 涉及的表\n\n" +
                                "<!-- TODO: 列出 SQL 文件创建/修改的数据库表 -->\n\n" +
                                "## 关键字段说明\n\n" +
                                "<!-- TODO: 描述关键字段的用途和约束 -->\n\n";
                            Files.write(mdFile, skeleton.getBytes(StandardCharsets.UTF_8));
                            added++;
                        }
                    }
                }
            }
        }

        return added;
    }

    /**
     * 判断文件名是否与模块相关（简单命名匹配）
     */
    private boolean isRelatedToModule(String fileName, String moduleKey)
    {
        String lower = fileName.toLowerCase();
        // 转换驼峰模块名：device → Device, meeting → Meeting
        String capitalized = moduleKey.substring(0, 1).toUpperCase() + moduleKey.substring(1);
        return lower.contains(moduleKey.toLowerCase()) || lower.contains(capitalized.toLowerCase());
    }

    /**
     * 标记过时文件：在 docs/ 中但对应源码已不存在的 .md 文件
     */
    private int markOrphanedFiles(Path docsRoot) throws IOException
    {
        int count = 0;
        try (Stream<Path> walk = Files.walk(docsRoot))
        {
            for (Path mdFile : walk.collect(Collectors.toList()))
            {
                if (!mdFile.toString().endsWith(".md")) continue;

                Map<String, String> fm = parseFrontmatter(mdFile);
                String sourcePath = fm.get("source");
                if (sourcePath == null || sourcePath.isEmpty()) continue;

                Path sourceFile = Paths.get(projectRoot, sourcePath);
                boolean sourceExists = Files.exists(sourceFile);
                boolean isOrphaned = "true".equals(fm.get("orphaned"));

                if (!sourceExists && !isOrphaned)
                {
                    // 源码已删除，标记过时
                    String content = new String(Files.readAllBytes(mdFile), StandardCharsets.UTF_8);
                    // 更新 frontmatter 添加 orphaned 标记
                    String updatedContent = addOrphanedFrontmatter(content);
                    Files.write(mdFile, updatedContent.getBytes(StandardCharsets.UTF_8));
                    count++;
                }
                else if (sourceExists && isOrphaned)
                {
                    // 源码恢复，移除孤儿标记
                    String content = new String(Files.readAllBytes(mdFile), StandardCharsets.UTF_8);
                    String updatedContent = removeOrphanedFrontmatter(content);
                    Files.write(mdFile, updatedContent.getBytes(StandardCharsets.UTF_8));
                }
            }
        }
        return count;
    }

    private String addOrphanedFrontmatter(String content)
    {
        // 在正文前添加 orphaned 标记
        if (content.startsWith("---"))
        {
            int endIdx = content.indexOf("---", 3);
            if (endIdx > 0)
            {
                String frontmatter = content.substring(3, endIdx).trim();
                String afterFm = content.substring(endIdx + 3);
                if (!frontmatter.contains("orphaned:"))
                {
                    frontmatter += "\norphaned: true";
                }
                String warning = "\n> ⚠ 关联源码已删除\n\n";
                return "---\n" + frontmatter + "\n---\n" + warning + afterFm.trim() + "\n";
            }
        }
        return content;
    }

    private String removeOrphanedFrontmatter(String content)
    {
        if (content.startsWith("---"))
        {
            int endIdx = content.indexOf("---", 3);
            if (endIdx > 0)
            {
                String frontmatter = content.substring(3, endIdx).trim();
                String afterFm = content.substring(endIdx + 3);
                // 移除 orphaned 行
                frontmatter = frontmatter.replaceAll("\norphaned: true", "").replace("orphaned: true\n", "");
                // 移除警告引用
                afterFm = afterFm.replaceAll("> ⚠ 关联源码已删除\n\n", "");
                return "---\n" + frontmatter.trim() + "\n---\n" + afterFm.trim() + "\n";
            }
        }
        return content;
    }

    /**
     * 生成 Controller 骨架
     */
    private String generateControllerSkeleton(String className, String srcRelPath, String moduleName, Path javaFile) throws IOException
    {
        // 解析 Java 文件提取方法签名
        String content = new String(Files.readAllBytes(javaFile), StandardCharsets.UTF_8);
        List<String[]> methods = extractControllerMethods(content);

        StringBuilder table = new StringBuilder();
        table.append("| 方法 | HTTP | 路径 | 权限 | 说明 |\n");
        table.append("|------|------|------|------|------|\n");
        for (String[] m : methods)
        {
            table.append(String.format("| `%s` | %s | `%s` | `%s` | |\n", m[0], m[1], m[2], m[3]));
        }

        return "---\n" +
            "source: " + srcRelPath + "\n" +
            "type: Controller\n" +
            "module: " + moduleName + "\n" +
            "chapter: 后端\n" +
            "autoGenerated: true\n" +
            "createdAt: " + new Date().toString() + "\n" +
            "---\n\n" +
            "# " + className + "\n\n" +
            "> 此文件由系统自动生成骨架，请填写实现说明。\n\n" +
            "## 接口列表\n\n" +
            table.toString() + "\n" +
            "## 核心逻辑\n\n" +
            "<!-- TODO: 描述关键业务逻辑、判断分支、异常处理 -->\n\n" +
            "## 依赖\n\n" +
            "<!-- TODO: 列出依赖的 Service、Mapper -->\n\n";
    }

    /**
     * 从 Java 控制器源码中提取 @GetMapping/@PostMapping 等注解的方法
     */
    private List<String[]> extractControllerMethods(String source)
    {
        List<String[]> methods = new ArrayList<>();
        // 匹配 @XxxMapping 注解 + 方法签名
        Pattern annotationPat = Pattern.compile("@(Get|Post|Put|Delete)Mapping\\(\"([^\"]*)\"\\)");
        Pattern preAuthPat = Pattern.compile("@PreAuthorize\\(\"@ss\\.hasPermi\\('([^']+)'\\)\"\\)");
        String[] lines = source.split("\n");

        for (int i = 0; i < lines.length; i++)
        {
            Matcher m = annotationPat.matcher(lines[i]);
            if (m.find())
            {
                String httpMethod = m.group(1).toUpperCase();
                String path = m.group(2);

                // 向前查找 @PreAuthorize
                String perm = "";
                for (int j = Math.max(0, i - 5); j < i; j++)
                {
                    Matcher pm = preAuthPat.matcher(lines[j]);
                    if (pm.find())
                    {
                        perm = pm.group(1);
                        break;
                    }
                }

                // 查找方法名
                String methodName = "";
                for (int j = i + 1; j < Math.min(lines.length, i + 3); j++)
                {
                    String line = lines[j].trim();
                    if (line.matches("public\\s+\\w+.*\\s+(\\w+)\\(.*"))
                    {
                        methodName = line.replaceAll(".*\\s+(\\w+)\\(.*", "$1");
                        break;
                    }
                }

                methods.add(new String[]{methodName, httpMethod, path, perm});
            }
        }
        return methods;
    }

    private String generateServiceSkeleton(String className, String srcRelPath, String moduleName)
    {
        return "---\n" +
            "source: " + srcRelPath + "\n" +
            "type: Service\n" +
            "module: " + moduleName + "\n" +
            "chapter: 后端\n" +
            "autoGenerated: true\n" +
            "createdAt: " + new Date().toString() + "\n" +
            "---\n\n" +
            "# " + className + "\n\n" +
            "> 此文件由系统自动生成骨架，请描述业务逻辑实现。\n\n" +
            "## 职责\n\n" +
            "<!-- TODO: 描述该 Service 的业务职责 -->\n\n" +
            "## 核心方法\n\n" +
            "<!-- TODO: 描述关键方法的实现逻辑、副作用、事务边界 -->\n\n" +
            "## 依赖\n\n" +
            "<!-- TODO: 列出依赖的 Mapper、其他 Service -->\n\n";
    }

    private String generateMapperSkeleton(String className, String srcRelPath, String moduleName)
    {
        return "---\n" +
            "source: " + srcRelPath + "\n" +
            "type: Mapper\n" +
            "module: " + moduleName + "\n" +
            "chapter: 后端\n" +
            "autoGenerated: true\n" +
            "createdAt: " + new Date().toString() + "\n" +
            "---\n\n" +
            "# " + className + "\n\n" +
            "> 此文件由系统自动生成骨架，请描述数据访问逻辑。\n\n" +
            "## SQL 说明\n\n" +
            "<!-- TODO: 描述关键 SQL 的查询逻辑、索引使用、性能考虑 -->\n\n" +
            "## 映射关系\n\n" +
            "<!-- TODO: 描述实体与数据库表的映射 -->\n\n";
    }

    private String generateEntitySkeleton(String className, String srcRelPath, String moduleName)
    {
        return "---\n" +
            "source: " + srcRelPath + "\n" +
            "type: Entity\n" +
            "module: " + moduleName + "\n" +
            "chapter: 后端\n" +
            "autoGenerated: true\n" +
            "createdAt: " + new Date().toString() + "\n" +
            "---\n\n" +
            "# " + className + "\n\n" +
            "> 此文件由系统自动生成骨架，请描述实体和数据库表。\n\n" +
            "## 对应数据库表\n\n" +
            "<!-- TODO: 数据库表名 -->\n\n" +
            "## 字段说明\n\n" +
            "<!-- TODO: 关键字段的用途和约束 -->\n\n";
    }

    private String generateXmlSkeleton(String baseName, String srcRelPath, String moduleName)
    {
        return "---\n" +
            "source: " + srcRelPath + "\n" +
            "type: MapperXml\n" +
            "module: " + moduleName + "\n" +
            "chapter: 后端\n" +
            "autoGenerated: true\n" +
            "createdAt: " + new Date().toString() + "\n" +
            "---\n\n" +
            "# " + baseName + ".xml\n\n" +
            "> 此文件由系统自动生成骨架，请描述 MyBatis XML 中的 SQL 语句。\n\n" +
            "## SQL 语句清单\n\n" +
            "<!-- TODO: 列出 XML 中定义的 SQL 语句及其用途 -->\n\n";
    }

    @Override
    public List<Map<String, Object>> search(String keyword)
    {
        List<Map<String, Object>> results = new ArrayList<>();
        if (StringUtils.isEmpty(keyword)) return results;

        Path docsRoot = Paths.get(projectRoot, DOCS_DIR);
        if (!Files.exists(docsRoot)) return results;

        try (Stream<Path> walk = Files.walk(docsRoot))
        {
            for (Path mdFile : walk.collect(Collectors.toList()))
            {
                if (!mdFile.toString().endsWith(".md")) continue;
                try
                {
                    String content = new String(Files.readAllBytes(mdFile), StandardCharsets.UTF_8);
                    if (content.toLowerCase().contains(keyword.toLowerCase()))
                    {
                        Map<String, Object> hit = new LinkedHashMap<>();
                        hit.put("path", projectRoot.relativize(mdFile).toString().replace('\\', '/'));
                        hit.put("name", mdFile.getFileName().toString().replace(".md", ""));
                        // 提取关键词周围的片段
                        int idx = content.toLowerCase().indexOf(keyword.toLowerCase());
                        int start = Math.max(0, idx - 50);
                        int end = Math.min(content.length(), idx + keyword.length() + 100);
                        hit.put("snippet", content.substring(start, end).replace("\n", " "));
                        results.add(hit);
                    }
                }
                catch (IOException ignored) {}
            }
        }
        catch (IOException e)
        {
            log.error("搜索文档失败", e);
        }

        return results;
    }

    /**
     * 获取当前 Git HEAD commit hash
     */
    private String getCurrentCommitHash()
    {
        try
        {
            Path gitDir = Paths.get(projectRoot, ".git");
            Path headFile = gitDir.resolve("HEAD");
            if (Files.exists(headFile))
            {
                String head = new String(Files.readAllBytes(headFile), StandardCharsets.UTF_8).trim();
                if (head.startsWith("ref: "))
                {
                    String ref = head.substring(5);
                    Path refFile = gitDir.resolve(ref);
                    if (Files.exists(refFile))
                    {
                        return new String(Files.readAllBytes(refFile), StandardCharsets.UTF_8).trim();
                    }
                }
                else
                {
                    return head;
                }
            }
        }
        catch (IOException ignored) {}
        return "";
    }
}
```

- [ ] **Step 3: 编译验证**

```bash
cd ruoyi-system && mvn compile -q
```

---

### Task 5: DocController 接口

**Files:**
- Create: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/doc/DocController.java`

**Interfaces:**
- Consumes: `IDocService.getTree()`, `IDocService.getContent(String)`, `IDocService.getStatus()`, `IDocService.syncAll()`, `IDocService.search(String)`

- [ ] **Step 1: 编写 Controller**

```java
package com.ruoyi.web.controller.doc;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.IDocService;

/**
 * 文档中心
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/doc")
public class DocController extends BaseController
{
    @Autowired
    private IDocService docService;

    /**
     * 获取文档目录树
     */
    @GetMapping("/tree")
    public AjaxResult tree()
    {
        List<Map<String, Object>> tree = docService.getTree();
        return success(tree);
    }

    /**
     * 获取文档内容
     */
    @GetMapping("/content")
    public AjaxResult content(@RequestParam("path") String path)
    {
        Map<String, Object> content = docService.getContent(path);
        if (content.containsKey("error"))
        {
            return error((String) content.get("error"));
        }
        return success(content);
    }

    /**
     * 获取同步状态
     */
    @GetMapping("/status")
    public AjaxResult status()
    {
        return success(docService.getStatus());
    }

    /**
     * 手动触发同步
     */
    @PreAuthorize("@ss.hasPermi('doc:sync')")
    @PostMapping("/sync")
    public AjaxResult sync()
    {
        return success(docService.syncAll());
    }

    /**
     * 搜索文档
     */
    @GetMapping("/search")
    public AjaxResult search(@RequestParam("keyword") String keyword)
    {
        return success(docService.search(keyword));
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
cd ruoyi-admin && mvn compile -q
```

---

### Task 6: DocSyncTask Quartz 定时任务

**Files:**
- Create: `ruoyi-quartz/src/main/java/com/ruoyi/quartz/task/DocSyncTask.java`

**Interfaces:**
- Consumes: `IDocService.syncAll()`
- Produces: `DocSyncTask.checkAndSync()` — entry point for Quartz scheduler

- [ ] **Step 1: 编写定时任务**

```java
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

    /** 上次同步时的 commit hash */
    private String lastCommitHash = "";

    /**
     * 任务入口 —— 由 Quartz 调度器调用
     */
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
```

- [ ] **Step 2: 编译验证**

```bash
cd ruoyi-quartz && mvn compile -q
```

---

### Task 7: 添加 JGit 依赖

**Files:**
- Modify: `ruoyi-system/pom.xml`

- [ ] **Step 1: 在 `<dependencies>` 中添加 JGit 依赖**

在 `ruoyi-system/pom.xml` 的 `<dependencies>` 标签内，`ruoyi-common` 依赖后面添加：

```xml
        <!-- JGit (Git操作，文档中心同步使用) -->
        <dependency>
            <groupId>org.eclipse.jgit</groupId>
            <artifactId>org.eclipse.jgit</artifactId>
            <version>6.10.0.202406032230-r</version>
        </dependency>
```

注意：DocServiceImpl 当前使用直接读取 `.git/HEAD` 文件的方式获取 commit hash（无需 JGit 即可工作）。JGit 依赖预留用于后续扩展更复杂的 Git 操作。当前方案无需等待 JGit 依赖即可运行。

- [ ] **Step 2: 编译验证**

```bash
cd ruoyi-system && mvn compile -q
```

---

### Task 8: SecurityConfig 匿名访问白名单

**Files:**
- Modify: `ruoyi-framework/src/main/java/com/ruoyi/framework/config/SecurityConfig.java`

- [ ] **Step 1: 在 `filterChain` 方法的 `permitAll` 链中添加文档接口**

找到 `.requestMatchers("/login", "/register", ...` 这一行，在末尾 `.permitAll()` 之前追加 `/doc/tree`, `/doc/content`, `/doc/status`, `/doc/search`：

```java
// 在现有的 permitAll 链中添加文档中心匿名接口
// 找到类似下面的行：
requests.requestMatchers("/login", "/register", "/captchaImage", ..., "/meeting/room/simpleList").permitAll()

// 改为（在 "/meeting/room/simpleList" 之后追加）：
requests.requestMatchers("/login", "/register", "/captchaImage", "/pass/**", "/visitor/h5/**",
    "/h5-register", "/h5-register.html", "/device/repair/public/**", "/device/alert/webhook",
    "/repair-complete", "/attendance/callback/**", "/ws/**", "/meeting/board/**",
    "/meeting/room/simpleList",
    "/doc/tree", "/doc/content", "/doc/status", "/doc/search").permitAll()
```

**Exact edit**: 在 `"/meeting/room/simpleList"` 之后、`).permitAll()` 之前，插入：
```
, "/doc/tree", "/doc/content", "/doc/status", "/doc/search"
```

---

## Phase 2: 前端（P0）

### Task 9: 安装 marked + 创建 API 模块

**Files:**
- Create: `ruoyi-ui/src/api/doc/doc.js`
- Run: `npm install marked --save` (in ruoyi-ui)

- [ ] **Step 1: 安装 marked**

```bash
cd ruoyi-ui && npm install marked@4.3.0 --save
```

注意：`highlight.js` 已存在（`9.18.5`），无需安装。

- [ ] **Step 2: 编写 API 封装**

```javascript
import request from '@/utils/request'

// 获取文档目录树
export function getDocTree() {
  return request({
    url: '/doc/tree',
    method: 'get'
  })
}

// 获取文档内容
export function getDocContent(path) {
  return request({
    url: '/doc/content',
    method: 'get',
    params: { path }
  })
}

// 获取同步状态
export function getDocStatus() {
  return request({
    url: '/doc/status',
    method: 'get'
  })
}

// 手动触发同步
export function syncDoc() {
  return request({
    url: '/doc/sync',
    method: 'post'
  })
}

// 搜索文档
export function searchDoc(keyword) {
  return request({
    url: '/doc/search',
    method: 'get',
    params: { keyword }
  })
}
```

---

### Task 10: DocTree 组件

**Files:**
- Create: `ruoyi-ui/src/views/doc/components/DocTree.vue`
- Create: `ruoyi-ui/src/views/doc/styles/atlas.scss`

**Interfaces:**
- Consumes: `getDocTree()` from `@/api/doc/doc`
- Produces: emits `select(file)` when user clicks a document node

- [ ] **Step 1: 编写 atlas.scss 设计体系样式**

```scss
// Code Atlas 设计体系
$atlas-bg: #FBFBF9;
$atlas-sidebar: #1E232B;
$atlas-sidebar-text: #BCC0C6;
$atlas-sidebar-active: #E8EDF2;
$atlas-accent: #5B7F6B;
$atlas-accent-soft: rgba(91, 127, 107, 0.10);
$atlas-text: #1C1C1C;
$atlas-muted: #7A7A72;
$atlas-rule: #E6E4E0;
$atlas-code-bg: #F3F1ED;
$atlas-code-block: #F6F5F3;
$atlas-warning: #D4A03A;
$atlas-statusbar: #F2F1EF;

.doc-center {
  --atlas-bg: #{$atlas-bg};
  --atlas-sidebar: #{$atlas-sidebar};
  --atlas-sidebar-text: #{$atlas-sidebar-text};
  --atlas-sidebar-active: #{$atlas-sidebar-active};
  --atlas-accent: #{$atlas-accent};
  --atlas-accent-soft: #{$atlas-accent-soft};
  --atlas-text: #{$atlas-text};
  --atlas-muted: #{$atlas-muted};
  --atlas-rule: #{$atlas-rule};
  --atlas-code-bg: #{$atlas-code-bg};
  --atlas-code-block: #{$atlas-code-block};
  --atlas-warning: #{$atlas-warning};
  --atlas-statusbar: #{$atlas-statusbar};

  height: calc(100vh - 84px);
  display: flex;
  flex-direction: column;
  background: var(--atlas-bg);
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;

  &__body {
    display: flex;
    flex: 1;
    overflow: hidden;
  }

  &__sidebar {
    width: 320px;
    min-width: 240px;
    background: var(--atlas-sidebar);
    color: var(--atlas-sidebar-text);
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  &__content {
    flex: 1;
    overflow-y: auto;
    padding: 48px 64px;
  }

  &__statusbar {
    height: 36px;
    line-height: 36px;
    background: var(--atlas-statusbar);
    border-top: 1px solid var(--atlas-rule);
    display: flex;
    align-items: center;
    padding: 0 24px;
    font-size: 12px;
    color: var(--atlas-muted);
    gap: 24px;
  }
}

.doc-tree {
  padding: 16px 0;
  overflow-y: auto;
  flex: 1;

  &__search {
    padding: 0 16px 12px;
    .el-input__inner {
      background: rgba(255,255,255,0.08);
      border: 1px solid rgba(255,255,255,0.10);
      color: var(--atlas-sidebar-text);
      border-radius: 4px;
      font-size: 13px;
      &::placeholder { color: rgba(188, 192, 198, 0.4); }
      &:focus { border-color: var(--atlas-accent); }
    }
  }

  &__node {
    position: relative;
    padding: 6px 16px 6px 28px;
    font-size: 13px;
    line-height: 1.4;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    transition: all 150ms ease;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    &:hover {
      background: rgba(255,255,255,0.06);
      color: #FFF;
    }

    &--active {
      color: var(--atlas-sidebar-active);
      background: var(--atlas-accent-soft);
      border-left: 3px solid var(--atlas-accent);
      padding-left: 25px;
    }

    &--skeleton {
      font-style: italic;
      opacity: 0.5;
      &:hover { opacity: 0.7; }
    }

    &--orphaned {
      .doc-tree__warning { display: inline; }
    }

    &--child { padding-left: 48px; }
    &--grandchild { padding-left: 68px; }
  }

  &__icon {
    font-size: 14px;
    width: 18px;
    flex-shrink: 0;
    color: rgba(188, 192, 198, 0.6);
  }

  &__label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__badge {
    font-size: 11px;
    color: rgba(188, 192, 198, 0.4);
    flex-shrink: 0;
    margin-left: auto;
  }

  &__warning {
    color: var(--atlas-warning);
    display: none;
    margin-right: 4px;
  }

  &__branch {
    position: relative;
    &::before {
      content: '';
      position: absolute;
      left: 20px;
      top: 24px;
      bottom: 0;
      width: 2px;
      background: linear-gradient(to bottom, var(--atlas-accent), transparent 60%);
      opacity: 0.4;
    }
  }
}

.doc-reader {
  max-width: 780px;
  margin: 0 auto;

  h1 { font-size: 28px; line-height: 1.3; color: var(--atlas-text); margin: 0 0 24px; font-weight: 700; }
  h2 { font-size: 20px; line-height: 1.4; color: var(--atlas-text); margin: 40px 0 16px; padding-bottom: 8px; border-bottom: 1px solid var(--atlas-rule); }
  h3 { font-size: 16px; line-height: 1.5; color: var(--atlas-text); margin: 32px 0 12px; }
  p { font-size: 15px; line-height: 1.75; color: var(--atlas-text); margin: 0 0 16px; }
  a { color: var(--atlas-accent); text-decoration: none; &:hover { text-decoration: underline; } }
  blockquote { border-left: 3px solid var(--atlas-accent); padding: 4px 16px; margin: 0 0 16px; color: var(--atlas-muted); background: rgba(91,127,107,0.04); }
  table { border-collapse: collapse; width: 100%; margin: 0 0 16px; font-size: 14px;
    th, td { border: 1px solid var(--atlas-rule); padding: 8px 12px; text-align: left; }
    th { background: var(--atlas-code-bg); font-weight: 600; }
  }
  ul, ol { padding-left: 24px; margin: 0 0 16px; li { line-height: 1.75; } }

  code {
    background: var(--atlas-code-bg);
    padding: 2px 6px;
    border-radius: 3px;
    font-family: "JetBrains Mono", "Cascadia Code", "Fira Code", monospace;
    font-size: 13px;
  }

  pre {
    background: var(--atlas-code-block);
    border: 1px solid var(--atlas-rule);
    border-radius: 6px;
    padding: 16px 20px;
    overflow-x: auto;
    margin: 0 0 20px;
    transition: transform 150ms ease, box-shadow 150ms ease;
    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 2px 8px rgba(0,0,0,0.06);
    }
    code {
      background: transparent;
      padding: 0;
      font-size: 13.5px;
      line-height: 1.6;
    }
  }

  img { max-width: 100%; border-radius: 4px; }

  .doc-reader__meta {
    margin-top: 48px;
    padding-top: 16px;
    border-top: 1px solid var(--atlas-rule);
    display: flex;
    gap: 24px;
    font-size: 13px;
    color: var(--atlas-muted);
    flex-wrap: wrap;

    &-item {
      display: flex;
      align-items: center;
      gap: 4px;
    }
  }

  .doc-reader__empty {
    text-align: center;
    padding: 80px 40px;
    color: var(--atlas-muted);
    &-icon { font-size: 48px; margin-bottom: 16px; }
    &-title { font-size: 18px; margin-bottom: 8px; color: var(--atlas-text); }
    &-desc { font-size: 14px; }
    &-source { margin-top: 12px; font-size: 13px; }
  }

  .doc-reader__warning {
    background: rgba(212, 160, 58, 0.08);
    border: 1px solid rgba(212, 160, 58, 0.3);
    border-radius: 4px;
    padding: 8px 16px;
    margin-bottom: 24px;
    font-size: 13px;
    color: var(--atlas-warning);
  }
}
```

- [ ] **Step 2: 编写 DocTree.vue**

```vue
<template>
  <div class="doc-tree">
    <div class="doc-tree__search">
      <el-input v-model="searchText" placeholder="搜索文档..." size="small" clearable prefix-icon="el-icon-search" />
    </div>
    <template v-for="module in filteredTree">
      <div :key="module.name" class="doc-tree__branch">
        <div class="doc-tree__node"
          @click="toggleModule(module.name)"
        >
          <span class="doc-tree__icon">{{ openModules[module.name] ? '▼' : '▶' }}</span>
          <span class="doc-tree__label">{{ module.name }}</span>
          <span class="doc-tree__badge" v-if="module.totalFiles">
            {{ module.filledFiles }}/{{ module.totalFiles }}
          </span>
        </div>
        <template v-if="openModules[module.name]">
          <div v-for="chapter in module.children" :key="chapter.path" class="doc-tree__branch">
            <div class="doc-tree__node doc-tree__node--child"
              @click="toggleModule(chapter.path)"
            >
              <span class="doc-tree__icon">{{ openModules[chapter.path] ? '▼' : '▶' }}</span>
              <span class="doc-tree__label">{{ chapter.name }}</span>
            </div>
            <template v-if="openModules[chapter.path]">
              <div v-for="file in flattenFiles(chapter)" :key="file.path"
                class="doc-tree__node"
                :class="[
                  file.indentClass,
                  { 'doc-tree__node--active': selectedPath === file.path,
                    'doc-tree__node--skeleton': file.meta && file.meta.autoGenerated,
                    'doc-tree__node--orphaned': file.meta && file.meta.orphaned }
                ]"
                @click="selectFile(file)"
              >
                <span class="doc-tree__warning" v-if="file.meta && file.meta.orphaned">⚠</span>
                <span class="doc-tree__icon">📄</span>
                <span class="doc-tree__label">{{ file.name }}</span>
                <el-tooltip v-if="file.meta && file.meta.autoGenerated" content="尚未编写" placement="right">
                  <span style="font-size:11px;opacity:0.4;">·</span>
                </el-tooltip>
              </div>
            </template>
          </div>
        </template>
      </div>
    </template>
    <div v-if="filteredTree.length === 0" style="padding:40px 16px;text-align:center;color:rgba(188,192,198,0.4);font-size:13px;">
      暂无文档模块，请执行同步
    </div>
  </div>
</template>

<script>
export default {
  name: 'DocTree',
  props: {
    tree: { type: Array, default: () => [] }
  },
  data() {
    return {
      searchText: '',
      openModules: {},
      selectedPath: ''
    }
  },
  computed: {
    filteredTree() {
      if (!this.searchText) return this.tree;
      const keyword = this.searchText.toLowerCase();
      return this.tree.filter(mod => {
        return this.moduleMatches(mod, keyword);
      });
    }
  },
  methods: {
    moduleMatches(node, keyword) {
      if (node.name.toLowerCase().includes(keyword)) return true;
      if (node.children) {
        return node.children.some(child => this.moduleMatches(child, keyword));
      }
      return false;
    },
    toggleModule(name) {
      this.$set(this.openModules, name, !this.openModules[name]);
    },
    flattenFiles(chapter) {
      // 递归展开所有文件节点
      const result = [];
      const walk = (node, depth) => {
        if (node.type === 'file') {
          result.push({
            ...node,
            indentClass: depth === 1 ? 'doc-tree__node--child' : 'doc-tree__node--grandchild'
          });
        } else if (node.children) {
          for (const child of node.children) {
            walk(child, depth + 1);
          }
        }
      };
      walk(chapter, 1);
      return result;
    },
    selectFile(file) {
      this.selectedPath = file.path;
      this.$emit('select', file);
    }
  }
}
</script>
```

---

### Task 11: DocReader 组件

**Files:**
- Create: `ruoyi-ui/src/views/doc/components/DocReader.vue`

**Interfaces:**
- Consumes: `getDocContent(path)` from `@/api/doc/doc`
- Props: `filePath` (String) — path of selected document

- [ ] **Step 1: 编写 DocReader.vue**

```vue
<template>
  <div class="doc-reader">
    <div v-if="error" class="doc-reader__empty">
      <div class="doc-reader__empty-icon">❌</div>
      <div class="doc-reader__empty-title">加载失败</div>
      <div class="doc-reader__empty-desc">{{ error }}</div>
    </div>

    <div v-else-if="loading" class="doc-reader__empty">
      <div class="doc-reader__empty-icon">⏳</div>
      <div class="doc-reader__empty-title">加载中...</div>
    </div>

    <div v-else-if="!filePath" class="doc-reader__empty">
      <div class="doc-reader__empty-icon">📚</div>
      <div class="doc-reader__empty-title">文档中心</div>
      <div class="doc-reader__empty-desc">从左侧目录树选择文档浏览</div>
    </div>

    <div v-else-if="orphaned" class="doc-reader__warning">
      ⚠ 该文档关联的源码文件已被删除，文档内容可能已过时。
    </div>

    <div v-else-if="content">
      <div v-if="autoGenerated" style="margin-bottom:16px;padding:12px 16px;background:rgba(91,127,107,0.06);border-radius:4px;font-size:13px;color:var(--atlas-muted);">
        📝 此文档由系统自动生成骨架，等待开发者填充实现说明。
      </div>
      <div v-html="renderedHtml"></div>

      <div class="doc-reader__meta">
        <div class="doc-reader__meta-item" v-if="frontmatter.source">
          📎 {{ frontmatter.source }}
        </div>
        <div class="doc-reader__meta-item" v-if="frontmatter.type">
          🏷 {{ frontmatter.type }}
        </div>
        <div class="doc-reader__meta-item" v-if="frontmatter.updatedAt">
          🕐 {{ frontmatter.updatedAt }}
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { getDocContent } from '@/api/doc/doc'
import { marked } from 'marked'
import hljs from 'highlight.js'

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(lang, code).value
      } catch (e) {}
    }
    try {
      return hljs.highlightAuto(code).value
    } catch (e) {}
    return code
  },
  breaks: true,
  gfm: true
})

export default {
  name: 'DocReader',
  props: {
    filePath: { type: String, default: '' }
  },
  data() {
    return {
      loading: false,
      error: '',
      content: '',
      renderedHtml: '',
      frontmatter: {},
      autoGenerated: false,
      orphaned: false
    }
  },
  watch: {
    filePath: {
      immediate: true,
      handler(val) {
        if (val) this.loadContent(val)
      }
    }
  },
  methods: {
    async loadContent(path) {
      this.loading = true
      this.error = ''
      this.content = ''
      this.renderedHtml = ''
      try {
        const res = await getDocContent(path)
        if (res.data && res.data.markdown !== undefined) {
          this.content = res.data.markdown
          this.frontmatter = res.data.frontmatter || {}
          this.autoGenerated = res.data.autoGenerated || false
          this.orphaned = res.data.orphaned || false
          this.renderedHtml = marked.parse(res.data.markdown || '')
        } else if (res.msg) {
          this.error = res.msg
        }
      } catch (e) {
        this.error = '文档加载失败: ' + (e.message || '未知错误')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>
```

---

### Task 12: 文档中心主页面 index.vue

**Files:**
- Create: `ruoyi-ui/src/views/doc/index.vue`

- [ ] **Step 1: 编写 index.vue**

```vue
<template>
  <div class="doc-center">
    <!-- 顶栏 -->
    <div style="height:48px;line-height:48px;display:flex;align-items:center;padding:0 24px;border-bottom:1px solid var(--atlas-rule);background:#FFF;gap:16px;">
      <a href="/" style="color:var(--atlas-muted);text-decoration:none;font-size:14px;">← 返回系统</a>
      <span style="font-size:16px;font-weight:600;color:var(--atlas-text);">文档中心</span>
    </div>

    <!-- 主内容区 -->
    <div class="doc-center__body">
      <!-- 左侧目录树 -->
      <div class="doc-center__sidebar">
        <DocTree :tree="tree" @select="onSelectFile" />
      </div>

      <!-- 右侧阅读区 -->
      <div class="doc-center__content" ref="contentArea">
        <DocReader :filePath="selectedFilePath" />
      </div>
    </div>

    <!-- 状态栏 -->
    <div class="doc-center__statusbar">
      <span v-if="status.totalModules">📁 {{ status.totalModules }} 个模块</span>
      <span v-if="status.totalFiles">📄 {{ status.totalFiles }} 个文档</span>
      <span v-if="status.filledFiles !== undefined && status.autoGenFiles !== undefined">
        📝 {{ status.autoGenFiles }} 个待填充
      </span>
      <span v-if="status.lastSyncTime">🕐 上次同步: {{ status.lastSyncTime }}</span>
      <span style="margin-left:auto;cursor:pointer;color:var(--atlas-accent);" @click="doSync">
        {{ syncing ? '同步中...' : '立即同步' }}
      </span>
    </div>
  </div>
</template>

<script>
import DocTree from './components/DocTree'
import DocReader from './components/DocReader'
import { getDocTree, getDocStatus, syncDoc } from '@/api/doc/doc'

export default {
  name: 'DocCenter',
  components: { DocTree, DocReader },
  data() {
    return {
      tree: [],
      status: {},
      selectedFilePath: '',
      syncing: false
    }
  },
  created() {
    this.loadTree()
    this.loadStatus()
  },
  methods: {
    async loadTree() {
      try {
        const res = await getDocTree()
        if (res.data) {
          this.tree = Array.isArray(res.data) ? res.data : []
        }
      } catch (e) {
        console.error('加载文档树失败', e)
      }
    },
    async loadStatus() {
      try {
        const res = await getDocStatus()
        if (res.data) {
          this.status = res.data
        }
      } catch (e) {
        console.error('加载同步状态失败', e)
      }
    },
    onSelectFile(file) {
      this.selectedFilePath = file.path
    },
    async doSync() {
      this.syncing = true
      try {
        await syncDoc()
        this.$message.success('同步完成')
        await Promise.all([this.loadTree(), this.loadStatus()])
      } catch (e) {
        this.$message.error('同步失败: ' + (e.message || '未知错误'))
      } finally {
        this.syncing = false
      }
    }
  }
}
</script>

<style scoped src="./styles/atlas.scss"></style>
```

---

### Task 13: 路由 + permission.js 白名单

**Files:**
- Modify: `ruoyi-ui/src/router/index.js`
- Modify: `ruoyi-ui/src/permission.js`

- [ ] **Step 1: 在 `staticRoutes` 数组中新增文档中心路由**

在 `ruoyi-ui/src/router/index.js` 的 `staticRoutes` 数组末尾（`];` 之前），添加：

```javascript
  // ==================== 文档中心 ====================
  {
    path: "/doc",
    component: Layout,
    name: "DocCenter",
    alwaysShow: true,
    meta: { title: "文档中心", icon: "documentation", parentMenuId: 2150, noAuth: true },
    children: [
      {
        path: "",
        component: () => import("@/views/doc/index"),
        name: "DocView",
        meta: { title: "文档中心", noAuth: true }
      }
    ],
  },
```

- [ ] **Step 2: 在 permission.js 的 whiteList 中添加 `/doc`**

找到 `permission.js` 中的 `whiteList` 行：

```javascript
const whiteList = ['/login', '/h5/login', '/h5-register', '/pass', '/repair-complete']
```

改为：

```javascript
const whiteList = ['/login', '/h5/login', '/h5-register', '/pass', '/repair-complete', '/doc']
```

- [ ] **Step 3: 验证前端编译**

```bash
cd ruoyi-ui && npm run build:prod
```

---

### Task 14: 全链路验证

- [ ] **Step 1: 执行 SQL 初始化**

```bash
# 在 MySQL 客户端执行
mysql -u root -p ruoyi < sql/doc_center.sql
```

- [ ] **Step 2: 手动触发首次同步**

```bash
curl -X POST http://localhost:8080/doc/sync -H "Authorization: Bearer <token>"
```

- [ ] **Step 3: 验证文档树接口**

```bash
curl http://localhost:8080/doc/tree
# 预期返回 JSON 树结构，包含所有模块的章节
```

- [ ] **Step 4: 验证文档内容接口**

```bash
curl "http://localhost:8080/doc/content?path=01-设备管理/README.md"
# 预期返回 frontmatter + markdown 内容
```

- [ ] **Step 5: 访问页面**

打开浏览器访问 `http://localhost/doc`，确认：
- 未登录也能访问（白名单生效）
- 左侧显示模块树、可展开折叠
- 右侧渲染 Markdown 内容
- 骨架文件显示灰色斜体提示
- 状态栏显示统计信息

- [ ] **Step 6: 提交代码**

```bash
git add .
git commit -m "feat(doc): 文档中心模块完整实现

- 后端: DocSyncService 扫描引擎 + DocController 5个接口
- 前端: DocTree + DocReader 双栏布局 + Code Atlas 设计体系
- Quartz: DocSyncTask 每分钟检测Git变化自动同步
- 安全: 匿名访问白名单 (SecurityConfig + permission.js)
- 数据库: sys_doc_sync_log 表 + 菜单 + 定时任务

Co-Authored-By: Claude <noreply@anthropic.com>"
```

---

## 自检清单

### Spec Coverage

| 规格要求 | 对应 Task |
|----------|-----------|
| 约定式映射 (源码→docs) | Task 4 (scanBackendSources/scanFrontendSources/scanSqlSources) |
| 模块识别 (controller 子目录) | Task 4 (MODULE_NAME_MAP + syncAll 扫描逻辑) |
| .md 骨架模板 (frontmatter + 结构化) | Task 4 (generateControllerSkeleton/generateServiceSkeleton 等) |
| 同步行为矩阵 (新增/删除/移动/修改/人工编辑) | Task 4 (markOrphanedFiles/addOrphanedFrontmatter) |
| Quartz 持续同步 | Task 6 (DocSyncTask.checkAndSync) |
| 后端接口 (tree/content/status/sync/search) | Task 5 (DocController) |
| 数据库表 sys_doc_sync_log | Task 1 (建表 SQL) + Task 2 (实体) + Task 3 (Mapper) |
| 前端 DocTree 组件 | Task 10 |
| 前端 DocReader 组件 | Task 11 |
| index.vue 主页面 + 状态栏 | Task 12 |
| 匿名访问白名单 (SecurityConfig) | Task 8 |
| 前端路由守卫 (permission.js whiteList) | Task 13 |
| 菜单 SQL (sys_menu) | Task 1 |
| Code Atlas 设计体系 (色彩/字体/书脊线) | Task 10 (atlas.scss) |
| 暗色模式预留 | Task 10 (CSS 变量 + doc-center class) |
| JGit 依赖 | Task 7 |

### Placeholder Scan

✅ 无 "TBD", "TODO", "implement later", "fill in details"
✅ 所有步骤含具体代码或命令
✅ 所有接口方法签名已在 Consumes/Produces 声明

### Type Consistency

✅ `IDocService.getTree()` → `List<Map<String,Object>>` → `DocController.tree()` → `AjaxResult` → 前端 `getDocTree()` → `res.data` 为数组
✅ `IDocService.getContent(String)` → `Map<String,Object>` → `DocController.content()` → 前端 `getDocContent(path)` → `res.data.markdown`
✅ `DocSyncTask.checkAndSync()` → Quartz `sys_job.invoke_target` 值为 `docSyncTask.checkAndSync()`
✅ `menu_type='M'` 目录 + `menu_type='C'` 菜单 + `menu_type='F'` 按钮，符合若依菜单体系
