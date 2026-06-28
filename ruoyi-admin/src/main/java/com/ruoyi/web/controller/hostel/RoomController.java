package com.ruoyi.web.controller.hostel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
import com.ruoyi.system.service.IRoomInfoService;

/**
 * 房间管理 信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/hostel/room")
public class RoomController extends BaseController
{
    @Autowired
    private IRoomInfoService roomInfoService;
    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 获取房间分页列表
     */
    @PreAuthorize("@ss.hasPermi('hostel:room:list')")
    @GetMapping("/list")
    public TableDataInfo list(RoomInfo roomInfo)
    {
        startPage();
        List<RoomInfo> list = roomInfoService.selectRoomInfoList(roomInfo);
        return getDataTable(list);
    }

    /**
     * 根据房间ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('hostel:room:query')")
    @GetMapping(value = "/{roomId}")
    public AjaxResult getInfo(@PathVariable Long roomId)
    {
        return success(roomInfoService.selectRoomInfoById(roomId));
    }

    /**
     * 获取房间卡片列表（支持公寓ID/楼栋/楼层/状态筛选）
     */
    @PreAuthorize("@ss.hasPermi('hostel:room:card')")
    @GetMapping("/card/list")
    public AjaxResult cardList(RoomInfo roomInfo)
    {
        List<RoomInfo> list = roomInfoService.selectRoomCardList(roomInfo);
        // 为每个房间附加租客姓名列表
        for (RoomInfo r : list) {
            List<Map<String, Object>> tenants = jdbc.queryForList(
                "SELECT tenant_name FROM tenant_info WHERE room_id=? AND status='NORMAL'", r.getRoomId());
            List<String> names = new java.util.ArrayList<>();
            for (Map<String, Object> t : tenants) names.add((String) t.get("tenant_name"));
            r.getParams().put("tenantNames", names);
            // 实际人数 & 容量
            r.getParams().put("actualCount", names.size());
        }
        return success(list);
    }

    /**
     * 获取房间弹窗详情（完整信息）
     */
    @PreAuthorize("@ss.hasPermi('hostel:room:query')")
    @GetMapping(value = "/{roomId}/detail")
    public AjaxResult detail(@PathVariable Long roomId)
    {
        return success(roomInfoService.selectRoomInfoById(roomId));
    }

    /**
     * 新增房间
     */
    @PreAuthorize("@ss.hasPermi('hostel:room:add')")
    @Log(title = "房间管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody RoomInfo roomInfo)
    {
        roomInfo.setCreateBy(SecurityUtils.getUsername());
        return toAjax(roomInfoService.insertRoomInfo(roomInfo));
    }

    /**
     * 修改房间
     */
    @PreAuthorize("@ss.hasPermi('hostel:room:edit')")
    @Log(title = "房间管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody RoomInfo roomInfo)
    {
        roomInfo.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(roomInfoService.updateRoomInfo(roomInfo));
    }

    /**
     * 删除房间
     */
    @PreAuthorize("@ss.hasPermi('hostel:room:remove')")
    @Log(title = "房间管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roomIds}")
    public AjaxResult remove(@PathVariable Long[] roomIds)
    {
        roomInfoService.deleteRoomInfoByIds(roomIds);
        return success();
    }
}
