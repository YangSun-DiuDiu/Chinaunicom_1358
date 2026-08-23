package com.ruoyi.framework.interceptor;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import org.springframework.stereotype.Component;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * MyBatis 拦截器：自动为业务表 SELECT 查询添加组织(dept_id)过滤条件
 *
 * 过滤规则：
 * 1. 按主表判定白名单：仅系统表/全局共享表不添加过滤，业务表即使 JOIN 系统表也会被过滤
 * 2. 管理员跳过过滤
 * 3. 非管理员强制追加"本部门 + 子部门"过滤，不可通过查询参数绕过
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare",
               args = {Connection.class, Integer.class})
})
@Component
public class DeptScopeInterceptor implements Interceptor {

    private static final Logger log = LoggerFactory.getLogger(DeptScopeInterceptor.class);

    /** 白名单：不需要组织过滤的表 */
    private static final Set<String> WHITELIST_TABLES = new HashSet<>(Arrays.asList(
        "sys_user", "sys_dept", "sys_role", "sys_menu", "sys_post",
        "sys_user_role", "sys_role_menu", "sys_role_dept", "sys_user_post",
        "sys_dict_type", "sys_dict_data",
        "sys_config", "sys_notice", "sys_notice_read",
        "sys_oper_log", "sys_logininfor",
        "sys_job", "sys_job_log", "sys_sms_config"
    ));

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        MetaObject metaObject = MetaObject.forObject(statementHandler,
            SystemMetaObject.DEFAULT_OBJECT_FACTORY,
            SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
            new DefaultReflectorFactory());

        MappedStatement mappedStatement = (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        String sqlCommandType = mappedStatement.getSqlCommandType().name();

        // 只处理 SELECT 语句
        if (!"SELECT".equalsIgnoreCase(sqlCommandType)) {
            return invocation.proceed();
        }

        String originalSql = statementHandler.getBoundSql().getSql().toLowerCase();

        // 白名单判定改为"主表"判定：仅当 FROM 后主表为系统/全局共享表时才跳过组织过滤。
        // 原实现按"SQL 文本是否包含任一白名单表"判定，导致业务表(如 iot_device) JOIN sys_dept
        // 取部门名时被误判为系统表查询而绕过 dept_id 过滤，造成非管理员越权查看全部数据。
        String mainTable = extractMainTable(originalSql);
        if (WHITELIST_TABLES.contains(mainTable)) {
            return invocation.proceed();
        }

        try {
            Long deptId = getCurrentDeptId();
            if (deptId == null) {
                return invocation.proceed();
            }

            // 管理员跳过
            if (SecurityUtils.isAdmin() || SecurityUtils.hasRole("admin")) {
                return invocation.proceed();
            }

            // 构造新的 SQL：在 WHERE 后追加 dept_id 条件
            BoundSql boundSql = statementHandler.getBoundSql();
            String newSql = appendDeptFilter(boundSql.getSql(), deptId);

            MetaObject boundSqlMeta = MetaObject.forObject(boundSql,
                SystemMetaObject.DEFAULT_OBJECT_FACTORY,
                SystemMetaObject.DEFAULT_OBJECT_WRAPPER_FACTORY,
                new DefaultReflectorFactory());
            boundSqlMeta.setValue("sql", newSql);

            log.debug("DeptScopeInterceptor: appended dept_id={} filter", deptId);
        } catch (Exception e) {
            log.warn("DeptScopeInterceptor: failed to get current deptId, skipping", e);
        }

        return invocation.proceed();
    }

    /**
     * 获取当前用户的组织ID
     * 优先读取 Redis 中的切换上下文，否则使用用户所属 dept
     */
    private Long getCurrentDeptId() {
        try {
            RedisCache redisCache = SpringUtils.getBean(RedisCache.class);
            Long userId = SecurityUtils.getUserId();
            if (userId != null) {
                String cacheKey = CacheConstants.DEPT_CONTEXT_KEY + userId;
                Long switchedDeptId = redisCache.getCacheObject(cacheKey);
                if (switchedDeptId != null) {
                    return switchedDeptId;
                }
            }
            return SecurityUtils.getDeptId();
        } catch (Exception e) {
            return SecurityUtils.getDeptId();
        }
    }

