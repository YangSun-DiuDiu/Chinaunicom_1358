package com.ruoyi.web.controller.h5;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.DeviceStatusLog;
import com.ruoyi.system.domain.MeetingBooking;
import com.ruoyi.system.domain.MeetingRoom;
import com.ruoyi.system.domain.VisitorAppointment;
import com.ruoyi.system.domain.VisitorLog;
import com.ruoyi.system.mapper.DeviceStatusLogMapper;
import com.ruoyi.system.mapper.MeetingBookingMapper;
import com.ruoyi.system.mapper.MeetingRoomMapper;
import com.ruoyi.system.mapper.VisitorAppointmentMapper;
import com.ruoyi.system.mapper.VisitorLogMapper;

/**
 * H5轻应用专用Controller
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/h5")
public class H5Controller extends BaseController
{
    @Autowired
    private DeviceStatusLogMapper deviceStatusLogMapper;

    @Autowired
    private MeetingRoomMapper meetingRoomMapper;

    @Autowired
    private MeetingBookingMapper meetingBookingMapper;

    @Autowired
    private VisitorLogMapper visitorLogMapper;

    @Autowired
    private VisitorAppointmentMapper visitorAppointmentMapper;

    /**
     * 设备列表（含最新心跳状态）
     */
    @GetMapping("/device/list")
    public AjaxResult deviceList()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        List<DeviceStatusLog> logs = deviceStatusLogMapper.selectLogList(new DeviceStatusLog());
        if (logs != null) {
            for (DeviceStatusLog log : logs) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("deviceId", log.getDeviceId());
                item.put("deviceName", log.getDeviceName());
                item.put("status", log.getNewStatus());
                item.put("lastCheckTime", log.getChangeTime());
                list.add(item);
            }
        }
        return success(list);
    }

    /**
     * 会议室列表 + 当日安排
     */
    @GetMapping("/meeting/rooms")
    public AjaxResult meetingRooms()
    {
        List<Map<String, Object>> list = new ArrayList<>();
        List<MeetingRoom> rooms = meetingRoomMapper.selectMeetingRoomList(new MeetingRoom());
        if (rooms != null) {
            for (MeetingRoom room : rooms) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("roomId", room.getRoomId());
                item.put("roomName", room.getRoomName());
                item.put("capacity", room.getCapacity());
                item.put("location", room.getLocation());
                item.put("status", room.getStatus());
                List<MeetingBooking> todayList = meetingBookingMapper.selectTodayByRoom(room.getRoomId());
                item.put("todayBookings", todayList != null ? todayList : Collections.emptyList());
                list.add(item);
            }
        }
        return success(list);
    }

    /**
     * 首页动态聚合
     */
    @GetMapping("/news")
    public AjaxResult news()
    {
        Map<String, Object> result = new LinkedHashMap<>();

        List<VisitorLog> logs = visitorLogMapper.selectLogList(new VisitorLog());
        List<Map<String, Object>> visitors = new ArrayList<>();
        if (logs != null) {
            for (int i = 0; i < Math.min(logs.size(), 5); i++) {
                VisitorLog log = logs.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("visitorName", log.getVisitorName());
                item.put("hostName", log.getHostName());
                item.put("entryTime", log.getEntryTime());
                visitors.add(item);
            }
        }
        result.put("recentVisitors", visitors);

        List<VisitorAppointment> pendingList = visitorAppointmentMapper.selectPendingList(
                SecurityUtils.isAdmin() ? null : SecurityUtils.getUserId());
        result.put("pendingApprovals", pendingList != null ? pendingList.size() : 0);

        List<MeetingBooking> meetings = meetingBookingMapper.selectMeetingBookingList(new MeetingBooking());
        List<Map<String, Object>> meetingList = new ArrayList<>();
        if (meetings != null) {
            for (int i = 0; i < Math.min(meetings.size(), 5); i++) {
                MeetingBooking m = meetings.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("title", m.getTitle());
                item.put("roomName", m.getRoomName());
                item.put("startTime", m.getStartTime());
                item.put("status", m.getStatus());
                meetingList.add(item);
            }
        }
        result.put("recentMeetings", meetingList);

        return success(result);
    }
}
