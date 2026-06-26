package com.ruoyi.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.domain.MeetingBooking;
import com.ruoyi.system.mapper.MeetingBookingMapper;
import com.ruoyi.system.service.IMeetingBookingService;

/**
 * 会议预约 服务层实现
 *
 * @author ruoyi
 */
@Service
public class MeetingBookingServiceImpl implements IMeetingBookingService
{
    @Autowired
    private MeetingBookingMapper meetingBookingMapper;

    /**
     * 查询会议预约列表
     *
     * @param meetingBooking 会议预约信息
     * @return 会议预约集合
     */
    @Override
    public List<MeetingBooking> selectMeetingBookingList(MeetingBooking meetingBooking)
    {
        return meetingBookingMapper.selectMeetingBookingList(meetingBooking);
    }

    /**
     * 根据预约ID查询信息
     *
     * @param bookingId 预约ID
     * @return 会议预约信息
     */
    @Override
    public MeetingBooking selectMeetingBookingById(Long bookingId)
    {
        return meetingBookingMapper.selectMeetingBookingById(bookingId);
    }

    /**
     * 新增会议预约
     *
     * @param meetingBooking 会议预约信息
     * @return 结果
     */
    @Override
    public int insertMeetingBooking(MeetingBooking meetingBooking)
    {
        return meetingBookingMapper.insertMeetingBooking(meetingBooking);
    }

    /**
     * 修改会议预约
     *
     * @param meetingBooking 会议预约信息
     * @return 结果
     */
    @Override
    public int updateMeetingBooking(MeetingBooking meetingBooking)
    {
        return meetingBookingMapper.updateMeetingBooking(meetingBooking);
    }

    /**
     * 批量删除会议预约
     *
     * @param bookingIds 需要删除的预约ID
     */
    @Override
    public void deleteMeetingBookingByIds(Long[] bookingIds)
    {
        for (Long bookingId : bookingIds)
        {
            meetingBookingMapper.deleteMeetingBookingById(bookingId);
        }
    }

    /**
     * 判断时间段是否有冲突
     *
     * @param roomId 会议室ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeBookingId 排除的预约ID
     * @return true 有冲突，false 无冲突
     */
    @Override
    public boolean hasConflict(Long roomId, Date startTime, Date endTime, Long excludeBookingId)
    {
        int count = meetingBookingMapper.countConflict(roomId, startTime, endTime, excludeBookingId);
        return count > 0;
    }

    /**
     * 查询今日指定会议室的预约
     *
     * @param roomId 会议室ID
     * @return 今日预约列表
     */
    @Override
    public List<MeetingBooking> selectTodayByRoom(Long roomId)
    {
        return meetingBookingMapper.selectTodayByRoom(roomId);
    }

    /**
     * 查询指定会议室指定日期的已占用时间段
     *
     * @param roomId 会议室ID
     * @param date 日期 (yyyy-MM-dd)
     * @return 预约列表
     */
    @Override
    public List<MeetingBooking> selectSlots(Long roomId, String date)
    {
        return meetingBookingMapper.selectSlots(roomId, date);
    }
}
