package com.ruoyi.common.core.controller;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.service.BaseCrudService;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
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
