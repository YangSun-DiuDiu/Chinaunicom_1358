package com.ruoyi.web.controller.meeting;

import java.util.Date;
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
    /** 即将开始的时间阈值（毫秒）：15分钟 */
    private static final long COMING_SOON_MS = 15 * 60 * 1000;

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
        result.put("currentStatus", calcCurrentStatus(todayList));
        result.put("currentBooking", getCurrentBooking(todayList));
        result.put("nextBooking", getNextBooking(todayList));
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
        result.put("currentStatus", calcCurrentStatus(todayList));
        result.put("nextBooking", getNextBooking(todayList));
        return success(result);
    }

    /**
     * 计算当前状态
     * <ul>
     *   <li>IN_USE：当前有预约正在进行中</li>
     *   <li>COMING_SOON：当前空闲，但下一预约在15分钟内开始</li>
     *   <li>FREE：当前空闲，且无即将开始的预约</li>
     * </ul>
     */
    private String calcCurrentStatus(List<MeetingBooking> todayList)
    {
        Date now = new Date();
        MeetingBooking current = getCurrentBooking(todayList);
        if (current != null)
        {
            return "IN_USE";
        }
        MeetingBooking next = getNextBooking(todayList);
        if (next != null && (next.getStartTime().getTime() - now.getTime()) <= COMING_SOON_MS)
        {
            return "COMING_SOON";
        }
        return "FREE";
    }

    /**
     * 获取当前正在进行的预约（startTime <= now < endTime）
     */
    private MeetingBooking getCurrentBooking(List<MeetingBooking> todayList)
    {
        Date now = new Date();
        for (MeetingBooking booking : todayList)
        {
            if ("APPROVED".equals(booking.getStatus())
                    && !booking.getStartTime().after(now)
                    && booking.getEndTime().after(now))
            {
                return booking;
            }
        }
        return null;
    }

    /**
     * 获取下一场即将开始的预约（startTime > now 且最近）
     */
    private MeetingBooking getNextBooking(List<MeetingBooking> todayList)
    {
        Date now = new Date();
        MeetingBooking next = null;
        for (MeetingBooking booking : todayList)
        {
            if (!"APPROVED".equals(booking.getStatus()))
            {
                continue;
            }
            if (booking.getStartTime().after(now))
            {
                if (next == null || booking.getStartTime().before(next.getStartTime()))
                {
                    next = booking;
                }
            }
        }
        return next;
    }
}
