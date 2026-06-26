package com.ruoyi.system.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.system.domain.MeetingBooking;

/**
 * 会议预约 服务层
 *
 * @author ruoyi
 */
public interface IMeetingBookingService
{
    /**
     * 查询会议预约列表
     *
     * @param meetingBooking 会议预约信息
     * @return 会议预约集合
     */
    public List<MeetingBooking> selectMeetingBookingList(MeetingBooking meetingBooking);

    /**
     * 根据预约ID查询信息
     *
     * @param bookingId 预约ID
     * @return 会议预约信息
     */
    public MeetingBooking selectMeetingBookingById(Long bookingId);

    /**
     * 新增会议预约
     *
     * @param meetingBooking 会议预约信息
     * @return 结果
     */
    public int insertMeetingBooking(MeetingBooking meetingBooking);

    /**
     * 修改会议预约
     *
     * @param meetingBooking 会议预约信息
     * @return 结果
     */
    public int updateMeetingBooking(MeetingBooking meetingBooking);

    /**
     * 批量删除会议预约
     *
     * @param bookingIds 需要删除的预约ID
     */
    public void deleteMeetingBookingByIds(Long[] bookingIds);

    /**
     * 判断时间段是否有冲突
     *
     * @param roomId 会议室ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeBookingId 排除的预约ID
     * @return true 有冲突，false 无冲突
     */
    public boolean hasConflict(Long roomId, Date startTime, Date endTime, Long excludeBookingId);

    /**
     * 查询今日指定会议室的预约
     *
     * @param roomId 会议室ID
     * @return 今日预约列表
     */
    public List<MeetingBooking> selectTodayByRoom(Long roomId);
}
