package com.ruoyi.web.controller.system;

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
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.ISysNoticeService;
import com.ruoyi.system.service.ISysNoticeReadService;

/**
 * 通知公告 Controller
 *
 * @author SmartIoT
 */
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController
{
    @Autowired
    private ISysNoticeService noticeService;

    @Autowired
    private ISysNoticeReadService noticeReadService;

    /** 首页顶部公告列表（带已读状态，最多5条） */
    @GetMapping("/listTop")
    public AjaxResult listTop()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysNotice> list = noticeReadService.selectNoticeListWithReadStatus(userId, 5);
        return success(list);
    }

    /** 公告列表 */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysNotice notice)
    {
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }

    /** 公告详情 */
    @PreAuthorize("@ss.hasPermi('system:notice:query')")
    @GetMapping("/{noticeId}")
    public AjaxResult getInfo(@PathVariable Long noticeId)
    {
        return success(noticeService.selectNoticeById(noticeId));
    }

    /** 新增公告 */
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice)
    {
        notice.setCreateBy(getUsername());
        return toAjax(noticeService.insertNotice(notice));
    }

    /** 修改公告 */
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotice notice)
    {
        notice.setUpdateBy(getUsername());
        return toAjax(noticeService.updateNotice(notice));
    }

    /** 删除公告 */
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Long[] noticeIds)
    {
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }

    /** 标记公告已读 */
    @PostMapping("/markRead")
    public AjaxResult markRead(Long noticeId)
    {
        noticeReadService.markRead(noticeId, SecurityUtils.getUserId());
        return success();
    }

    /** 批量标记已读 */
    @PostMapping("/markReadAll")
    public AjaxResult markReadAll(Long[] ids)
    {
        if (ids != null)
        {
            for (Long noticeId : ids)
            {
                noticeReadService.markRead(noticeId, SecurityUtils.getUserId());
            }
        }
        return success();
    }

    /** 查询公告已读用户列表 */
    @GetMapping("/readUsers/list")
    public AjaxResult readUsers(Long noticeId, String searchValue)
    {
        return success(noticeReadService.selectReadUsersByNoticeId(noticeId, searchValue));
    }
}