    /**
     * 在 SQL 的 WHERE 子句后追加 dept_id 过滤条件
     * 自动检测 FROM 子句中的表别名，有 JOIN 时使用别名避免歧义
     */
    private String appendDeptFilter(String sql, Long deptId) {
        // 提取主表别名：查找 "from table_name alias" 或 "FROM table_name alias"
        String deptColumn = "dept_id";
        String sqlLower = sql.toLowerCase();
        int fromIdx = sqlLower.indexOf("from ");
        if (fromIdx >= 0) {
            // 从 FROM 后找到表名和别名，例如 "from iot_device d" → alias="d"
            String afterFrom = sqlLower.substring(fromIdx + 5);
            int joinIdx = afterFrom.indexOf(" join "); // 空格 + join + 空格
            String firstTableClause = joinIdx > 0 ? afterFrom.substring(0, joinIdx) : afterFrom;
            // 检查是否有 LEFT/RIGHT/INNER JOIN
            if (sqlLower.contains(" join ")) {
                // 有 JOIN: 提取主表别名
                String[] parts = firstTableClause.trim().split("\\s+");
                if (parts.length >= 2) {
                    // parts[0] = table_name, parts[1] = alias
                    String alias = parts[1];
                    if (alias.length() <= 3 && !alias.equalsIgnoreCase("left") && !alias.equalsIgnoreCase("right") && !alias.equalsIgnoreCase("inner")) {
                        deptColumn = alias + ".dept_id";
                    }
                }
            }
        }

        // 组织过滤条件：本部门 + 该部门全部子部门（ancestors 包含当前 deptId 的部门），
        // 用括号包裹避免与其它 AND 条件产生 OR 优先级问题
        String deptFilter = "(" + deptColumn + " = " + deptId
                + " OR " + deptColumn + " IN ( SELECT t.dept_id FROM sys_dept t WHERE find_in_set(" + deptId + ", t.ancestors) ))";

        StringBuilder sb = new StringBuilder(sql);
        int whereIndex = sqlLower.indexOf("where");

        if (whereIndex >= 0) {
            int insertPos = whereIndex + 5;
            sb.insert(insertPos, " " + deptFilter + " AND ");
        } else {
            int orderIndex = sqlLower.indexOf("order by");
            int groupIndex = sqlLower.indexOf("group by");
            int limitIndex = sqlLower.indexOf("limit");

            int insertPos = sql.length();
            if (orderIndex >= 0) insertPos = Math.min(insertPos, orderIndex);
            if (groupIndex >= 0) insertPos = Math.min(insertPos, groupIndex);
            if (limitIndex >= 0) insertPos = Math.min(insertPos, limitIndex);

            sb.insert(insertPos, " WHERE " + deptFilter + " ");
        }

        return sb.toString();
    }

    /**
     * 提取 SQL 主表名（FROM 后的第一个表），用于白名单判定
     *
     * @param sqlLower 已转为小写的 SQL
     * @return 主表名，找不到返回空串
     */
    private String extractMainTable(String sqlLower) {
        int fromIdx = sqlLower.indexOf("from ");
        if (fromIdx < 0) {
            return "";
        }
        String afterFrom = sqlLower.substring(fromIdx + 5).trim();
        int joinIdx = afterFrom.indexOf(" join ");
        String firstTableClause = joinIdx > 0 ? afterFrom.substring(0, joinIdx) : afterFrom;
        String[] parts = firstTableClause.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof StatementHandler) {
            return Plugin.wrap(target, this);
        }
        return target;
    }

    @Override
    public void setProperties(Properties properties) {
        // no-op
    }
}
