package com.ruoyi.system.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.MeetingBooking;

/**
 * 会议预约 数据层
 *
 * @author ruoyi
 */
public interface MeetingBookingMapper
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
     * 删除会议预约
     *
     * @param bookingId 预约ID
     * @return 结果
     */
    public int deleteMeetingBookingById(Long bookingId);

    /**
     * 批量删除会议预约
     *
     * @param bookingIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteMeetingBookingByIds(Long[] bookingIds);

    /**
     * 检查时间段冲突
     *
     * @param roomId 会议室ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeBookingId 排除的预约ID（编辑时排除自身）
     * @return 冲突数量
     */
    public int countConflict(@Param("roomId") Long roomId,
                             @Param("startTime") Date startTime,
                             @Param("endTime") Date endTime,
                             @Param("excludeBookingId") Long excludeBookingId);

    /**
     * 查询今日指定会议室的预约
     *
     * @param roomId 会议室ID
     * @return 今日预约列表
     */
    public List<MeetingBooking> selectTodayByRoom(@Param("roomId") Long roomId);
}
