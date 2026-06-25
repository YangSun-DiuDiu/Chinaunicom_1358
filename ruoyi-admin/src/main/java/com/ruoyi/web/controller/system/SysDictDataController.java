package com.ruoyi.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.service.ISysDictDataService;

/**
 * 字典数据
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/dict/data")
public class SysDictDataController
{
    @Autowired
    private ISysDictDataService dictDataService;

    /**
     * 根据字典类型查询字典数据
     */
    @GetMapping("/type/{dictType}")
    public AjaxResult dictType(@PathVariable String dictType)
    {
        SysDictData dictData = new SysDictData();
        dictData.setDictType(dictType);
        dictData.setStatus("0");
        List<SysDictData> list = dictDataService.selectDictDataList(dictData);
        return AjaxResult.success(list);
    }
}
