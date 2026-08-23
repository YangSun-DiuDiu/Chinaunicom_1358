package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.MeetingBooking;
import com.ruoyi.system.domain.MeetingRoom;
import com.ruoyi.system.mapper.MeetingRoomMapper;
import com.ruoyi.system.service.IMeetingRoomService;

/**
 * 会议室 服务层实现
 *
 * @author ruoyi
 */
@Service
public class MeetingRoomServiceImpl implements IMeetingRoomService
{
    @Autowired
    private MeetingRoomMapper meetingRoomMapper;

    /**
     * 查询会议室列表
     *
     * @param meetingRoom 会议室信息
     * @return 会议室集合
     */
    @Override
    public List<MeetingRoom> selectMeetingRoomList(MeetingRoom meetingRoom)
    {
        return meetingRoomMapper.selectMeetingRoomList(meetingRoom);
    }

    /**
     * 根据会议室ID查询信息
     *
     * @param roomId 会议室ID
     * @return 会议室信息
     */
    @Override
    public MeetingRoom selectMeetingRoomById(Long roomId)
    {
        return meetingRoomMapper.selectMeetingRoomById(roomId);
    }

    /**
     * 新增会议室
     *
     * @param meetingRoom 会议室信息
     * @return 结果
     */
    @Override
    public int insertMeetingRoom(MeetingRoom meetingRoom)
    {
        return meetingRoomMapper.insertMeetingRoom(meetingRoom);
    }

    /**
     * 修改会议室
     *
     * @param meetingRoom 会议室信息
     * @return 结果
     */
    @Override
    public int updateMeetingRoom(MeetingRoom meetingRoom)
    {
        return meetingRoomMapper.updateMeetingRoom(meetingRoom);
    }

    /**
     * 批量删除会议室
     *
     * @param roomIds 需要删除的会议室ID
     */
    @Override
    public void deleteMeetingRoomByIds(Long[] roomIds)
    {
        for (Long roomId : roomIds)
        {
            meetingRoomMapper.deleteMeetingRoomById(roomId);
        }
    }

    /** 即将开始的时间阈值（毫秒）：15分钟 */
    private static final long COMING_SOON_MS = 15 * 60 * 1000;

    /**
     * 计算会议室当前使用状态
     * <ul>
     *   <li>IN_USE：当前有 APPROVED 预约正在进行中（startTime <= now < endTime）</li>
     *   <li>COMING_SOON：当前空闲，但下一预约在 15 分钟内开始</li>
     *   <li>FREE：当前空闲，且无即将开始的预约</li>
     * </ul>
     */
    @Override
    public String calcCurrentStatus(List<MeetingBooking> todayList)
    {
        if (getCurrentBooking(todayList) != null)
        {
            return "IN_USE";
        }
        MeetingBooking next = getNextBooking(todayList);
        if (next != null && (next.getStartTime().getTime() - new Date().getTime()) <= COMING_SOON_MS)
        {
            return "COMING_SOON";
        }
        return "FREE";
    }

    /**
     * 获取当前正在进行的预约（APPROVED 且 startTime <= now < endTime）
     */
    @Override
    public MeetingBooking getCurrentBooking(List<MeetingBooking> todayList)
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
     * 获取下一场即将开始的预约（APPROVED 且 startTime > now 中最近的一场）
     */
    @Override
    public MeetingBooking getNextBooking(List<MeetingBooking> todayList)
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
