package com.ruoyi.framework.aspectj;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AOP 切面：自动为新增/修改的实体填充 dept_id（所属组织）
 *
 * 拦截 Controller 层 add/edit/save/insert/update 方法
 * 对于继承 BaseEntity 的入参，自动设置 deptId
 */
@Aspect
@Component
public class DeptFillAspect {

    private static final Logger log = LoggerFactory.getLogger(DeptFillAspect.class);

    @Before("execution(* com.ruoyi.web.controller..*.add*(..)) || " +
            "execution(* com.ruoyi.web.controller..*.edit*(..)) || " +
            "execution(* com.ruoyi.web.controller..*.save*(..)) || " +
            "execution(* com.ruoyi.web.controller..*.insert*(..)) || " +
            "execution(* com.ruoyi.web.controller..*.update*(..))")

    public void fillDeptId(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return;
            }
            Long currentDeptId = getCurrentDeptId();
            if (currentDeptId == null) {
                return;
            }
            for (Object arg : args) {
                if (arg instanceof BaseEntity) {
                    // SysDept.deptId 是部门主键，不是数据范围字段，必须跳过
                    if (arg instanceof SysDept) {
                        continue;
                    }
                    BaseEntity entity = (BaseEntity) arg;
                    if (entity.getDeptId() == null) {
                        entity.setDeptId(currentDeptId);
                        log.debug("DeptFillAspect: auto-filled deptId={} for {}", currentDeptId,
                                arg.getClass().getSimpleName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("DeptFillAspect: failed to fill deptId", e);
        }
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
}
