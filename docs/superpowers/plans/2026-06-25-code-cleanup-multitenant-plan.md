# 代码精简 + 多租户改造 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 分两阶段：1) 删除死代码 + 抽取 CRUD 基类 + 前端 mixin；2) 实现基于 Dept 的多租户自动数据隔离

**Architecture:** 第一阶段新增 BaseCrudController/BaseCrudService 基类减少重复模板；第二阶段新增 MyBatis 拦截器 DeptScopeInterceptor 实现查询自动过滤 + AOP 写入自动填充 dept_id

**Tech Stack:** Java 17, SpringBoot 4.0.3, MyBatis, Vue 2.6, Element UI 2.15

## Global Constraints

- 不改动 ruoyi-framework 基础配置类（SecurityConfig, DruidConfig 等）
- 不强制所有 Controller 继承基类（登录/WebSocket/回调等排除）
- 不引入独立 tenant 表，复用 sys_dept
- 所有业务数据隔离以 dept_id 为核心
- 管理员 (userId=1 或 data_scope=ALL) 可跨组织查看
- 数据库变更已提前完成（9 张表已加 dept_id 列和索引）

---

## File Structure Map

### Files to CREATE:
```
ruoyi-common/src/main/java/com/ruoyi/common/core/controller/BaseCrudController.java
ruoyi-common/src/main/java/com/ruoyi/common/core/service/BaseCrudService.java
ruoyi-common/src/main/java/com/ruoyi/common/core/service/impl/BaseCrudServiceImpl.java
ruoyi-framework/src/main/java/com/ruoyi/framework/interceptor/DeptScopeInterceptor.java
ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/DeptFillAspect.java
ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysDeptController.java (新增 switchContext 方法)
ruoyi-ui/src/mixins/listPageMixin.js
ruoyi-ui/src/mixins/formDialogMixin.js
ruoyi-ui/src/components/CrudTable/index.vue
```

### Files to MODIFY:
```
ruoyi-common/src/main/java/com/ruoyi/common/core/domain/BaseEntity.java     (新增 deptId 字段)
ruoyi-common/src/main/java/com/ruoyi/common/constant/CacheConstants.java    (新增 DEPT_CONTEXT_KEY)
ruoyi-framework/src/main/java/com/ruoyi/framework/config/MyBatisConfig.java (注册 DeptScopeInterceptor)
ruoyi-system/src/main/java/com/ruoyi/system/domain/*.java                   (各业务实体确认 deptId 字段)
ruoyi-ui/src/layout/components/Navbar.vue                                   (新增组织切换下拉框)
ruoyi-ui/src/api/system/dept.js                                             (新增 switchContext API)
```

### Files to DELETE:
```
ruoyi-quartz/src/main/java/com/ruoyi/quartz/config/ScheduleConfig.java
ruoyi-common/src/main/java/com/ruoyi/common/constant/GenConstants.java
ruoyi-admin/src/main/java/com/ruoyi/web/core/config/SwaggerConfig.java (如有)
ruoyi-ui/src/api/menu.js
ruoyi-ui/src/api/system/dict/data.js
ruoyi-ui/src/utils/generator/ (整个目录)
ruoyi-ui/src/views/index_v1.vue (废弃的备用首页)
```

### Files NOT TOUCHED (白名单):
```
ruoyi-framework/config/SecurityConfig.java
ruoyi-framework/config/DruidConfig.java
ruoyi-framework/config/RedisConfig.java
ruoyi-framework/aspectj/DataScopeAspect.java (保留但降级)
ruoyi-admin 中 login/websocket/callback 相关 Controller
```

---

## PHASE 1: 代码精简

---

### Task 1: 删除后端死代码文件

**Files:**
- Delete: `ruoyi-quartz/src/main/java/com/ruoyi/quartz/config/ScheduleConfig.java`
- Delete: `ruoyi-common/src/main/java/com/ruoyi/common/constant/GenConstants.java`

**Interfaces:**
- Consumes: nothing
- Produces: nothing (pure deletion)

