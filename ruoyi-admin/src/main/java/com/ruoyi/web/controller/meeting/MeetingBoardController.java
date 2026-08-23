package com.ruoyi.web.controller.meeting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.MeetingBooking;
import com.ruoyi.system.domain.MeetingRoom;
import com.ruoyi.system.service.IMeetingBookingService;
import com.ruoyi.system.service.IMeetingRoomService;

/**
 * 会议室看板（平板端）公共接口 - 无需登录
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/meeting/board")
public class MeetingBoardController extends BaseController
{
    @Autowired
    private IMeetingRoomService meetingRoomService;

    @Autowired
    private IMeetingBookingService meetingBookingService;

    /**
     * 会议室看板完整信息
     *
     * @param roomId 会议室ID
     * @return 会议室信息 + 今日预约 + 当前状态 + 当前预约 + 下一预约
     */
    @GetMapping("/{roomId}")
    public AjaxResult board(@PathVariable Long roomId)
    {
        MeetingRoom room = meetingRoomService.selectMeetingRoomById(roomId);
        if (room == null)
        {
            return error("会议室不存在");
        }
        List<MeetingBooking> todayList = meetingBookingService.selectTodayByRoom(roomId);

        Map<String, Object> result = new HashMap<>();
        result.put("room", room);
        result.put("todayBookings", todayList);
        result.put("currentStatus", meetingRoomService.calcCurrentStatus(todayList));
        result.put("currentBooking", meetingRoomService.getCurrentBooking(todayList));
        result.put("nextBooking", meetingRoomService.getNextBooking(todayList));
        return success(result);
    }

    /**
     * 会议室看板轻量刷新 - 仅返回当前状态和下一预约
     *
     * @param roomId 会议室ID
     * @return currentStatus + nextBooking
     */
    @GetMapping("/{roomId}/refresh")
    public AjaxResult refresh(@PathVariable Long roomId)
    {
        MeetingRoom room = meetingRoomService.selectMeetingRoomById(roomId);
        if (room == null)
        {
            return error("会议室不存在");
        }
        List<MeetingBooking> todayList = meetingBookingService.selectTodayByRoom(roomId);

        Map<String, Object> result = new HashMap<>();
        result.put("roomId", room.getRoomId());
        result.put("roomName", room.getRoomName());
        result.put("currentStatus", meetingRoomService.calcCurrentStatus(todayList));
        result.put("nextBooking", meetingRoomService.getNextBooking(todayList));
        return success(result);
    }
}
