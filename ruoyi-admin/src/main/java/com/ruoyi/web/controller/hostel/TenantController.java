package com.ruoyi.web.controller.hostel;

import java.util.Date;
import java.util.List;
import java.util.Map;
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
import com.ruoyi.system.domain.RoomInfo;
import com.ruoyi.system.domain.TenantInfo;
import com.ruoyi.system.service.IRoomInfoService;
import com.ruoyi.system.service.ITenantInfoService;

/**
 * 租客管理 信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/hostel/tenant")
public class TenantController extends BaseController
{
    @Autowired
    private ITenantInfoService tenantInfoService;

    @Autowired
    private IRoomInfoService roomInfoService;

    /**
     * 获取租客分页列表
     */
    @PreAuthorize("@ss.hasPermi('hostel:tenant:list')")
    @GetMapping("/list")
    public TableDataInfo list(TenantInfo tenantInfo)
    {
        startPage();
        List<TenantInfo> list = tenantInfoService.selectTenantInfoList(tenantInfo);
        return getDataTable(list);
    }

    /**
     * 根据租客ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('hostel:tenant:query')")
    @GetMapping(value = "/{tenantId}")
    public AjaxResult getInfo(@PathVariable Long tenantId)
    {
        return success(tenantInfoService.selectTenantInfoById(tenantId));
    }

    /**
     * 新增租客
     */
    @PreAuthorize("@ss.hasPermi('hostel:tenant:add')")
    @Log(title = "租客管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody TenantInfo tenantInfo)
    {
        tenantInfo.setCreateBy(SecurityUtils.getUsername());
        return toAjax(tenantInfoService.insertTenantInfo(tenantInfo));
    }

    /**
     * 修改租客
     */
    @PreAuthorize("@ss.hasPermi('hostel:tenant:edit')")
    @Log(title = "租客管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody TenantInfo tenantInfo)
    {
        tenantInfo.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(tenantInfoService.updateTenantInfo(tenantInfo));
    }

    /**
     * 删除租客
     */
    @PreAuthorize("@ss.hasPermi('hostel:tenant:remove')")
    @Log(title = "租客管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tenantIds}")
    public AjaxResult remove(@PathVariable Long[] tenantIds)
    {
        tenantInfoService.deleteTenantInfoByIds(tenantIds);
        return success();
    }

    /**
     * 办理入住
     * 更新租客房间/入住日期/租期/状态，同步更新房间状态和人数
     */
    @PreAuthorize("@ss.hasPermi('hostel:tenant:checkin')")
    @Log(title = "办理入住", businessType = BusinessType.UPDATE)
    @PostMapping("/check-in")
    public AjaxResult checkIn(@RequestBody Map<String, Object> body)
    {
        Long tenantId = ((Number) body.get("tenantId")).longValue();
        Long roomId = ((Number) body.get("roomId")).longValue();

        // 更新租客信息
        TenantInfo tenantInfo = tenantInfoService.selectTenantInfoById(tenantId);
        tenantInfo.setRoomId(roomId);
        tenantInfo.setCheckInDate(new Date());
        if (body.get("rentStart") != null)
        {
            tenantInfo.setRentStart(new Date(((Number) body.get("rentStart")).longValue()));
        }
        if (body.get("rentEnd") != null)
        {
            tenantInfo.setRentEnd(new Date(((Number) body.get("rentEnd")).longValue()));
        }
        tenantInfo.setStatus("NORMAL");
        tenantInfo.setUpdateBy(SecurityUtils.getUsername());
        tenantInfoService.updateTenantInfo(tenantInfo);

        // 更新房间状态：BLUE，人数+1
        RoomInfo roomInfo = roomInfoService.selectRoomInfoById(roomId);
        roomInfo.setStatus("BLUE");
        roomInfo.setTenantCount(roomInfo.getTenantCount() == null ? 1 : roomInfo.getTenantCount() + 1);
        roomInfo.setUpdateBy(SecurityUtils.getUsername());
        roomInfoService.updateRoomInfo(roomInfo);

        return success();
    }

    /**
     * 办理退租
     * 更新租客退租日期/状态，同步更新房间状态和人数
     */
    @PreAuthorize("@ss.hasPermi('hostel:tenant:checkin')")
    @Log(title = "办理退租", businessType = BusinessType.UPDATE)
    @PostMapping("/check-out/{tenantId}")
    public AjaxResult checkOut(@PathVariable Long tenantId)
    {
        // 更新租客信息
        TenantInfo tenantInfo = tenantInfoService.selectTenantInfoById(tenantId);
        tenantInfo.setCheckOutDate(new Date());
        tenantInfo.setStatus("CHECKED_OUT");
        tenantInfo.setUpdateBy(SecurityUtils.getUsername());
        tenantInfoService.updateTenantInfo(tenantInfo);

        // 更新房间：人数-1，人数为0则状态恢复GREEN
        Long roomId = tenantInfo.getRoomId();
        if (roomId != null)
        {
            RoomInfo roomInfo = roomInfoService.selectRoomInfoById(roomId);
            int newCount = (roomInfo.getTenantCount() == null ? 0 : roomInfo.getTenantCount()) - 1;
            roomInfo.setTenantCount(Math.max(newCount, 0));
            if (roomInfo.getTenantCount() == 0)
            {
                roomInfo.setStatus("GREEN");
            }
            roomInfo.setUpdateBy(SecurityUtils.getUsername());
            roomInfoService.updateRoomInfo(roomInfo);
        }

        return success();
    }

    /**
     * 续租
     * 更新租客的租期结束日期
     */
    @PreAuthorize("@ss.hasPermi('hostel:tenant:edit')")
    @Log(title = "续租管理", businessType = BusinessType.UPDATE)
    @PutMapping("/renew/{tenantId}")
    public AjaxResult renew(@PathVariable Long tenantId, @RequestBody Map<String, Object> body)
    {
        TenantInfo tenantInfo = tenantInfoService.selectTenantInfoById(tenantId);
        if (body.get("rentEnd") != null)
        {
            tenantInfo.setRentEnd(new Date(((Number) body.get("rentEnd")).longValue()));
        }
        tenantInfo.setUpdateBy(SecurityUtils.getUsername());
        tenantInfoService.updateTenantInfo(tenantInfo);

        return success();
    }
}