- [ ] **Step 1: 删除 ScheduleConfig.java**

```bash
rm "e:\gitee\production_release\RuoYi-Vue-master2026-06-24\ruoyi-quartz\src\main\java\com\ruoyi\quartz\config\ScheduleConfig.java"
```

- [ ] **Step 2: 删除 GenConstants.java**

```bash
rm "e:\gitee\production_release\RuoYi-Vue-master2026-06-24\ruoyi-common\src\main\java\com\ruoyi\common\constant\GenConstants.java"
```

- [ ] **Step 3: 检查是否有编译错误（引用了被删文件的类）**

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24" && grep -r "GenConstants" --include="*.java" ruoyi-*/src/
```

Expected: no output or only in GenConstants.java itself.

- [ ] **Step 4: Commit**

```bash
git add -A && git commit -m "chore: delete dead code - ScheduleConfig, GenConstants"
```

---

### Task 2: 删除前端死代码文件

**Files:**
- Delete: `ruoyi-ui/src/api/menu.js`
- Delete: `ruoyi-ui/src/api/system/dict/data.js`
- Delete: `ruoyi-ui/src/utils/generator/` (整个目录)
- Delete: `ruoyi-ui/src/views/index_v1.vue`

**Interfaces:**
- Consumes: nothing
- Produces: nothing (pure deletion)

- [ ] **Step 1: 删除前端死代码文件**

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24\ruoyi-ui"
rm src/api/menu.js
rm src/api/system/dict/data.js
rm -rf src/utils/generator
rm src/views/index_v1.vue
```

- [ ] **Step 2: 检查是否有其他文件 import 了被删文件**

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24\ruoyi-ui"
grep -r "menu\.js\|dict/data\|generator\|index_v1" --include="*.js" --include="*.vue" src/
```

Expected: no output.

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "chore: delete dead frontend code - menu.js, dict/data.js, generator/, index_v1.vue"
```

---

### Task 3: 创建 BaseCrudService 基类

**Files:**
- Create: `ruoyi-common/src/main/java/com/ruoyi/common/core/service/BaseCrudService.java`
- Create: `ruoyi-common/src/main/java/com/ruoyi/common/core/service/impl/BaseCrudServiceImpl.java`

**Interfaces:**
- Consumes: `BaseEntity` (existing), MyBatis Mapper pattern
- Produces:
  - `BaseCrudService<E>` interface with: `List<E> selectList(E)`, `E selectById(Long)`, `int insert(E)`, `int update(E)`, `int deleteByIds(Long[])`
  - `BaseCrudServiceImpl<M, E>` with default implementations

- [ ] **Step 1: 创建 BaseCrudService.java**

```java
// file: ruoyi-common/src/main/java/com/ruoyi/common/core/service/BaseCrudService.java
package com.ruoyi.common.core.service;

import java.util.List;

/**
 * 通用 CRUD Service 接口
 * @param <E> 实体类型
 */
public interface BaseCrudService<E> {

    List<E> selectList(E entity);

    E selectById(Long id);

    int insert(E entity);

    int update(E entity);

    int deleteByIds(Long[] ids);
}
```

- [ ] **Step 2: 创建 BaseCrudServiceImpl.java**

```java
// file: ruoyi-common/src/main/java/com/ruoyi/common/core/service/impl/BaseCrudServiceImpl.java
package com.ruoyi.common.core.service.impl;

import com.ruoyi.common.core.service.BaseCrudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 通用 CRUD Service 实现基类
 * @param <M> Mapper 类型
 * @param <E> 实体类型
 */
public abstract class BaseCrudServiceImpl<M, E> implements BaseCrudService<E> {

    @Autowired
    protected M mapper;

    protected abstract List<E> doSelectList(E entity);

    protected abstract E doSelectById(Long id);

    protected abstract int doInsert(E entity);

    protected abstract int doUpdate(E entity);

    protected abstract int doDeleteByIds(Long[] ids);

    @Override
    public List<E> selectList(E entity) {
        return doSelectList(entity);
    }

    @Override
    public E selectById(Long id) {
        return doSelectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(E entity) {
        return doInsert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(E entity) {
        return doUpdate(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids) {
        return doDeleteByIds(ids);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: add BaseCrudService and BaseCrudServiceImpl base classes"
```

