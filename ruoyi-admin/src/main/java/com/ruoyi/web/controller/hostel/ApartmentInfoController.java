package com.ruoyi.web.controller.hostel;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.ApartmentInfo;
import com.ruoyi.system.service.IApartmentInfoService;

/**
 * 公寓管理 信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/hostel/apartment")
public class ApartmentInfoController extends BaseController
{
    @Autowired
    private IApartmentInfoService apartmentInfoService;

    /**
     * 获取公寓分页列表
     */
    @PreAuthorize("@ss.hasPermi('hostel:apartment:list')")
    @GetMapping("/list")
    public TableDataInfo list(ApartmentInfo apartmentInfo)
    {
        startPage();
        List<ApartmentInfo> list = apartmentInfoService.selectApartmentInfoList(apartmentInfo);
        return getDataTable(list);
    }

    /**
     * 根据公寓ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('hostel:apartment:query')")
    @GetMapping(value = "/{apartmentId}")
    public AjaxResult getInfo(@PathVariable Long apartmentId)
    {
        return success(apartmentInfoService.selectApartmentInfoById(apartmentId));
    }

    /**
     * 新增公寓
     */
    @PreAuthorize("@ss.hasPermi('hostel:apartment:add')")
    @Log(title = "公寓管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ApartmentInfo apartmentInfo)
    {
        apartmentInfo.setCreateBy(SecurityUtils.getUsername());
        return toAjax(apartmentInfoService.insertApartmentInfo(apartmentInfo));
    }

    /**
     * 修改公寓
     */
    @PreAuthorize("@ss.hasPermi('hostel:apartment:edit')")
    @Log(title = "公寓管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ApartmentInfo apartmentInfo)
    {
        apartmentInfo.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(apartmentInfoService.updateApartmentInfo(apartmentInfo));
    }

    /**
     * 删除公寓
     */
    @PreAuthorize("@ss.hasPermi('hostel:apartment:remove')")
    @Log(title = "公寓管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{apartmentIds}")
    public AjaxResult remove(@PathVariable Long[] apartmentIds)
    {
        apartmentInfoService.deleteApartmentInfoByIds(apartmentIds);
        return success();
    }
}
