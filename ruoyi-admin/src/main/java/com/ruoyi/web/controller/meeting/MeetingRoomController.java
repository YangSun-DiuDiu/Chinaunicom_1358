package com.ruoyi.web.controller.meeting;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.MeetingBooking;
import com.ruoyi.system.domain.MeetingRoom;
import com.ruoyi.system.service.IMeetingBookingService;
import com.ruoyi.system.service.IMeetingRoomService;
import com.ruoyi.system.service.ISysDeptService;

/**
 * 会议室管理 信息操作处理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/meeting/room")
public class MeetingRoomController extends BaseController
{
    @Autowired
    private IMeetingRoomService meetingRoomService;

    @Autowired
    private IMeetingBookingService meetingBookingService;

    @Autowired
    private ISysDeptService deptService;

    /**
     * 获取会议室分页列表
     */
    @PreAuthorize("@ss.hasPermi('meeting:room:list')")
    @GetMapping("/list")
    public TableDataInfo list(MeetingRoom meetingRoom)
    {
        startPage();
        List<MeetingRoom> list = meetingRoomService.selectMeetingRoomList(meetingRoom);
        return getDataTable(list);
    }

    /**
     * 获取组织架构部门树（与系统管理-部门管理数据同步）
     */
    @PreAuthorize("@ss.hasPermi('meeting:room:list')")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept)
    {
        return success(deptService.selectDeptTreeList(dept));
    }

    /**
     * 获取会议室使用状态总览（所有会议室当前状态 + 今日预约记录）
     * <p>需登录授权（meeting:room:list）；会议室与预约记录均按登录用户部门隔离（DeptScopeInterceptor）</p>
     *
     * @return 每间会议室 [{ room, currentStatus, currentBooking, nextBooking, todayBookings }]
     */
    @PreAuthorize("@ss.hasPermi('meeting:room:list')")
    @GetMapping("/status")
    public AjaxResult status()
    {
        // 会议室列表（拦截器自动按登录用户部门隔离）
        List<MeetingRoom> roomList = meetingRoomService.selectMeetingRoomList(new MeetingRoom());
        // 今日全部已通过预约（一次查询，避免循环内查询；拦截器同样按部门隔离）
        List<MeetingBooking> todayList = meetingBookingService.selectTodayApprovedList();

        // 按 roomId 分组今日预约
        Map<Long, List<MeetingBooking>> todayByRoom = new HashMap<>();
        for (MeetingBooking booking : todayList)
        {
            todayByRoom.computeIfAbsent(booking.getRoomId(), k -> new ArrayList<>()).add(booking);
        }

        // 组装每间会议室状态
        List<Map<String, Object>> resultList = new ArrayList<>();
        for (MeetingRoom room : roomList)
        {
            List<MeetingBooking> roomBookings = todayByRoom.getOrDefault(room.getRoomId(), new ArrayList<>());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("room", room);
            item.put("currentStatus", meetingRoomService.calcCurrentStatus(roomBookings));
            item.put("currentBooking", meetingRoomService.getCurrentBooking(roomBookings));
            item.put("nextBooking", meetingRoomService.getNextBooking(roomBookings));
            item.put("todayBookings", roomBookings);
            resultList.add(item);
        }
        return success(resultList);
    }

    /**
     * 获取会议室简单列表（供门牌平板配置页使用，无需权限认证）
     */
    @GetMapping("/simpleList")
    public AjaxResult simpleList()
    {
        List<MeetingRoom> list = meetingRoomService.selectMeetingRoomList(new MeetingRoom());
        List<Map<String, Object>> simpleList = new java.util.ArrayList<>();
        for (MeetingRoom room : list)
        {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("roomId", room.getRoomId());
            item.put("roomName", room.getRoomName());
            item.put("location", room.getLocation());
            simpleList.add(item);
        }
        return success(simpleList);
    }

    /**
     * 根据会议室ID获取详细信息
     */
    @PreAuthorize("@ss.hasPermi('meeting:room:query')")
    @GetMapping(value = "/{roomId}")
    public AjaxResult getInfo(@PathVariable Long roomId)
    {
        return success(meetingRoomService.selectMeetingRoomById(roomId));
    }

    /**
     * 新增会议室
     */
    @PreAuthorize("@ss.hasPermi('meeting:room:add')")
    @Log(title = "会议室管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody MeetingRoom meetingRoom)
    {
        meetingRoom.setCreateBy(SecurityUtils.getUsername());
        return toAjax(meetingRoomService.insertMeetingRoom(meetingRoom));
    }

    /**
     * 修改会议室
     */
    @PreAuthorize("@ss.hasPermi('meeting:room:edit')")
    @Log(title = "会议室管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody MeetingRoom meetingRoom)
    {
        meetingRoom.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(meetingRoomService.updateMeetingRoom(meetingRoom));
    }

    /**
     * 删除会议室
     */
    @PreAuthorize("@ss.hasPermi('meeting:room:remove')")
    @Log(title = "会议室管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roomIds}")
    public AjaxResult remove(@PathVariable Long[] roomIds)
    {
        meetingRoomService.deleteMeetingRoomByIds(roomIds);
        return success();
    }
}