---

### Task 4: 创建 BaseCrudController 基类

**Files:**
- Create: `ruoyi-common/src/main/java/com/ruoyi/common/core/controller/BaseCrudController.java`

**Interfaces:**
- Consumes: `BaseCrudService<E>` (from Task 3), `BaseEntity`, `BaseController` (existing)
- Produces: `BaseCrudController<E, S extends BaseCrudService<E>>` with: `list()`, `getInfo()`, `add()`, `edit()`, `remove()`, `export()`

- [ ] **Step 1: 创建 BaseCrudController.java**

```java
// file: ruoyi-common/src/main/java/com/ruoyi/common/core/controller/BaseCrudController.java
package com.ruoyi.common.core.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.BaseCrudService;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * 通用 CRUD Controller 基类
 * @param <E> 实体类型
 * @param <S> Service 类型
 */
public abstract class BaseCrudController<E, S extends BaseCrudService<E>> extends BaseController {

    protected abstract S getService();

    protected abstract String getPermissionPrefix();

    @GetMapping("/list")
    public TableDataInfo list(E entity) {
        startPage();
        List<E> list = getService().selectList(entity);
        return getDataTable(list);
    }

    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(getService().selectById(id));
    }

    @Log(title = "新增", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody E entity) {
        return toAjax(getService().insert(entity));
    }

    @Log(title = "修改", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody E entity) {
        return toAjax(getService().update(entity));
    }

    @Log(title = "删除", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(getService().deleteByIds(ids));
    }

    @Log(title = "导出", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, E entity) {
        List<E> list = getService().selectList(entity);
        ExcelUtil<E> util = new ExcelUtil<>(getEntityClass());
        util.exportExcel(response, list, "数据导出");
    }

    protected abstract Class<E> getEntityClass();
}
```

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "feat: add BaseCrudController base class"
```

---

### Task 5: 前端 Mixin 抽取 — listPageMixin

**Files:**
- Create: `ruoyi-ui/src/mixins/listPageMixin.js`

**Interfaces:**
- Consumes: Vue 2 mixin API
- Produces: `listPageMixin` — data: { queryParams, list, total, loading, ids, dateRange }, methods: { getList, handleQuery, handleExport, handleSelectionChange, handleDelete, resetQuery }

- [ ] **Step 1: 创建 listPageMixin.js**

```javascript
// file: ruoyi-ui/src/mixins/listPageMixin.js
export default {
  data() {
    return {
      queryParams: {},
      list: [],
      total: 0,
      loading: false,
      ids: [],
      dateRange: []
    }
  },
  methods: {
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.queryParams = {}
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item[this.idField || 'id'])
    },
    handleDelete(row) {
      const ids = row[this.idField || 'id'] || this.ids
      this.$modal.confirm('确认删除？').then(() => {
        return this.deleteApi(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      })
    },
    handleExport() {
      this.$modal.confirm('确认导出？').then(() => {
        return this.exportApi(this.queryParams)
      }).then((res) => {
        this.$download.name(res.msg)
      })
    }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "feat: add listPageMixin for frontend list pages"
```

---

### Task 6: 前端 Mixin 抽取 — formDialogMixin

**Files:**
- Create: `ruoyi-ui/src/mixins/formDialogMixin.js`

**Interfaces:**
- Consumes: Vue 2 mixin API
- Produces: `formDialogMixin` — data: { open, title, form }, methods: { handleAdd, handleUpdate, submitForm, reset, cancel }

- [ ] **Step 1: 创建 formDialogMixin.js**

```javascript
// file: ruoyi-ui/src/mixins/formDialogMixin.js
export default {
  data() {
    return {
      open: false,
      title: '',
      form: {}
    }
  },
  methods: {
    reset() {
      this.form = this.$options.data().form || {}
      this.resetForm && this.resetForm('form')
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增'
    },
    handleUpdate(row) {
      this.reset()
      const id = row[this.idField || 'id'] || this.ids[0]
      this.getApi(id).then(res => {
        this.form = res.data
        this.open = true
        this.title = '修改'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          const api = this.form[this.idField || 'id']
            ? this.updateApi(this.form)
            : this.addApi(this.form)
          api.then(() => {
            this.$modal.msgSuccess('操作成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    cancel() {
      this.open = false
      this.reset()
    }
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "feat: add formDialogMixin for frontend form dialogs"
```

---

### Task 7: 前端 CrudTable 通用表格组件

**Files:**
- Create: `ruoyi-ui/src/components/CrudTable/index.vue`

**Interfaces:**
- Consumes: Element UI el-table, Pagination component
- Produces: `<CrudTable>` with props: columns, data, total, loading, selection

- [ ] **Step 1: 创建 CrudTable/index.vue**

```vue
<!-- file: ruoyi-ui/src/components/CrudTable/index.vue -->
<template>
  <div class="crud-table">
    <el-table
      v-loading="loading"
      :data="data"
      @selection-change="handleSelectionChange"
      border
      stripe
    >
      <el-table-column v-if="selection" type="selection" width="55" align="center" />
      <template v-for="col in columns">
        <el-table-column
          :key="col.prop"
          :label="col.label"
          :prop="col.prop"
          :width="col.width"
          :align="col.align || 'center'"
          :sortable="col.sortable"
          :formatter="col.formatter"
        >
          <template v-if="col.slot" v-slot="scope">
            <slot :name="col.slot" :row="scope.row" :index="scope.$index" />
          </template>
        </el-table-column>
      </template>
    </el-table>
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
export default {
  name: 'CrudTable',
  props: {
    columns: { type: Array, required: true },
    data: { type: Array, default: () => [] },
    total: { type: Number, default: 0 },
    loading: { type: Boolean, default: false },
    queryParams: { type: Object, default: () => ({}) },
    selection: { type: Boolean, default: true }
  },
  methods: {
    handleSelectionChange(selection) {
      this.$emit('selection-change', selection)
    },
    getList() {
      this.$emit('get-list')
    }
  }
}
</script>
```

- [ ] **Step 2: 在 main.js 中全局注册 CrudTable**

Modify `ruoyi-ui/src/main.js` — after existing global component registrations, add:

```javascript
import CrudTable from '@/components/CrudTable'
Vue.component('CrudTable', CrudTable)
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: add CrudTable reusable table component"
```

---

### Task 8: 验证第一阶段 — 编译检查

**Files:** (none new)

**Interfaces:**
- Consumes: all Phase 1 changes
- Produces: compilation verification

- [ ] **Step 1: 后端编译验证**

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24"
# 使用 Maven 编译（跳过测试）
mvn compile -pl ruoyi-common,ruoyi-system,ruoyi-framework,ruoyi-quartz,ruoyi-admin -am -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: 前端构建检查**

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24\ruoyi-ui"
npm run build:prod 2>&1 | tail -20
```

Expected: no build errors.

- [ ] **Step 3: Commit (if any fixes needed)**

```bash
git add -A && git commit -m "fix: compilation fixes after Phase 1 cleanup"
```

---

## PHASE 2: 多租户改造

---

### Task 9: BaseEntity 新增 deptId 字段

**Files:**
- Modify: `ruoyi-common/src/main/java/com/ruoyi/common/core/domain/BaseEntity.java`
- Modify: `ruoyi-common/src/main/java/com/ruoyi/common/constant/CacheConstants.java`

**Interfaces:**
- Consumes: nothing new
- Produces: `BaseEntity.deptId` (Long), `CacheConstants.DEPT_CONTEXT_KEY`

- [ ] **Step 1: BaseEntity 新增 deptId**

In `BaseEntity.java`, after the `remark` field, add:

```java
/** 所属组织ID（多租户数据隔离） */
private Long deptId;

public Long getDeptId() {
    return deptId;
}

public void setDeptId(Long deptId) {
    this.deptId = deptId;
}
```

- [ ] **Step 2: CacheConstants 新增 DEPT_CONTEXT_KEY**

In `CacheConstants.java`, add:

```java
/** 管理员组织切换上下文 */
public static final String DEPT_CONTEXT_KEY = "dept_context:";
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: add deptId field to BaseEntity and DEPT_CONTEXT_KEY constant"
```

---

### Task 10: 创建 DeptScopeInterceptor — 查询自动过滤

**Files:**
- Create: `ruoyi-framework/src/main/java/com/ruoyi/framework/interceptor/DeptScopeInterceptor.java`

**Interfaces:**
- Consumes: `SecurityUtils.getDeptId()`, `SecurityUtils.isAdmin()`, `RedisCache`
- Produces: MyBatis Interceptor that auto-appends `WHERE dept_id = ?` to SELECT queries on business tables

- [ ] **Step 1: 创建 DeptScopeInterceptor.java**

```java
// file: ruoyi-framework/src/main/java/com/ruoyi/framework/interceptor/DeptScopeInterceptor.java
package com.ruoyi.framework.interceptor;

import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
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
 * 白名单表（系统表/全局共享表）不添加过滤
 * 管理员跳过过滤
 */
@Intercepts({
    @Signature(type = StatementHandler.class, method = "prepare",
               args = {Connection.class, Integer.class})
})
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

        // 检查是否包含白名单表（任一白名单表出现在 SQL 中则跳过）
        for (String table : WHITELIST_TABLES) {
            if (originalSql.contains(table)) {
                return invocation.proceed();
            }
        }

        // 检查是否已包含 dept_id 条件（避免重复添加）
        if (originalSql.contains("dept_id")) {
            return invocation.proceed();
        }

        try {
            Long deptId = getCurrentDeptId();
            if (deptId == null) {
                return invocation.proceed();
            }

            // 管理员跳过
            if (SecurityUtils.isAdmin()) {
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
     */
    private String appendDeptFilter(String sql, Long deptId) {
        StringBuilder sb = new StringBuilder(sql);
        int whereIndex = sql.toLowerCase().indexOf("where");
        
        if (whereIndex >= 0) {
            // 已有 WHERE，追加 AND
            int insertPos = whereIndex + 5;
            sb.insert(insertPos, " dept_id = " + deptId + " AND ");
        } else {
            // 无 WHERE 子句的情况
            int orderIndex = sql.toLowerCase().indexOf("order by");
            int groupIndex = sql.toLowerCase().indexOf("group by");
            int limitIndex = sql.toLowerCase().indexOf("limit");
            
            int insertPos = sql.length();
            if (orderIndex >= 0) insertPos = Math.min(insertPos, orderIndex);
            if (groupIndex >= 0) insertPos = Math.min(insertPos, groupIndex);
            if (limitIndex >= 0) insertPos = Math.min(insertPos, limitIndex);
            
            sb.insert(insertPos, " WHERE dept_id = " + deptId + " ");
        }
        
        return sb.toString();
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
```

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "feat: add DeptScopeInterceptor for automatic dept_id query filtering"
```

---

### Task 11: 创建 DeptFillAspect — 写入自动填充

**Files:**
- Create: `ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/DeptFillAspect.java`

**Interfaces:**
- Consumes: `BaseEntity.deptId`, `SecurityUtils.getDeptId()`
- Produces: AOP aspect that auto-fills `deptId` on add/edit controller methods

- [ ] **Step 1: 创建 DeptFillAspect.java**

```java
// file: ruoyi-framework/src/main/java/com/ruoyi/framework/aspectj/DeptFillAspect.java
package com.ruoyi.framework.aspectj;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.utils.SecurityUtils;
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

            Long currentDeptId = SecurityUtils.getDeptId();
            if (currentDeptId == null) {
                return;
            }

            for (Object arg : args) {
                if (arg instanceof BaseEntity) {
                    BaseEntity entity = (BaseEntity) arg;
                    if (entity.getDeptId() == null) {
                        entity.setDeptId(currentDeptId);
                        log.debug("DeptFillAspect: auto-filled deptId={} for {}", currentDeptId, arg.getClass().getSimpleName());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("DeptFillAspect: failed to fill deptId", e);
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "feat: add DeptFillAspect for automatic dept_id write fill"
```

---

### Task 12: 管理员组织切换 — 后端接口

**Files:**
- Modify: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysDeptController.java` (如果已存在则修改，否则创建)
- or locate the existing dept controller to add a new method

**Interfaces:**
- Consumes: `ISysDeptService`, `RedisCache`, `SecurityUtils`
- Produces: `GET /system/dept/switchContext/{deptId}` — stores switched dept_id in Redis

- [ ] **Step 1: 在 SysDeptController 中新增切换组织方法**

Locate the existing SysDeptController at: `ruoyi-admin/src/main/java/com/ruoyi/web/controller/system/SysDeptController.java`

Add method:

```java
/**
 * 管理员切换当前组织视角
 */
@GetMapping("/switchContext/{deptId}")
public AjaxResult switchContext(@PathVariable Long deptId) {
    if (!SecurityUtils.isAdmin()) {
        return error("仅管理员可切换组织");
    }
    Long userId = SecurityUtils.getUserId();
    String cacheKey = CacheConstants.DEPT_CONTEXT_KEY + userId;
    redisCache.setCacheObject(cacheKey, deptId);
    return success("已切换到组织ID: " + deptId);
}

/**
 * 清除组织切换上下文，恢复默认
 */
@GetMapping("/clearContext")
public AjaxResult clearContext() {
    Long userId = SecurityUtils.getUserId();
    String cacheKey = CacheConstants.DEPT_CONTEXT_KEY + userId;
    redisCache.deleteObject(cacheKey);
    return success("已恢复默认组织");
}
```

Add import:
```java
import com.ruoyi.common.constant.CacheConstants;
import org.springframework.beans.factory.annotation.Autowired;
import com.ruoyi.common.core.redis.RedisCache;
```

Add field:
```java
@Autowired
private RedisCache redisCache;
```

- [ ] **Step 2: Commit**

```bash
git add -A && git commit -m "feat: add dept switch context API for admin"
```

---

### Task 13: 管理员组织切换 — 前端

**Files:**
- Create/Modify: `ruoyi-ui/src/api/system/dept.js` (add switchContext/clearContext API)
- Modify: `ruoyi-ui/src/layout/components/Navbar.vue` (add org selector dropdown)

**Interfaces:**
- Consumes: dept API, Vuex user store
- Produces: org selector dropdown in top navbar (admin only)

- [ ] **Step 1: 在 dept.js API 中添加切换组织接口**

In `ruoyi-ui/src/api/system/dept.js`, add:

```javascript
// 切换组织视角（管理员）
export function switchDeptContext(deptId) {
  return request({
    url: '/system/dept/switchContext/' + deptId,
    method: 'get'
  })
}

// 清除组织切换上下文
export function clearDeptContext() {
  return request({
    url: '/system/dept/clearContext',
    method: 'get'
  })
}

// 获取部门树（用于选择器）
export function deptTreeSelect() {
  return request({
    url: '/system/dept/treeselect',
    method: 'get'
  })
}
```

- [ ] **Step 2: 在 Navbar.vue 中添加组织切换下拉框**

在 `ruoyi-ui/src/layout/components/Navbar.vue` 的 right-menu 区域，在搜索按钮之前添加：

```vue
<!-- 组织切换（仅管理员可见） -->
<el-dropdown v-if="isAdmin" @command="handleSwitchDept" class="dept-switch">
  <span class="el-dropdown-link">
    {{ currentDeptName || '选择组织' }} <i class="el-icon-arrow-down el-icon--right" />
  </span>
  <el-dropdown-menu slot="dropdown">
    <el-dropdown-item command="__clear__">恢复默认</el-dropdown-item>
    <el-dropdown-item divided v-for="dept in deptOptions" :key="dept.id" :command="dept.id">
      {{ dept.label }}
    </el-dropdown-item>
  </el-dropdown-menu>
</el-dropdown>
```

Add data:
```javascript
deptOptions: [],
currentDeptName: ''
```

Add computed:
```javascript
isAdmin() {
  return this.$store.getters.roles.includes('admin')
}
```

Add methods:
```javascript
loadDeptOptions() {
  deptTreeSelect().then(res => {
    this.deptOptions = res.data
  })
},
handleSwitchDept(deptId) {
  if (deptId === '__clear__') {
    clearDeptContext().then(() => {
      this.currentDeptName = ''
      this.$modal.msgSuccess('已恢复默认组织')
      this.refreshPage()
    })
  } else {
    switchDeptContext(deptId).then(() => {
      const dept = this.deptOptions.find(d => d.id === deptId)
      this.currentDeptName = dept ? dept.label : ''
      this.$modal.msgSuccess('已切换到: ' + this.currentDeptName)
      this.refreshPage()
    })
  }
},
refreshPage() {
  // 刷新当前页面和标签页
  this.$store.dispatch('tagsView/delAllCachedViews')
  this.$router.go(0)
}
```

Add mounted:
```javascript
mounted() {
  if (this.isAdmin) {
    this.loadDeptOptions()
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add -A && git commit -m "feat: add admin org selector dropdown in navbar"
```

---

### Task 14: 业务实体类确认 deptId 字段

**Files:**
- Check all domain entities under `ruoyi-system/src/main/java/com/ruoyi/system/domain/` that map to the 9 business tables

**Interfaces:**
- Consumes: `BaseEntity.deptId` (from Task 9)
- Produces: verified all business entities inherit deptId via BaseEntity

- [ ] **Step 1: 确认所有业务实体类继承 BaseEntity**

Check these entity classes extend `BaseEntity`:

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24"
grep -l "extends BaseEntity" ruoyi-system/src/main/java/com/ruoyi/system/domain/*.java
```

Expected output should include: Device, DeviceRepair, DevicePort, DeviceHeartbeatLog, DeviceStatusLog, VisitorAppointment, VisitorLog, SmsLog, AttendanceCallbackLog.

If any do NOT extend BaseEntity, they need to be modified.

- [ ] **Step 2: 检查 MyBatis XML 中 resultMap 包含 dept_id 列**

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24"
grep -l "dept_id" ruoyi-system/src/main/resources/mapper/system/*.xml
```

Ensure the 9 business table XMLs include `dept_id` column in their resultMap and SQL. If any are missing, add:

```xml
<result property="deptId" column="dept_id"/>
```

- [ ] **Step 3: Commit (if any fixes)**

```bash
git add -A && git commit -m "fix: ensure all business entities map dept_id column"
```

---

### Task 15: 最终验证

**Files:** (none new)

- [ ] **Step 1: 完整编译**

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24"
mvn clean compile -DskipTests
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: 前端构建**

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24\ruoyi-ui"
npm run build:prod
```

Expected: no errors.

- [ ] **Step 3: 启动应用**

```bash
cd "e:\gitee\production_release\RuoYi-Vue-master2026-06-24"
mvn spring-boot:run -pl ruoyi-admin
```

Expected: application starts successfully.

- [ ] **Step 4: 验证多租户隔离**

```
1. 用普通用户登录 → 访问 /device/list
   → 确认只返回该用户所属 dept 的设备数据

2. 用管理员登录 → 访问 /device/list
   → 确认可看到所有数据

3. 管理员切换组织 → 访问 /device/list
   → 确认只返回所选组织的数据

4. 新增一条设备数据
   → 确认 dept_id 自动填充为当前用户 dept
```

- [ ] **Step 5: Commit**

```bash
git add -A && git commit -m "chore: final verification of multi-tenant isolation"
```
