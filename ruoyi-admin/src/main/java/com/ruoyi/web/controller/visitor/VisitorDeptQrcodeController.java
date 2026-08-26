package com.ruoyi.web.controller.visitor;

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
import com.ruoyi.system.domain.VisitorDeptQrcode;
import com.ruoyi.system.service.IVisitorDeptQrcodeService;

/**
 * 部门访客登记二维码管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/visitor/deptQrcode")
public class VisitorDeptQrcodeController extends BaseController
{
    @Autowired
    private IVisitorDeptQrcodeService visitorDeptQrcodeService;

    /**
     * 获取部门登记二维码配置列表
     */
    @PreAuthorize("@ss.hasPermi('visitor:qrcode:list')")
    @GetMapping("/list")
    public TableDataInfo list(VisitorDeptQrcode qrcode)
    {
        startPage();
        List<VisitorDeptQrcode> list = visitorDeptQrcodeService.selectDeptQrcodeList(qrcode);
        return getDataTable(list);
    }

    /**
     * 获取部门登记二维码配置详情
     */
    @PreAuthorize("@ss.hasPermi('visitor:qrcode:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(visitorDeptQrcodeService.selectDeptQrcodeById(id));
    }

    /**
     * 新增部门登记二维码配置
     */
    @PreAuthorize("@ss.hasPermi('visitor:qrcode:add')")
    @Log(title = "部门登记二维码", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody VisitorDeptQrcode qrcode)
    {
        qrcode.setCreateBy(getUsername());
        return toAjax(visitorDeptQrcodeService.insertDeptQrcode(qrcode));
    }

    /**
     * 修改部门登记二维码配置（仅启用状态等，部门不允许变更）
     */
    @PreAuthorize("@ss.hasPermi('visitor:qrcode:edit')")
    @Log(title = "部门登记二维码", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody VisitorDeptQrcode qrcode)
    {
        qrcode.setUpdateBy(getUsername());
        return toAjax(visitorDeptQrcodeService.updateDeptQrcode(qrcode));
    }

    /**
     * 删除部门登记二维码配置
     */
    @PreAuthorize("@ss.hasPermi('visitor:qrcode:remove')")
    @Log(title = "部门登记二维码", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(visitorDeptQrcodeService.deleteDeptQrcodeByIds(ids));
    }

    /**
     * 为尚未配置登记二维码的部门批量初始化（仅管理员）
     */
    @PreAuthorize("@ss.hasPermi('visitor:qrcode:add')")
    @Log(title = "部门登记二维码", businessType = BusinessType.INSERT)
    @GetMapping("/init")
    public AjaxResult init()
    {
        // 批量初始化会覆盖全部部门，非管理员禁止执行，避免越权生成他部门二维码
        if (!SecurityUtils.isAdmin(SecurityUtils.getUserId()))
        {
            return error("仅管理员可执行批量初始化");
        }
        int count = visitorDeptQrcodeService.initAllDepts();
        return success(count);
    }
}
